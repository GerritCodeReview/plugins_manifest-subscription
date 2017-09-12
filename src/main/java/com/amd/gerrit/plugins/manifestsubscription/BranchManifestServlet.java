// Copyright (C) 2016 Advanced Micro Devices, Inc.  All rights reserved.
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

import com.google.gerrit.extensions.annotations.Export;
import com.google.gerrit.server.extensions.events.GitReferenceUpdated;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.MetaDataUpdate;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Export("/branch")
@Singleton
public class BranchManifestServlet extends HttpServlet {

  @Inject
  private GitRepositoryManager gitRepoManager;

  @Inject
  private MetaDataUpdate.Server metaDataUpdateFactory;

  @Inject
  private GitReferenceUpdated gitRefUpdated;

  protected void doPost(HttpServletRequest req, HttpServletResponse res)
                                                            throws IOException {

    Map<String, String[]> input = req.getParameterMap();

    if (Utilities.httpInputValid(req)) {
      res.setContentType("application/json");
      res.setCharacterEncoding("UTF-8");

      String newManifestRepo = null;
      String newManifestBranch= null;
      String newManifestPath = null;
      boolean createSnapShotBranch = false;

      if (input.containsKey("new-manifest-repo")) {
        newManifestRepo = input.get("new-manifest-repo")[0];
        newManifestBranch = input.get("new-manifest-branch")[0];
        newManifestPath = input.get("new-manifest-path")[0];
      }

      if (input.containsKey("create-snapshot-branch")) {
        createSnapShotBranch =
            Boolean.parseBoolean(input.get("create-snapshot-branch")[0]);
      }

      Utilities.branchManifest(
          gitRepoManager, metaDataUpdateFactory, gitRefUpdated,
          input.get("manifest-repo")[0],
          input.get("manifest-commit-ish")[0],
          input.get("manifest-path")[0],
          input.get("new-branch")[0],
          newManifestRepo,
          newManifestBranch,
          newManifestPath,
          createSnapShotBranch, res.getWriter(), null, true);

    } else {
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}
