// Copyright (C) 2015 Advanced Micro Devices, Inc.  All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.amd.gerrit.plugins.manifestsubscription;

import com.amd.gerrit.plugins.manifestsubscription.manifest.Manifest;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.VersionedMetaData;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VersionedManifests extends VersionedMetaData implements ManifestProvider {
  private String refName;
  private Unmarshaller manifestUnmarshaller;
  private Marshaller manifestMarshaller;
  private Map<String, Manifest> manifests;
  private String srcManifestRepo = "";
  private String extraCommitMsg = "";

  public String getExtraCommitMsg() {
    return extraCommitMsg;
  }

  public void setExtraCommitMsg(String extraCommitMsg) {
    this.extraCommitMsg = extraCommitMsg;
  }

  public String getSrcManifestRepo() {
    return srcManifestRepo;
  }

  public void setSrcManifestRepo(String srcManifestRepo) {
    this.srcManifestRepo = srcManifestRepo;
  }

  public Map<String, Manifest> getManifests() {
    return Collections.unmodifiableMap(manifests);
  }

  public void setManifests(Map<String, Manifest> manifests) {
    this.manifests = manifests;
  }

  public Set<String> getManifestPaths() {
    return manifests.keySet();
  }

  private VersionedManifests() throws JAXBException {
    JAXBContext jaxbctx = JAXBContext.newInstance(Manifest.class);
    this.manifestUnmarshaller = jaxbctx.createUnmarshaller();
    this.manifestMarshaller = jaxbctx.createMarshaller();
    this.manifestMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
  }

  public VersionedManifests(String refName)
      throws JAXBException {
    this();
    this.refName = refName;

  }

  public VersionedManifests(String refName,
                            Map<String, Manifest> manifests) throws JAXBException {
    this(refName);
    this.manifests = manifests;

  }

  @Override
  protected String getRefName() {
    return refName;
  }

  @Override
  protected void onLoad() throws IOException, ConfigInvalidException {
    manifests = Maps.newHashMap();

    String path;
    Manifest manifest;

    RevWalk rw = new RevWalk(reader);

    // This happens when someone configured a invalid branch name
    if (getRevision() == null) {
      throw new ConfigInvalidException(refName);
    }
    RevCommit r = rw.parseCommit(getRevision());
    TreeWalk treewalk = new TreeWalk(reader);
    treewalk.addTree(r.getTree());
    treewalk.setRecursive(false);
    treewalk.setFilter(PathSuffixFilter.create(".xml"));
    while (treewalk.next()) {
      if (treewalk.isSubtree()) {
        treewalk.enterSubtree();
      } else {
        path = treewalk.getPathString();
        try {
          //TODO: Should this be done more lazily?
          //TODO: difficult to do when reader is not available outside of onLoad?
          ByteArrayInputStream input = new ByteArrayInputStream(readFile(path));
          manifest = (Manifest) manifestUnmarshaller.unmarshal(input);
          manifests.put(path, manifest);
        } catch (JAXBException e) {
          e.printStackTrace();
        }
      }
    }

    treewalk.release();

    //TODO load changed manifest
//    DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
  }

  @Override
  protected boolean onSave(CommitBuilder commit) throws IOException {
    StringBuilder commitMsg = new StringBuilder();
    commitMsg.append("Snapshot manifest from " +
        srcManifestRepo + " updated\n\n");

    if (extraCommitMsg != null && extraCommitMsg.length() > 0) {
      commitMsg.append(extraCommitMsg);
    }

    String path;
    Manifest manifest;
    for (Map.Entry<String, Manifest> entry : manifests.entrySet()) {
      path = entry.getKey();
      manifest = entry.getValue();

      try {
        saveManifest(path, manifest);
      } catch (JAXBException e) {
        throw new IOException(e);
      }
    }

    if (commit.getMessage() == null || "".equals(commit.getMessage())) {
      commit.setMessage(commitMsg.toString());
    }

    return true;
  }

  @Override
  public Manifest readManifest(String path) throws ManifestReadException {
    if (manifests.containsKey(path)) {
      return manifests.get(path);
    }

    throw new ManifestReadException(path);
  }

  /**
   * Must be called inside onSave
   *
   * @param path
   * @param manifest
   * @throws JAXBException
   * @throws IOException
   */
  private void saveManifest(String path, Manifest manifest)
      throws JAXBException, IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    manifestMarshaller.marshal(manifest, output);
    saveFile(path, output.toByteArray());
  }

  /**
   * Pass in a {@link com.google.common.collect.Table} if you want to reuse
   * the lookup cache
   *
   * @param gitRepoManager
   * @param manifest
   * @param lookup
   */
  static void affixManifest(GitRepositoryManager gitRepoManager,
                            Manifest manifest, Table<String, String, String> lookup) {
    if (lookup == null) {
      // project, branch, hash
      lookup = HashBasedTable.create();
    }

    String defaultRef = null;

    if (manifest.getDefault() != null) {
      defaultRef = manifest.getDefault().getRevision();
    }

    affixManifest(gitRepoManager, manifest.getProject(), defaultRef, lookup);
  }

  private static void affixManifest(GitRepositoryManager gitRepoManager,
                                    List<com.amd.gerrit.plugins.manifestsubscription.manifest.Project> projects,
                                    String defaultRef,
                                    Table<String, String, String> lookup) {

    String ref;
    String hash;
    String projectName;
    Project.NameKey p;
    for (com.amd.gerrit.plugins.manifestsubscription.manifest.Project project : projects) {
      projectName = project.getName();
      ref = project.getRevision();

      ref = (ref == null) ? defaultRef : ref;

      if (ref != null) {
        hash = lookup.get(projectName, ref);

        if (hash == null) {
          p = new Project.NameKey(projectName);
          try {
            Repository db = gitRepoManager.openRepository(p);

            hash = db.resolve(ref).getName();
            db.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }

        if (hash != null) {
          lookup.put(projectName, ref, hash);
          project.setRevision(hash);
          project.setUpstream(ref);
        }
      }

      if (project.getProject().size() > 0) {
        affixManifest(gitRepoManager, project.getProject(), defaultRef, lookup);
      }
    }
  }
}
