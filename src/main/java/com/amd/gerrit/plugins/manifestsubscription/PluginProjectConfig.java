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

import java.util.Set;

public class PluginProjectConfig {
  private String store;
  private Set<String> branches;

  public PluginProjectConfig(String store, Set<String> branches) {
    this.store = store;
    this.branches = branches;
  }

  public String getStore() {
    return store;
  }

  public void setStore(String store) {
    this.store = store;
  }

  public Set<String> getBranches() {
    return branches;
  }

  public void setBranches(Set<String> branches) {
    this.branches = branches;
  }
}
