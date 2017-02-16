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
  [-o/--output-type]
  [-nr/--new-manifest-repo <new manifest repo>]
  [-nb/--new-manifest-branch <new manifest branch>]
  [-np/--new-manifest-path <new manifest path>]
  [-cs/--create-snapeshot-branch]
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

`-nr/--new-manifest-repo <new manifest repo>`
`-nb/--new-manifest-branch <new manifest branch>`
`-np/--new-manifest-path <new manifest path>`
: (optional) A new manifest (to be created) that points to the new branch

`-cs/--create-snapeshot-branch`
: (optional, depends on -nr/-nb/-np) Create a new snapshot manifest branch for
 the newly created manifest (as defined by -nr, -nb, -np) in the repository
 defined by -r

EXAMPLES
--------
Branch all projects on the server described in the `default.xml` manifest in 
commit 5f51acb585b6a of repo `demo/build_manifest` to branch `releases/1.0.0`

```
ssh -p @SSH_PORT@ @SSH_HOST@ @PLUGIN@ branch -r demo/build_manifest -c 5f51acb585b6a -p default.xml -b releases/1.0.0
```

Branch all projects on the server described in the `default.xml` manifest in 
commit v0.9-15-g5f51acb of repo `demo/build_manifest` to branch `releases/1.0.0`
and create a new manifest with path `releases/1.0.0.xml` in repository 
`project/manifest` branch `master`.

```
ssh -p @SSH_PORT@ @SSH_HOST@ @PLUGIN@ branch -r demo/build_manifest -c v0.9-15-g5f51acb -p default.xml -b releases/1.0.0 -nr project/manifest -nb master -np releases/1.0.0.xml -cs
```

In the following charts, commit 'c' is v0.9-15-g5f51acb. 

With -cs:

```
 a---b---c---d---e master
          \
           f       m/master/releases/1.0.0.xml
```

Without -cs:

```
 a---b---c---d---e master
  \
   f               m/master/releases/1.0.0.xml
```

SEE ALSO
--------
* [@PLUGIN@ tag SSH command](cmd-tag.md)

[Back to @PLUGIN@ documentation index][index]

[index]: index.html

