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

import java.util.Objects;

public class ProjectBranchKey {
  private String project;
  private String branch;

  public String getBranch() {
    return branch;
  }

  public String getProject() {
    return project;
  }


  public ProjectBranchKey(String project, String branch) {
    this.project = project;
    this.branch = branch;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProjectBranchKey that = (ProjectBranchKey) o;
    return Objects.equals(project, that.project) &&
        Objects.equals(branch, that.branch);
  }

  @Override
  public int hashCode() {
    return Objects.hash(project, branch);
  }

  @Override
  public String toString() {
    return com.google.common.base.Objects.toStringHelper(this)
        .add("project", project)
        .add("branch", branch)
        .toString();
  }
}