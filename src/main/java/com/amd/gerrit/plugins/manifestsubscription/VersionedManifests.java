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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.gerrit.common.ChangeHooks;
import com.google.gerrit.reviewdb.client.Branch;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.VersionedMetaData;

import com.amd.gerrit.plugins.manifestsubscription.manifest.Manifest;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

public class VersionedManifests extends VersionedMetaData implements ManifestProvider {
  private static final Logger log =
      LoggerFactory.getLogger(VersionedManifests.class);

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

    try (RevWalk rw = new RevWalk(reader);
         TreeWalk treewalk = new TreeWalk(reader)) {
      // This happens when someone configured a invalid branch name
      if (getRevision() == null) {
        throw new ConfigInvalidException(refName);
      }
      RevCommit r = rw.parseCommit(getRevision());
      treewalk.addTree(r.getTree());
      treewalk.setRecursive(false);
      treewalk.setFilter(PathSuffixFilter.create(".xml"));
      while (treewalk.next()) {
        if (treewalk.isSubtree()) {
          treewalk.enterSubtree();
        } else {
          path = treewalk.getPathString();
          //TODO: Should this be done more lazily?
          //TODO: difficult to do when reader is not available outside of onLoad?
          try (ByteArrayInputStream input
                   = new ByteArrayInputStream(readFile(path))) {
            manifest = (Manifest) manifestUnmarshaller.unmarshal(input);
            manifests.put(path, manifest);
          } catch (JAXBException e) {
            e.printStackTrace();
          }
        }
      }
    }

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

    // For some reason the default author and committer date is
    // invalid (always the same date and time)
    Date date = new Date();
    commit.setAuthor(new PersonIdent(commit.getAuthor(), date));
    commit.setCommitter(new PersonIdent(commit.getCommitter(), date));

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

  static void tagManifest(GitRepositoryManager gitRepoManager,
                             Manifest manifest,
                             final String tag)
      throws GitAPIException, IOException {
    Table<String, String, String> lookup = HashBasedTable.create();
    String defaultRef = null;

    if (manifest.getDefault() != null) {
      defaultRef = manifest.getDefault().getRevision();
    }

    ManifestOp op = new ManifestOp() {
      @Override
      public boolean apply(com.amd.gerrit.plugins.manifestsubscription.manifest.Project project,
                           String hash, String name,
                           GitRepositoryManager gitRepoManager) throws
          GitAPIException, IOException {
        Project.NameKey p = new Project.NameKey(project.getName());
        try (Repository db = gitRepoManager.openRepository(p);
             Git git = new Git(db);
             RevWalk walk = new RevWalk(db)) {
          RevCommit commit = walk.parseCommit(db.resolve(hash));
          git.tag().setName(tag).setObjectId(commit).setAnnotated(true).call();
        }
        return true;
      }
    };

    traverseManifestAndApplyOp(gitRepoManager, manifest.getProject(), defaultRef, op, lookup);
  }

  static void branchManifest(GitRepositoryManager gitRepoManager,
                             Manifest manifest,
                             final String branch,
                             final ChangeHooks changeHooks)
                                          throws GitAPIException, IOException {
    Table<String, String, String> lookup = HashBasedTable.create();
    String defaultRef = null;

    if (manifest.getDefault() != null) {
      defaultRef = manifest.getDefault().getRevision();
    }

    ManifestOp op = new ManifestOp() {
      @Override
      public boolean apply(com.amd.gerrit.plugins.manifestsubscription.manifest.Project project,
                           String hash, String name,
                           GitRepositoryManager gitRepoManager) throws
          GitAPIException, IOException {
        Project.NameKey p = new Project.NameKey(project.getName());
        try (Repository db = gitRepoManager.openRepository(p);
             Git git = new Git(db)) {
          try {
            Ref r = git.branchCreate().setName(branch).setStartPoint(hash).call();
            changeHooks.doRefUpdatedHook(new Branch.NameKey(p, branch),
                ObjectId.zeroId(),
                r.getObjectId(), null);
            log.info(r.toString());
            log.info(r.getObjectId().toString());
          } catch (Exception e) {

          }
        }
        return true;
      }
    };

    traverseManifestAndApplyOp(gitRepoManager, manifest.getProject(), defaultRef, op, lookup);
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
                            Manifest manifest,
                            Table<String, String, String> lookup)
                                          throws GitAPIException, IOException {
    if (lookup == null) {
      // project, branch, hash
      lookup = HashBasedTable.create();
    }

    String defaultRef = null;

    if (manifest.getDefault() != null) {
      defaultRef = manifest.getDefault().getRevision();
    }

    ManifestOp op = new ManifestOp() {
      @Override
      public boolean apply(com.amd.gerrit.plugins.manifestsubscription.manifest.Project project,
                           String hash, String name,
                           GitRepositoryManager gitRepoManager) {
        project.setRevision(hash);
        if (!ObjectId.isId(name)) {
          project.setUpstream(name);
        }
        return true;
      }
    };

    traverseManifestAndApplyOp(gitRepoManager, manifest.getProject(), defaultRef, op, lookup);
  }

  static void traverseManifestAndApplyOp(
      GitRepositoryManager gitRepoManager,
      List<com.amd.gerrit.plugins.manifestsubscription.manifest.Project> projects,
      String defaultRef,
      ManifestOp op,
      Table<String, String, String> lookup) throws GitAPIException, IOException {

    String ref;
    String hash;
    String projectName;
    Project.NameKey p;
    for (com.amd.gerrit.plugins.manifestsubscription.manifest.Project project : projects) {
      projectName = project.getName();
      ref = project.getRevision();

      ref = (ref == null) ? defaultRef : ref;

      if (ref != null) {
        if (lookup != null) {
          hash = lookup.get(projectName, ref);
        } else {
          hash = null;
        }

        if (hash == null) {
          p = new Project.NameKey(projectName);
          try (Repository db = gitRepoManager.openRepository(p)) {
            hash = db.resolve(ref).getName();
          } catch (IOException | NullPointerException e) {
            log.warn(ref);
            log.warn(projectName);
            log.warn(defaultRef);
            e.printStackTrace();
          }
        }

        if (hash != null) {
          if (lookup != null) lookup.put(projectName, ref, hash);
          op.apply(project, hash, ref, gitRepoManager);
        }
      }

      if (project.getProject().size() > 0) {
        traverseManifestAndApplyOp(gitRepoManager, project.getProject(), defaultRef, op, lookup);
      }
    }
  }
}
