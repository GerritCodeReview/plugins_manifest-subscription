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
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Export("/show")
@Singleton
public class ShowSubscriptionServlet extends HttpServlet {
  @Inject
  private ManifestSubscription manifestSubscription;

  protected void doGet(HttpServletRequest req, HttpServletResponse res)
                                                            throws IOException {
    res.setContentType("application/json");
    res.setCharacterEncoding("UTF-8");

    res.getWriter().write("Hello\n");
    Utilities.showSubscription(manifestSubscription, res.getWriter(), true);
  }
}
