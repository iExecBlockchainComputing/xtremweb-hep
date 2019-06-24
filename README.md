# Deprecated

Warning!! This repository is deprecated and not used anymore. It was used in the version 2 of iexec. Now, since version 3, the project to use should be https://github.com/iExecBlockchainComputing/iexec-core.

---------------------------------------------------------------

Data driven volunteer cloud

### Status
[![Build Status](https://travis-ci.org/lodygens/xtremweb-hep.svg?branch=master)](https://travis-ci.org/lodygens/xtremweb-hep)

Quick start
===========

The easiest way is by using Docker.
Please see [Docker deployment](docker/master).

## Manual Build
### Pre-requisites

The following should be already installed on the machine:
1. *make*
2. *ant*
3. *Java JDK* (The variable *JAVA_HOME* should also be set.)

### Build
[Gradle](http://gradle.org) is used to build the project. You can use either the native gradle command if gradle is available on your system:
```
gradle buildAll
```
or if gradle is not installed, you can use it's wrapped version, simply type:
```
./gradlew buildAll
```

To build Docker images locally
```
gradle buildAll buildImages -Penvironment=docker
```
### Run
After the build, in the folder *build/dist/xtremweb-X.Y.Y-SNAPSHOT/bin*, you can call the server, worker:

For the server:
```
./xtremweb.server console
```

For the worker:
```
./xtremweb.worker console
```
The client's commands can be called natively in the */bin* folder:
1. xwapps
2. xwusers
3. xwworkers
4. ...

### Run in docker
It is also possible to run the scheduler and one worker using docker out of the box. For that, you simply need to call the script 
```
docker-compose-firstinstall.sh
```
in xtremweb-hep/build/dist/xtremweb-X.Y.Z/docker/docker-compose-firstinstall.sh, the first time. After that, if you want to restart the docker containers, you can call the script:
```
docker-compose-simple-run.sh
```
Some variables will be set by default but they can be modified in the file *.env* next to the docker-compose file.

