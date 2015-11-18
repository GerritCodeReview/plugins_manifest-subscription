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
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.inject.Inject;

import java.util.Set;

@RequiresCapability(GlobalCapability.ADMINISTRATE_SERVER)
@CommandMetaData(name="show", description="Show states of manifest-subscription")
public class ShowSubscription extends SshCommand {

  @Inject
  private ManifestSubscription manifestSubscription;

  @Override
  protected void run() throws UnloggedFailure, Failure, Exception {
    stdout.println("Enabled manifest repositories:");

    Set<String> repos = manifestSubscription.getEnabledManifestRepos();
    Set<ProjectBranchKey> projects = manifestSubscription.getSubscribedProjects();

    for (String repo : repos) {
      stdout.println(repo);
    }

      stdout.println("");
      stdout.println("Monitoring projects:");

      for (ProjectBranchKey project : projects) {
          stdout.println(project.getProject() + " | " + project.getBranch());
      }
  }
}
