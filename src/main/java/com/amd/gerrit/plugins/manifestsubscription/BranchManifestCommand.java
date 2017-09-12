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

import com.google.gerrit.common.data.GlobalCapability;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.server.extensions.events.GitReferenceUpdated;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.MetaDataUpdate;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.inject.Inject;
import org.kohsuke.args4j.Option;

@RequiresCapability(GlobalCapability.ADMINISTRATE_SERVER)
@CommandMetaData(name = "branch", description = "branch all projects described in a manifest on the server")
public class BranchManifestCommand extends SshCommand {

  @Inject
  private GitRepositoryManager gitRepoManager;

  @Inject
  private MetaDataUpdate.Server metaDataUpdateFactory;

  @Inject
  private GitReferenceUpdated gitRefUpdated;

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

  @Option(name = "-o", aliases = {"--output-type"},
      usage = "", required = false)
  private Utilities.OutputType outputType;

  @Option(name = "-nr", aliases = {"--new-manifest-repo"},
      depends = {"-nb", "-np"},
      usage = "", required = false)
  private String newManifestRepo;

  @Option(name = "-nb", aliases = {"--new-manifest-branch"},
      depends = {"-nr", "-np"},
      usage = "", required = false)
  private String newManifestBranch;

  @Option(name = "-np", aliases = {"--new-manifest-path"},
      depends = {"-nb", "-nr"},
      usage = "", required = false)
  private String newManifestPath;

  @Option(name = "-cs", aliases = {"--create-snapshot-branch"},
      depends = {"-nb", "-nr", "-np"},
      usage = "", required = false)
  private boolean createSnapShotBranch;

  @Override
  protected void run() {
    stdout.println("Branching manifest:");
    stdout.println(manifestRepo);
    stdout.println(manifestCommitish);
    stdout.println(manifestPath);

    stdout.println(newManifestRepo);
    stdout.println(newManifestBranch);
    stdout.println(newManifestPath);
    stdout.println("Create snapshot branch: " + createSnapShotBranch);

    Utilities.branchManifest(gitRepoManager, metaDataUpdateFactory, gitRefUpdated,
        manifestRepo, manifestCommitish, manifestPath,
        newBranch,
        newManifestRepo, newManifestBranch, newManifestPath,
        createSnapShotBranch,
        stdout, stderr,
        outputType==Utilities.OutputType.JSON);

  }
}
