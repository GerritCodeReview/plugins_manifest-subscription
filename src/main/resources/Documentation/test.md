Manual test
=================
Until the acceptance framework / AbstractDeamonTest is used for testing, manual test cases:

Projects setup
--------------
* demo/manifest
* demo/project1
* demo/project2
* demo/project3

project.config{refs/meta/config} in demo/manifest
-------------------------------
~~~
[plugin "manifest-subscription"]
    store = "demo/build_manifest"
    branch = "topic"
    branch = "master"
~~~


Manifests setup (branch master)
-------------------------------
* default.xml
  * project 1-3
* less.xml
  * project 1 and 2
* subdir/default.xml
  * project 1-3

Manifests setup (branch topic)
------------------------------
* default.xml
  * project 1-3


Test cases
----------

### Enable refs/meta/config in demo/manifest ###
Snapshot manifests at (branch):
* m/master/default.xml
* m/master/less.xml
* m/master/subdir/default.xml
* m/topic/default.xml

### Toggle refs/meta/config in demo/manifest ###
* No change

### Change demo/project2 non-master branch ###
* No change

### Change less.xml{master} (while subscription is turned off after being on)###
* No change

### Change demo/project3 ###
These snapshot manifests should be updated:
* m/master/default.xml
* m/master/subdir/default.xml
* m/topic/default.xml

### Change less.xml{master} ###
Only this snapshot manifest should be updated:
* m/master/less.xml

### Change demo/project3 (after topic branch is removed from plugin-config)###
These snapshot manifests should be updated:
* m/master/default.xml
* m/master/subdir/default.xml

[Back to @PLUGIN@ documentation index][index]

[index]: index.html