// Copyright (C) 2017 Advanced Micro Devices, Inc. All rights reserved.
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
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.inject.Inject;
import org.kohsuke.args4j.Option;

@RequiresCapability(GlobalCapability.ADMINISTRATE_SERVER)
@CommandMetaData(name = "project-trigger", description = "Trigger snapshot manifest update for externally updated project")
public class TriggerManifestUpdateCommand extends SshCommand {

  @Inject
  private ManifestSubscription manifestSubscription;

  @Inject
  private GitRepositoryManager gitRepositoryManager;

  @Option(name = "-b", aliases = {"--project-branch"},
          usage = "", required = true)
  private String projectBranch;

  @Option(name = "-n", aliases = {"--project-name"},
          usage = "", required = true)
  private String projectName;

  @Option(name = "-r", aliases = {"--project-hash"},
          usage = "", required = true)
  private String projectHash;

  @Override
  protected void run() {
      Utilities.triggerManifestUpdate(gitRepositoryManager, manifestSubscription,
              projectHash, projectName, projectBranch,
              stdout, false);
  }
}
