@PLUGIN@ - /tag REST API
==============================
This page describes the project related REST endpoints that are added
by the @PLUGIN@.
Please also take note of the general information on the
[REST API](../../../Documentation/rest-api.html).

tag Endpoint
---------------

_POST /plugins/@PLUGIN@/tag?manifest-repo=repo/name&manifest-commit-ish=commitish&manifest-path=default.xml&new-tag=tag/name_

* manifest-repo: name of the manifest repository that defines the projects to be tagged
* manifest-commit-ish: commit-ish that points to the commit that contain the manifest (tag name, branch name, git describe string, etc.)
* manifest-path: path to the manifest that defines the projects to be tagged
* new-tag: name of the tag to be created for each of the project defined in the manifest above

#### Request
```
http://gerritserver/a/plugins/manifest-subscription/tag?manifest-repo=demo/build_manifest&manifest-commit-ish=m/topic/dupl.xml&manifest-path=default.xml&new-tag=r/1.12.0
```

#### Response
The content of the manifest used by the tagging operation in JSON

```
HTTP/1.1 200
Content-Type: application/json;charset=UTF-8
)]}'
{
  "remote": [
    {
      "name": "testGerrit",
      "fetch": "ssh://gerritserver/",
      "review": "gerritserver"
    }
  ],
  "_default": {
    "remote": {
      "name": "testGerrit",
      "fetch": "ssh://gerritserver/",
      "review": "gerritserver"
    },
    "revision": "master",
    "sync_j": "4"
  },
  "remove_project": [],
  "project": [
    {
      "name": "demo/project1",
      "path": "ws/project1",
      "revision": "ebc8392b32494a03767d794dcaa8c4bcbb538be9",
      "upstream": "master",
      "project": []
    },
    {
      "name": "demo/project2",
      "path": "ws/project2",
      "revision": "f4737c3bf124d5fd9437bbc0883be92219f7f8da",
      "upstream": "master",
      "project": []
    },
    {
      "name": "demo/project3",
      "path": "ext/project3",
      "revision": "ce2b81d9c04d45e2ef5b561c99d08709aa19d300",
      "upstream": "master",
      "project": []
    }
  ],
  "include": []
}
```

SEE ALSO
--------
* [/branch REST endpoint](rest-api-branch.md)

[Back to @PLUGIN@ documentation index][index]

[index]: index.html