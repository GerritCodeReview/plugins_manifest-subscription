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
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.MetaDataUpdate;
import com.google.gerrit.server.git.ProjectConfig;
import com.google.gerrit.server.project.ProjectCache;
import com.google.inject.Inject;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.*;

public class ManifestSubscription implements
    GitReferenceUpdatedListener, LifecycleListener {
  private static final Logger log =
      LoggerFactory.getLogger(ManifestSubscription.class);

  private static final String KEY_BRANCH = "branch";
  private static final String KEY_STORE = "store";

  private static final String STORE_BRANCH_PREFIX = "refs/heads/m/";

  private final String pluginName;

  private final MetaDataUpdate.Server metaDataUpdateFactory;
  private final GitRepositoryManager gitRepoManager;
  private final ProjectCache projectCache;

  /**
   * source project
   **/
  private Map<String, PluginProjectConfig> enabledManifestRepos;
  private Set<String> stores;

  /**
   * subscribed project name and branch, manifest dest store, manifest dest branch
   **/
  private Table<ProjectBranchKey, String, Set<String>> subscribedRepos;

  @Override
  public void start() {
    ProjectConfig config;
    for (Project.NameKey p : projectCache.all()) {
      try {
        config = ProjectConfig.read(metaDataUpdateFactory.create(p));
        loadStoreFromProjectConfig(p.toString(),config);

        PluginProjectConfig ppc = enabledManifestRepos.get(p.toString());

        if (ppc != null) {
          Project.NameKey store = new Project.NameKey(ppc.getStore());
          Map<String, Ref> branches = gitRepoManager.openRepository(store)
              .getRefDatabase().getRefs(STORE_BRANCH_PREFIX);

          //list all branches in the store
          for (String branchPath : branches.keySet()) {
            try {
              VersionedManifests manifests = parseManifests(store,
                  STORE_BRANCH_PREFIX+branchPath);
              watchCanonicalManifest(manifests.getManifests().get("default.xml")
                  , store.toString(), branchPath);
            } catch (Exception e) {
              log.error(e.toString());
              e.printStackTrace();
            }

          }
        }

      } catch (IOException | ConfigInvalidException e) {
        log.error(e.toString());
        e.printStackTrace();
      }
    }
  }

  @Override
  public void stop() {

  }

  @Inject
  ManifestSubscription(MetaDataUpdate.Server metaDataUpdateFactory,
                       GitRepositoryManager gitRepoManager,
                       @PluginName String pluginName,
                       ProjectCache projectCache) {
    this.gitRepoManager = gitRepoManager;
    this.projectCache = projectCache;
    this.enabledManifestRepos = Maps.newHashMap();
    this.stores = Sets.newHashSet();

    this.subscribedRepos = HashBasedTable.create();

    this.metaDataUpdateFactory = metaDataUpdateFactory;
    this.pluginName = pluginName;
  }

  @Override
  public void onGitReferenceUpdated(Event event) {
    String projectName = event.getProjectName();
    String refName = event.getRefName();
    String branchName = refName.startsWith("refs/heads/") ?
        refName.substring(11) : "";
    ProjectBranchKey pbKey = new ProjectBranchKey(projectName, branchName);

//    log.warn(event.getNewObjectId());
//    log.warn(projectName);
//    log.warn(refName);
//    log.warn(pluginName);

    if ("refs/meta/config".equals(refName)) {
      // possible change in enabled repos
      processProjectConfigChange(event);
    } else if (enabledManifestRepos.containsKey(projectName) &&
        enabledManifestRepos.get(projectName)
            .getBranches().contains(branchName)) {
      processManifestChange(event, projectName, branchName);

    } else if (subscribedRepos.containsRow(pbKey)) {
      // Manifest store and branch
      Map<String, Set<String>> destinations = subscribedRepos.row(pbKey);

      VersionedManifests versionedManifests;
      MetaDataUpdate update;
      for (String store : destinations.keySet()) {
        for (String storeBranch : destinations.get(store)) {
          Project.NameKey p = new Project.NameKey(store);
          try {
            update = metaDataUpdateFactory.create(p);
            versionedManifests =
                new VersionedManifests(STORE_BRANCH_PREFIX+storeBranch);
            versionedManifests.load(update);

            Manifest manifest =
                versionedManifests.getManifests().get("default.xml");
            updateProjectRev(projectName, branchName,
                event.getNewObjectId(), manifest.getProject());
            versionedManifests.commit(update);
          } catch (IOException | ConfigInvalidException | JAXBException e) {
            e.printStackTrace();
          }
        }
      }

      //updates in subscribed repos
    }


  }

  private void updateProjectRev(String projectName, String branch, String rev,
                                List<com.amd.gerrit.plugins.
                                    manifestsubscription.manifest.Project> projects) {
    //TODO optimize to not have to iterate through manifest?
    for (com.amd.gerrit.plugins.manifestsubscription.manifest.Project project : projects) {
      if (Objects.equals(projectName, project.getName()) &&
          Objects.equals(branch, project.getUpstream())) {
        project.setRevision(rev);
      }

      if (project.getProject().size() > 0) {
        updateProjectRev(projectName, branch, rev, project.getProject());
      }
    }

  }

  private void processManifestChange(Event event,
                                     String projectName, String branchName) {
    VersionedManifests versionedManifests;
    //possible manifest update in subscribing repos
    //TODO Fix, right now update all manifest every time
    //TODO even when only one of the manifest has changed
    //TODO ** remove old manifest

    try {
      versionedManifests = parseManifests(event);
      if (versionedManifests != null) {
        CanonicalManifest cManifest = new CanonicalManifest(versionedManifests);
        Map<String, Manifest> manifests = versionedManifests.getManifests();
        Manifest manifest;
        String store = enabledManifestRepos.get(projectName).getStore();
        Table<String, String, String> lookup = HashBasedTable.create();

        //TODO need to make sure remote is pointing to this server?
        //TODO this may be impossible
        //TODO only monitor projects without 'remote' attribute / only using default?

        for (String path : manifests.keySet()) {
          String bp = branchName + "/" + path;
          try {
            manifest = cManifest.getCanonicalManifest(path);

            watchCanonicalManifest(manifest, store, bp);

            VersionedManifests.affixManifest(gitRepoManager, manifest, lookup);
            //save manifest
            //TODO added the m/ to the ref to to work around LOCK_FAILURE error of creating master/bla/bla
            //TODO ref when master ref already exists... better solution?
            updateManifest(store, STORE_BRANCH_PREFIX + bp, manifest);

          } catch (ManifestReadException e) {
            e.printStackTrace();
          }

        }

      }
    } catch (JAXBException | IOException | ConfigInvalidException e) {
      e.printStackTrace();
    }

  }

  private void watchCanonicalManifest(Manifest manifest, String store,
                                      String branchPath) {
    String defaultBranch;
    ProjectBranchKey pbKey;
    if (manifest.getDefault() != null &&
        manifest.getDefault().getRevision() != null) {
      defaultBranch = manifest.getDefault().getRevision();
    } else {
      defaultBranch = "";
    }

    for (com.amd.gerrit.plugins.manifestsubscription.manifest.Project project : manifest.getProject()) {
      if (stores.contains(project.getName())) {
        // Skip if it's one of the repo for storing
        // manifest to avoid infinite loop
        // This is a bit too general, but it's done to avoid the complexity
        // of actually tracing out the loop
        // i.e. manifest1->store2 --> manifest2->store1
        continue;
      }

      // Make sure revision is branch ref w/o refs/heads
      String branch = project.getRevision() == null ?
          defaultBranch : (project.getUpstream() != null ?
                           project.getUpstream() : project.getRevision());
      pbKey = new ProjectBranchKey(project.getName(),
                    Repository.shortenRefName(branch));


      //TODO only update manifests that changed
      if (!subscribedRepos.contains(pbKey, store)) {
        subscribedRepos.put(pbKey, store, Sets.<String>newHashSet());
      }
      subscribedRepos.get(pbKey,store).add(branchPath);

    }
  }

  private void processProjectConfigChange(Event event) {
    Project.NameKey p = new Project.NameKey(event.getProjectName());

    //TODO test two separate project configured to the same store
    try {
      ProjectConfig oldCfg = parseConfig(p, event.getOldObjectId());
      ProjectConfig newCfg = parseConfig(p, event.getNewObjectId());

      //TODO selectively update changes instead of complete reset
      if (oldCfg != null) {
        String oldStore =
            oldCfg.getPluginConfig(pluginName).getString(KEY_STORE);

        if (oldStore != null && !oldStore.isEmpty()) {
          //TODO FIX assume unique store for each source project
          stores.remove(oldStore);
          enabledManifestRepos.remove(event.getProjectName());
          Iterator<Table.Cell<ProjectBranchKey, String, Set<String>>> iter =
              subscribedRepos.cellSet().iterator();
          while (iter.hasNext()) {
            if (oldStore.equals(iter.next().getColumnKey())) {
              iter.remove();
            }
          }
        }
      }

      if (newCfg != null) {
        loadStoreFromProjectConfig(event.getProjectName(), newCfg);
      }
    } catch (IOException | ConfigInvalidException e) {
      e.printStackTrace();
    }
  }

  private void loadStoreFromProjectConfig(String projectName,
                                          ProjectConfig config) {
    String newStore =
        config.getPluginConfig(pluginName).getString(KEY_STORE);

    if (newStore != null) {
      newStore = newStore.trim();
      if (!newStore.isEmpty()) {
        Set<String> branches = Sets.newHashSet(
            config.getPluginConfig(pluginName)
                .getStringList(KEY_BRANCH));

        if (branches.size() > 0) {
          enabledManifestRepos.put(projectName,
              new PluginProjectConfig(newStore, branches));
          stores.add(newStore);
          //TODO trigger reparse manifest into memory
        }
      }
    }
  }

  private ProjectConfig parseConfig(Project.NameKey p, String idStr)
      throws IOException, ConfigInvalidException {
    ObjectId id = ObjectId.fromString(idStr);
    if (ObjectId.zeroId().equals(id)) {
      return null;
    }
    return ProjectConfig.read(metaDataUpdateFactory.create(p), id);
  }

  private VersionedManifests parseManifests(Event event)
      throws JAXBException, IOException, ConfigInvalidException {
    Project.NameKey p = new Project.NameKey(event.getProjectName());
    return parseManifests(p, event.getRefName());
  }

  private VersionedManifests parseManifests(Project.NameKey p, String refName)
      throws IOException, JAXBException, ConfigInvalidException {

    MetaDataUpdate update = metaDataUpdateFactory.create(p);
    VersionedManifests vManifests = new VersionedManifests(refName);
    vManifests.load(update);

    return vManifests;
  }

  private void updateManifest(String projectName, String refName,
                              Manifest manifest)
      throws JAXBException, IOException {
    Project.NameKey p = new Project.NameKey(projectName);
    MetaDataUpdate update = metaDataUpdateFactory.create(p);
    VersionedManifests vManifests = new VersionedManifests(refName);

    //TODO find a better way to detect no branch
    boolean refExists = true;
    try {
      vManifests.load(update);
    } catch (Exception e) {
      refExists = false;
    }

    if (refExists) {
      Map<String, Manifest> entry = Maps.newHashMapWithExpectedSize(1);
      entry.put("default.xml", manifest);
      vManifests.setManifests(entry);
      vManifests.commit(update);
    } else {
      vManifests = new VersionedManifests("master");
      try {
        vManifests.load(update);
      } catch (ConfigInvalidException e) {
        e.printStackTrace();
      }
      Map<String, Manifest> entry = Maps.newHashMapWithExpectedSize(1);
      entry.put("default.xml", manifest);
      vManifests.setManifests(entry);
      vManifests.commitToNewRef(update, refName);
    }
  }

}
