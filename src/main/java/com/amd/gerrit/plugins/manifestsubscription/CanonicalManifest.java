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

import com.amd.gerrit.plugins.manifestsubscription.manifest.Include;
import com.amd.gerrit.plugins.manifestsubscription.manifest.Manifest;
import com.amd.gerrit.plugins.manifestsubscription.manifest.Project;
import com.amd.gerrit.plugins.manifestsubscription.manifest.RemoveProject;
import com.google.common.collect.Sets;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class CanonicalManifest {
  private Map<String, Manifest> manifests;

  public CanonicalManifest(VersionedManifests manifests) {
    this.manifests = manifests.getManifests();
  }

  public CanonicalManifest(Map<String, Manifest> manifests) {
    this.manifests = manifests;
  }

  Manifest getCanonicalManifest(String path) throws ManifestReadException {
    if (manifests.containsKey(path)) {
      Manifest manifest = (Manifest) manifests.get(path).clone();

      Manifest includedManifest;
      Path includedPath;
      Iterator<Include> i = manifest.getInclude().listIterator();
      String include;
      while (i.hasNext()) {
        include = i.next().getName();
        includedPath = Paths.get(include);
        i.remove();

        includedManifest = getCanonicalManifest(includedPath.normalize().toString());

        try {
          mergeManifestInto(includedManifest, manifest);
        } catch (Exception e) {
          throw new ManifestReadException(path);
        }

      }
      // Clear remove project after all include manifest is processed
      manifest.getRemoveProject().clear();

      removeNotDefaultProject(manifest);

      return manifest;
    }

    throw new ManifestReadException(path);
  }

  private Manifest mergeManifestInto(Manifest inner, Manifest outer)
      throws Exception {
    if (outer.getDefault() != null && inner.getDefault() != null) {
      throw new Exception();
    }
    if (outer.getNotice() != null && inner.getNotice() != null) {
      throw new Exception();
    }
    if (outer.getManifestServer() != null && inner.getManifestServer() != null) {
      throw new Exception();
    }
    if (outer.getRepoHooks() != null && inner.getRepoHooks() != null) {
      throw new Exception();
    }

    //TODO add more check
    resolveRemoveProject(inner, outer);

    if (outer.getDefault() == null) {
      outer.setDefault(inner.getDefault());
    }
    if (outer.getNotice() == null) {
      outer.setNotice(inner.getNotice());
    }
    if (outer.getManifestServer() == null) {
      outer.setManifestServer(inner.getManifestServer());
    }
    if (outer.getRepoHooks() == null) {
      outer.setRepoHooks(inner.getRepoHooks());
    }


    //TODO name remote name duplication check
    outer.getRemote().addAll(inner.getRemote());

    outer.getProject().addAll(inner.getProject());
    outer.getExtendProject().addAll(inner.getExtendProject());

    return outer;
  }

  private void resolveRemoveProject(Manifest manifest, Set<String> toBeRemoved) {
    Project p;
    Iterator<Project> i = manifest.getProject().listIterator();
    while (i.hasNext()) {
      p = i.next();

      if (toBeRemoved.contains(p.getName())) {
        i.remove();
      }
    }
  }

  private void resolveRemoveProject(Manifest manifest, Manifest toRemove) {
    Set<String> removeProjects = Sets.newHashSet();

    RemoveProject rp;
    Iterator<RemoveProject> i = toRemove.getRemoveProject().listIterator();
    while (i.hasNext()) {
      rp = i.next();
      removeProjects.add(rp.getName());
    }

    if (removeProjects.size() > 0) {
      resolveRemoveProject(manifest, removeProjects);
    }
  }

  private void removeNotDefaultProject(Manifest manifest) {
    Project p;
    Iterator<Project> i = manifest.getProject().listIterator();
    while (i.hasNext()) {
      p = i.next();

      if (p.getGroups() != null && p.getGroups().contains("notdefault")) {
        i.remove();
      }
    }
  }
}
