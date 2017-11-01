## Core Gerrit Settings

The following options can be configured in $GERRIT_SITE/etc/gerrit.config.

```
[plugin "@PLUGIN@"]
  maxBranchesPerRepo = 1000
```

plugin.@PLUGIN@.maxBranchesPerRepo: Specify the maximum number of branches
allowed to monitor for each manifest repository. When not specified, the default
value is 1000.

## Local Project Configuration

In the manifest project's project.config in refs/meta/config, set the following:

```
[plugin "@PLUGIN@"]
  store = "repo/name/on/the/server"
  branch = "branch-being-monitored-in-this-repo"
  branch = "another-branch-being-monitored-in-this-repo"
  branch = "master"
```

There should be only one value for store.  Zero or more values for branch, if no
branch is specified, all will be monitored.

[Back to @PLUGIN@ documentation index][index]

[index]: index.html