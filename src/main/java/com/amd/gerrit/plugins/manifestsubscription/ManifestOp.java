package com.amd.gerrit.plugins.manifestsubscription;

import com.amd.gerrit.plugins.manifestsubscription.manifest.Project;
import com.google.gerrit.server.git.GitRepositoryManager;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;

import java.io.IOException;

public interface ManifestOp {
    boolean apply(Project project, String hash, String name,
                  GitRepositoryManager gitRepoManager) throws RefAlreadyExistsException, InvalidRefNameException, RefNotFoundException, GitAPIException, IOException;
}
