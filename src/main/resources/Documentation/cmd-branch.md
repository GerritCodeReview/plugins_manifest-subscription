@PLUGIN@ branch
==============

NAME
----
@PLUGIN@ branch - Branch all projects on the server described by a manifest
on the server.

SYNOPSIS
--------
```
ssh -p @SSH_PORT@ @SSH_HOST@ @PLUGIN@ branch
  {-r/--manifest-repo <manifest repo>}
  {-c/--manifest-commit-ish <manifest commit-ish>}
  {-p/--manifest-path <manifest path>}
  {-b/--new-branch <new branch name>}
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
: The manifest the branching operation is based on

`-b/--new-branch <new branch name>`
: The name of the branch that will be created on all projects in the manifest specified above

EXAMPLES
--------
Branch all projects on the server described in the `default.xml` manifest in commit 5f51acb585b6a of repo `demo/build_manifest` to branch `releases/1.0.0`
```
ssh -p @SSH_PORT@ @SSH_HOST@ @PLUGIN@ branch -r demo/build_manifest -c 5f51acb585b6a -p default.xml -b releases/1.0.0

```

Branch all projects on the server described in the `default.xml` manifest in commit v0.9-15-g5f51acb of repo `demo/build_manifest` to branch `releases/1.0.0`
```
ssh -p @SSH_PORT@ @SSH_HOST@ @PLUGIN@ branch -r demo/build_manifest -c v0.9-15-g5f51acb -p default.xml -b releases/1.0.0

```
