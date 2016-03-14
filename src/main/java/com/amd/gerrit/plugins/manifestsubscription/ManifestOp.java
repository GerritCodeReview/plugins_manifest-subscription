package com.amd.gerrit.plugins.manifestsubscription;

import com.amd.gerrit.plugins.manifestsubscription.manifest.Project;
import com.google.gerrit.server.git.GitRepositoryManager;

public interface ManifestOp {
    boolean apply(Project project, String hash, String name,
                  GitRepositoryManager gitRepoManager);
}
