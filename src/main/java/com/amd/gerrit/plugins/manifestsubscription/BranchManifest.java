// Copyright (C) 2015 Advanced Micro Devices, Inc. All rights reserved.
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
import com.google.gerrit.common.data.GlobalCapability;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.inject.Inject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.kohsuke.args4j.Option;

import javax.xml.bind.JAXBException;
import java.io.IOException;

@RequiresCapability(GlobalCapability.ADMINISTRATE_SERVER)
@CommandMetaData(name = "branch", description = "branch all projects described in a manifest on the server")
public class BranchManifest extends SshCommand {

  @Inject
  private GitRepositoryManager gitRepoManager;

  @Option(name = "-r", aliases = {"--manifest-repo"},
          usage = "", required = true)
  private String manifestRepo;

  @Option(name = "-c", aliases = {"--manifest-commit-ish"},
          usage = "", required = true)
  private String manifestCommitish;

  @Option(name = "-p", aliases = {"--manifest-path"},
          usage = "", required = true)
  private String manifestPath;

  @Option(name = "-b", aliases = {"--new-branch"},
      usage = "", required = true)
  private String newBranch;

  @Override
  protected void run() {
    stdout.println("Branching manifest:");
    stdout.println(manifestRepo);
    stdout.println(manifestCommitish);
    stdout.println(manifestPath);


    Project.NameKey p = new Project.NameKey(manifestRepo);
    try {
      Repository repo = gitRepoManager.openRepository(p);
      ObjectId commitId = repo.resolve(manifestCommitish);
      VersionedManifests vManifests = new VersionedManifests(manifestCommitish);
      vManifests.load(repo, commitId);
      CanonicalManifest manifests = new CanonicalManifest(vManifests);

      Manifest manifest = manifests.getCanonicalManifest(manifestPath);

      stdout.println("");
      stdout.println("Branch '" + newBranch +
          "' will be created for the following projects:");
      for (com.amd.gerrit.plugins.manifestsubscription.manifest.Project proj :
            manifest.getProject()) {
        stdout.print(proj.getRevision());
        stdout.print("\t");
        stdout.println(proj.getName());
      }

      VersionedManifests.branchManifest(gitRepoManager, manifest, newBranch);

    } catch (IOException e) {
      e.printStackTrace(stderr);
    } catch (ConfigInvalidException e) {
      e.printStackTrace(stderr);
    } catch (JAXBException e) {
      e.printStackTrace(stderr);
    } catch (ManifestReadException e) {
      e.printStackTrace(stderr);
    } catch (GitAPIException e) {
      e.printStackTrace(stderr);
    }
  }
}
