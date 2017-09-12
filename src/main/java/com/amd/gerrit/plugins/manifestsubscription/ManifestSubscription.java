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
import com.google.common.collect.*;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.extensions.events.GitReferenceUpdated;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.MetaDataUpdate;
import com.google.gerrit.server.git.ProjectConfig;
import com.google.gerrit.server.project.ProjectCache;
import com.google.inject.Inject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.*;

import static com.google.gerrit.reviewdb.client.RefNames.REFS_CONFIG;

public class ManifestSubscription implements
    GitReferenceUpdatedListener, LifecycleListener {
  private static final Logger log =
      LoggerFactory.getLogger(ManifestSubscription.class);

  private static final String KEY_BRANCH = "branch";
  private static final String KEY_STORE = "store";

  static final String STORE_BRANCH_PREFIX = "refs/heads/m/";

  private final String pluginName;

  private final MetaDataUpdate.Server metaDataUpdateFactory;
  private final GitRepositoryManager gitRepoManager;
  private final ProjectCache projectCache;
  private final GitReferenceUpdated gitRefUpdated;



  /**
   * source project lookup
   * manifest source project name, plugin config
   **/
  private Map<String, PluginProjectConfig> enabledManifestSource = Maps.newHashMap();

  /**
   * manifest store lookup
   * repo name, branch (branchPath), Manifest
   */
  private Table<String, String, Manifest> manifestStores = HashBasedTable.create();

  /**
   * manifest source lookup
   * repo name, branch (branchPath), manifest source repo
   */
  private Table<String, String, String> manifestSource = HashBasedTable.create();

  /**
   * lookup
   * subscribed project name and branch, manifest dest store, <manifest dest branch Project>
   **/
  private Table<ProjectBranchKey, String, Map<String, Set<
      com.amd.gerrit.plugins.manifestsubscription.manifest.Project>>> subscribedRepos = HashBasedTable.create();

  public Set<String> getEnabledManifestSource() {
    return ImmutableSet.copyOf(enabledManifestSource.keySet());
  }

  public Set<ProjectBranchKey> getSubscribedProjects() {
    return ImmutableSet.copyOf(subscribedRepos.rowKeySet());
  }

  public ImmutableTable<ProjectBranchKey, String, Map<String, Set<
      com.amd.gerrit.plugins.manifestsubscription.manifest.Project>>> getSubscribedRepos() {
    return new ImmutableTable.Builder<ProjectBranchKey, String, Map<String, Set<
      com.amd.gerrit.plugins.manifestsubscription.manifest.Project>>>().putAll(subscribedRepos).build();
  }

  @Override
  public void start() {
    ProjectConfig config;
    for (Project.NameKey p : projectCache.all()) {
      //TODO parallelize parsing/load up?
      try {
        config = ProjectConfig.read(metaDataUpdateFactory.create(p));
        loadStoreFromProjectConfig(p.toString(), config);

      } catch (IOException | ConfigInvalidException | JAXBException e) {
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
                       ProjectCache projectCache,
                       GitReferenceUpdated gitRefUpdated) {
    this.metaDataUpdateFactory = metaDataUpdateFactory;
    this.gitRepoManager = gitRepoManager;
    this.pluginName = pluginName;
    this.projectCache = projectCache;
    this.gitRefUpdated = gitRefUpdated;
  }

  @Override
  public void onGitReferenceUpdated(Event event) {
    String projectName = event.getProjectName();
    String refName = event.getRefName();
    String branchName = refName.startsWith("refs/heads/") ?
        refName.substring(11) : refName;
    ProjectBranchKey pbKey = new ProjectBranchKey(projectName, branchName);

    if (event.getNewObjectId().equals(ObjectId.zeroId().toString())) {
      // This happens when there's a branch deletion and possibly other events
      log.info("Project: " + projectName +
               "\nrefName: " + refName);
    } else if (REFS_CONFIG.equals(refName)) {
      // possible change in enabled repos
      processProjectConfigChange(event);
    } else if (enabledManifestSource.containsKey(projectName) &&
        enabledManifestSource.get(projectName)
            .getBranches().contains(branchName)) {
      processManifestChange(event, projectName, branchName);

    } else if (subscribedRepos.containsRow(pbKey)) {
      //updates in subscribed repos
      processRepoChange(event.getNewObjectId(), projectName, pbKey);
    }
  }

  void processRepoChange(String refUpdatedHash, String projectName,
                                 ProjectBranchKey pbKey) {
    // Manifest store and branch
    Map<String, Map<String, Set<
            com.amd.gerrit.plugins.manifestsubscription.manifest.Project>>>
        destinations = subscribedRepos.row(pbKey);

    for (String store : destinations.keySet()) {
      for (String storeBranch : destinations.get(store).keySet()) {
        Set<com.amd.gerrit.plugins.manifestsubscription.manifest.Project> ps
            = destinations.get(store).get(storeBranch);

        Manifest manifest = manifestStores.get(store, storeBranch);
        String manifestSrc = manifestSource.get(store, storeBranch);
        StringBuilder extraCommitMsg = new StringBuilder();

        Project.NameKey p = new Project.NameKey(projectName);
        try (Repository r = gitRepoManager.openRepository(p);
             RevWalk walk = new RevWalk(r)) {

          RevCommit c = walk.parseCommit(
              ObjectId.fromString(refUpdatedHash));

          extraCommitMsg.append(refUpdatedHash.substring(0,7));
          extraCommitMsg.append(" ");
          extraCommitMsg.append(projectName);
          extraCommitMsg.append(" ");
          extraCommitMsg.append(c.getShortMessage());
        } catch (IOException e) {
          e.printStackTrace();
        }

        // these are project from the above manifest previously
        // cached in the lookup table
        for (com.amd.gerrit.plugins.manifestsubscription.manifest.Project
            updateProject : ps) {
          updateProject.setRevision(refUpdatedHash);
        }

        try {
          Utilities.updateManifest(gitRepoManager, metaDataUpdateFactory,
                  gitRefUpdated, store, STORE_BRANCH_PREFIX + storeBranch,
              manifest, manifestSrc, extraCommitMsg.toString(), null);
        } catch (JAXBException | IOException e) {
          e.printStackTrace();
        }

      }
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
    try {
      VersionedManifests versionedManifests = parseManifests(event);
      processManifestChange(versionedManifests, projectName, branchName);
    } catch (JAXBException | IOException | ConfigInvalidException e) {
      e.printStackTrace();
    }

  }

  private void processManifestChange(VersionedManifests versionedManifests,
                                     String projectName, String branchName) {
    //possible manifest update in subscribing repos
    //TODO Fix, right now update all manifest every time
    //TODO even when only one of the manifest has changed

    try {
      if (versionedManifests != null) {
        CanonicalManifest cManifest = new CanonicalManifest(versionedManifests);
        Set<String> manifests = versionedManifests.getManifestPaths();
        Manifest manifest;
        String store = enabledManifestSource.get(projectName).getStore();
        Table<String, String, String> lookup = HashBasedTable.create();

        // Remove old manifest from subscription if destination store and branch
        // matches manifest source being updated
        // TODO again, this assume 1-1 map between store and manifest store
        Map<String,
            Set<com.amd.gerrit.plugins.manifestsubscription.manifest.Project>> branchPaths;
        for (Table.Cell<ProjectBranchKey, String,
            Map<String,
                Set<com.amd.gerrit.plugins.manifestsubscription.manifest.Project>>> cell :
            subscribedRepos.cellSet()) {
          if (store.equals(cell.getColumnKey())) {
            branchPaths = cell.getValue();

            Iterator<String> iter = branchPaths.keySet().iterator();
            String branchPath;
            while (iter.hasNext()) {
              branchPath = iter.next();
              if (branchPath.startsWith(branchName+"/")) {
                iter.remove();
                manifestStores.remove(store, branchPath);
                manifestSource.remove(store, branchPath);
              }
            }
          }
        }

        //TODO need to make sure remote is pointing to this server?
        //TODO this may be impossible
        //TODO only monitor projects without 'remote' attribute / only using default?

        for (String path : manifests) {
          String bp = branchName + "/" + path;
          try {
            manifest = cManifest.getCanonicalManifest(path);

            watchCanonicalManifest(manifest, store, bp, projectName);

            VersionedManifests.affixManifest(gitRepoManager, manifest, lookup);
            //save manifest
            //TODO added the m/ to the ref to to work around LOCK_FAILURE error of creating master/bla/bla
            //TODO (because default master ref already exists) better solution?
            updateManifest(store, STORE_BRANCH_PREFIX + bp,
                           manifest, projectName);

          } catch (ManifestReadException | GitAPIException e) {
            e.printStackTrace();
          }

        }

      }
    } catch (JAXBException | IOException e) {
      e.printStackTrace();
    }

  }

  private void watchCanonicalManifest(Manifest manifest, String store,
                                      String branchPath, String manifestSrc) {
    String defaultBranch;
    if (manifest.getDefault() != null &&
        manifest.getDefault().getRevision() != null) {
      defaultBranch = manifest.getDefault().getRevision();
    } else {
      defaultBranch = "";
    }

    manifestStores.put(store, branchPath, manifest);
    manifestSource.put(store, branchPath, manifestSrc);

    List<com.amd.gerrit.plugins.manifestsubscription.manifest.Project> projects
        = manifest.getProject();
    watchProjectsInCanonicalManifest(store, branchPath, defaultBranch, projects);
  }

  private void watchProjectsInCanonicalManifest(String store, String branchPath,
                                                String defaultBranch,
                                                List<com.amd.gerrit.plugins.manifestsubscription.manifest.Project> projects) {
    ProjectBranchKey pbKey;
    for (com.amd.gerrit.plugins.manifestsubscription.manifest.Project project : projects) {
      if (manifestStores.containsRow(project.getName()) &&
          !manifestStores.row(project.getName()).isEmpty()) {
        // Skip if it's one of the repo for storing
        // manifest to avoid infinite loop
        // This is a bit too general, but it's done to avoid the complexity
        // of actually tracing out the loop
        // i.e. manifest1->store2 --> manifest2->store1
        continue;
      }

      String branch = project.getRevision() == null ?
          defaultBranch : project.getRevision();
      pbKey = new ProjectBranchKey(project.getName(),
          Repository.shortenRefName(branch));


      //TODO only update manifests that changed
      if (!subscribedRepos.contains(pbKey, store)) {
        subscribedRepos.put(pbKey, store,
            Maps.<String, Set<com.amd.gerrit.plugins.manifestsubscription.manifest.Project>>newHashMap());
      }

      Map<String,
          Set<com.amd.gerrit.plugins.manifestsubscription.manifest.Project>> ps;
      ps = subscribedRepos.get(pbKey, store);
      if (!ps.containsKey(branchPath)) {
        ps.put(branchPath,
            Sets.<com.amd.gerrit.plugins.manifestsubscription.manifest.Project>newHashSet());
      }
      ps.get(branchPath).add(project);

      if (project.getProject().size() > 0) {
        watchProjectsInCanonicalManifest(store, branchPath, defaultBranch,
            project.getProject());
      }
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
          //TODO FIX assume unique store for each manifest source (1-1 map)
          manifestStores.row(oldStore).clear();
          manifestSource.row(oldStore).clear();
          enabledManifestSource.remove(event.getProjectName());

          Iterator<Table.Cell<ProjectBranchKey, String, Map<String,
              Set<com.amd.gerrit.plugins.manifestsubscription.manifest.Project>>>> iter =
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
    } catch (IOException | ConfigInvalidException | JAXBException e) {
      e.printStackTrace();
    }
  }

  private void loadStoreFromProjectConfig(String projectName,
                                          ProjectConfig config)
      throws JAXBException, IOException, ConfigInvalidException {
    String newStore =
        config.getPluginConfig(pluginName).getString(KEY_STORE);

    if (newStore != null) {
      newStore = newStore.trim();
      if (!newStore.isEmpty()) {
        Set<String> branches = Sets.newHashSet(
            config.getPluginConfig(pluginName)
                .getStringList(KEY_BRANCH));

        if (branches.size() > 0) {
          PluginProjectConfig ppc = new PluginProjectConfig(newStore, branches);

          enabledManifestSource.put(projectName, ppc);
          Project.NameKey nameKey = new Project.NameKey(projectName);
          VersionedManifests versionedManifests;
          for (String branch : branches) {
            versionedManifests = parseManifests(nameKey, branch);
            processManifestChange(versionedManifests, projectName, branch);
          }
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

    Repository repo = gitRepoManager.openRepository(p);
    ObjectId commitId = repo.resolve(refName);
    MetaDataUpdate update = metaDataUpdateFactory.create(p);
    VersionedManifests vManifests = new VersionedManifests(refName);
    vManifests.load(update, commitId);

    return vManifests;
  }

  private void updateManifest(String projectName, String refName,
                              Manifest manifest, String manifestSrc)
      throws JAXBException, IOException {
    Utilities.updateManifest(gitRepoManager, metaDataUpdateFactory, gitRefUpdated,
        projectName, refName, manifest, manifestSrc, "", null);
  }

}
