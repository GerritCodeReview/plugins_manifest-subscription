In the manifest project's project.config in refs/meta/config, set the following:

```
[plugin "manifest-subscription"]
  store = "repo/name/on/the/server"
  branch = "branch-being-monitored-in-this-repo"
  branch = "another-branch-being-monitored-in-this-repo"
  branch = "master"
```

There should be only one value for store.  More than one value for branch.

[Back to @PLUGIN@ documentation index][index]

[index]: index.html