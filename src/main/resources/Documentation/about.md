This plugin allows users to monitor git-repo manifests in manifest repositories
and generate rev-specific manifests (similar to "repo manifest -o") and store
them to a separate git repository (configurable.)

> **Warning: this plugin is currently under development and is targeting 2.9.1**

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

* Enabled by project.config in the manifest project to be monitored
* Manifest class is generated from 'repo help manifest' DTD using JAXB
* \<include\>s are expanded similar to "repo manifest -o" (only works for
manifests in the same source manifest repository reference by relative path)
* Supports \<remove-project\>
* Circular update loop is prevented by not monitoring any of the rev-specific
manifest repositories
* Projects in the manifests are assumed to be on the server.  (There is no check
currently so it's a potential source of error.)


```
xjc -dtd -d gen -p com.amd.gerrit.plugins.manifestsubscription.manifest manifest.dtd
```

Manifest represent raw XML
CanonicalManifest resolve <include> and <remove-project>

* TODO: keep parsed manifest in memory for quick lookup
* TODO: monitor all manifest branch if no branch is specified
* TODO: ssh command to check what is being monitored
* TODO: stright mode, only monitor projects using default (no remote)
* TODO: split mode (store all manifests in same branch strcuture as source instead of flattening the underlying file strcuture)
* TODO: not monitor non-local projects

* TODO: make sure no circular dependencies (project with manifest subscription is not in the
manifest being monitored.)
* TODO: support changes in manifest

* resolve relative path in include
* supports include tag in manifest

* TODO: check project.config on newkly created project

* TODO: include an external manifest DTD or XML schema/XSD
* generates/xjc classes from DTD/XSD at build time
* TODO add test verify include manifest have original project
* TODO sub dir include manifest have same level include working
