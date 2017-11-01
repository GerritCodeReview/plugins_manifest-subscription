In the manifest project's project.config in refs/meta/config, set the following:

```
[plugin "manifest-subscription"]
  store = "repo/name/on/the/server"
  branch = "branch-being-monitored-in-this-repo"
  branch = "another-branch-being-monitored-in-this-repo"
  branch = "master"
```

There should be only one value for store.  Zero or more values for branch, if no branch is specified, all will be monitored.

[Back to @PLUGIN@ documentation index][index]

[index]: index.html