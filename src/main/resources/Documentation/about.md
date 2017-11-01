This plugin allows users to monitor git-repo manifests in manifest repositories
and generate rev-specific manifests (similar to "repo manifest -o") and store
them to a separate git repository (configurable.)

> **Warning: this plugin is currently under development and is targeting 2.14.1**

The rev-specific manifest of each source manifest is stored in its own branch
with the following naming convention:
m/\<source manifest branch\>/\<source manifest path\>

The name of the rev-specific manifest is default.xml

For example, if the source manifests are
branch: master
file: default.xml
file: dev.xml
file: upstream-mirror.xml

There will be three *branches* in the destination repository with these names:
m/master/default.xml
m/master/dev.xml
m/master/upstream-mirror.xml

If a tag is placed at the start of one of these branches, "git describe" can be
used to provide system-level version metadata.  One can also use "git bisect" on
the branches to identify system-level regression.

The subject line of the change that triggered the snapshot manifest is included
in the commit message of the snapshot manifest.  This way, you can get a summary
sequence of changes of all repository described by that manifest by running the
following git log command in the snapshot manifest branch:

```
git log --pretty=format:%b
```

* Enabled by project.config in the manifest project to be monitored
* Manifest class is generated from 'repo help manifest' DTD using JAXB
(part of the build step)
* \<include\>s are expanded similar to "repo manifest -o" (only works for
manifests in the same source manifest repository reference by relative path)
* Supports \<remove-project\>
* Circular update loop is prevented by not monitoring any of the snapshot
manifest repositories
* Projects in the manifests are assumed to be on the server.  (There is no check
currently so it's a potential source of error.)
* Snapshot manifests are generated when the source manifests are updated
* Parsed manifests are kept in memory for quick lookup


Development Notes:
------------------
Entry points for this plugin are:
 * ManifestSubscription.onGitReferenceUpdated() (for event driven activities)
 * SshModule (for ssh commands)
 * HttpModule (for REST/http commands)

```
xjc -dtd -d gen -p com.amd.gerrit.plugins.manifestsubscription.manifest manifest.dtd
```

Manifest represent raw XML

CanonicalManifest resolve \<include\> and \<remove-project\>

* TODO: possibly separate out manifest op into a separate plugin and make VesionedManifest as a libary (manifest-base or something)
* TODO: generate snapshot manifest for all commit (Currently only generated on a per push/ref-updated basis)
  * Make it configurable, possibly per project, using plugin config or via special tag in manifest
* TODO: ssh command to check what is being monitored
* TODO: strict mode, only monitor projects using default (no remote)
* TODO: split mode (store all manifests in same branch structure as source instead of flattening the underlying file strcuture)
* TODO: not monitor non-local projects
* TODO: provide hook for non-local projects to trigger snapshot update

* TODO: add something to prevent loop even if store repo is the same as source
manifest repo

* TODO: check project.config on newly created project

* TODO: include an external manifest DTD or XML schema/XSD
* TODO add test verify manifest that can cause circular conditions
* TODO sub dir include manifest have same level include working


