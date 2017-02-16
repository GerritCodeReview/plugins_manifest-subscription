@PLUGIN@ tag
==============

NAME
----
@PLUGIN@ tag - Tag all projects on the server described by a manifest
on the server.

SYNOPSIS
--------
```
ssh -p @SSH_PORT@ @SSH_HOST@ @PLUGIN@ tag
  {-r/--manifest-repo <manifest repo>}
  {-c/--manifest-commit-ish <manifest commit-ish>}
  {-p/--manifest-path <manifest path>}
  {-t/--new-tag <new tag name>}
  [--help]
```

ACCESS
------
Caller must be a member of the privileged 'Administrators' group

OPTIONS
-------

`-r/--manifest-repo <manifest repo>`
`-c/--manifest-commit-ish <manifest commit-ish>`
`-p/--manifest-path <manifest path>`
: The manifest the taging operation is based on

`-t/--new-tag <new tag name>`
: The name of the tag that will be created on all projects in the manifest specified above

EXAMPLES
--------
Tag all projects on the server described in the `default.xml` manifest in commit 5f51acb585b6a of repo `demo/build_manifest` to tag `releases/1.0.0`

```
ssh -p @SSH_PORT@ @SSH_HOST@ @PLUGIN@ tag -r demo/build_manifest -c 5f51acb585b6a -p default.xml -t releases/1.0.0
```

Tag all projects on the server described in the `default.xml` manifest in commit v0.9-15-g5f51acb of repo `demo/build_manifest` to tag `releases/1.0.0`

```
ssh -p @SSH_PORT@ @SSH_HOST@ @PLUGIN@ tag -r demo/build_manifest -c v0.9-15-g5f51acb -p default.xml -t releases/1.0.0
```

SEE ALSO
--------
* [@PLUGIN@ branch SSH command](cmd-branch.md)

[Back to @PLUGIN@ documentation index][index]

[index]: index.html
