#!/bin/sh
#=============================================================================
#
#  File    : docker
#  Date    : March, 2018
#  Author  : Oleg Lodygensky
#
#  Change log:
#  - Jul 3rd,2017 : Oleg Lodygensky; creation
#
#  OS      : Linux, mac os x
#
#  Purpose : this script checks docker usage on worker side
#
# Some environment variables, automatically set by the volunteer resource:
#  - XWJOBUID : this must contain the job UID on worker side
#  - XWSCRATCHPATH : this must contains the directory where drive are stored
#  - XWCPULOAD : this contains expected CPU load in percentage
#  - XWRAMSIZE : this contains expected RAM size
#  - XWDISKSPACE : this contains expected storage capacity
#  - XWPORTS  : this may contain a comma separated ports list
#               ssh  port forwarding localhost:$XWPORTS[0] to guest:22
#               http port forwarding localhost:$XWPORTS[1] to guest:80

#
#  !!!!!!!!!!!!!!!!    DO NOT EDIT    !!!!!!!!!!!!!!!!
#  Remarks : this script is auto generated by install process
#
#=============================================================================


# Copyrights     : CNRS
# Author         : Oleg Lodygensky
# Acknowledgment : XtremWeb-HEP is based on XtremWeb 1.8.0 by inria : http://www.xtremweb.net/
# Web            : http://www.xtremweb-hep.org
#
#      This file is part of XtremWeb-HEP.
#
# Copyright [2018] [CNRS]
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#
#




THISOS=`uname -s`

case "$THISOS" in

  Darwin )
    DATE_FORMAT='+%Y-%m-%d %H:%M:%S%z'
    ;;

  Linux )
    DATE_FORMAT='--rfc-3339=seconds'
    ;;

  * )
    fatal  "OS not supported ($THISOS)"  TRUE
    ;;

esac

#=============================================================================
#
#  Function  fatal (Message, Force)
#
#=============================================================================
fatal ()
{
  msg="$1"
  FORCE="$2"
  [ "$msg" ]  ||  msg="Ctrl+C"

  echo  "$(date "$DATE_FORMAT")  $SCRIPTNAME  FATAL : $msg"

  [ "$FORCE" = "TRUE" ]  &&  clean


  exit 1
}

#=============================================================================
#
#  Function  warning (Message)
#
#=============================================================================
warning ()
{
  msg="$1"
  echo  "$(date "$DATE_FORMAT")  $SCRIPTNAME  WARNING : $msg"
  return 0
}

#=============================================================================
#
#  Function  info (Message)
#
#=============================================================================
info ()
{
  msg="$1"
  echo  "$(date "$DATE_FORMAT")  $SCRIPTNAME  INFO : $msg"
  return 0
}

#=============================================================================
#
#  Function  debug (Message)
#
#=============================================================================
debug ()
{
  msg="$1"
  [ "${TESTINGONLY}" = "TRUE" ] && echo  "$(date "$DATE_FORMAT")  $SCRIPTNAME  DEBUG : $msg"
  return 0
}

#=============================================================================
#
#  Function  clean ()
#
#=============================================================================
clean ()
{
  echo
  info_message  "clean '${CONTAINERNAME}'"

  [ "$VERBOSE" ]  &&  echo  > /dev/stderr
  [ "${CONTAINERNAME}" ]  ||  return
  }

#=============================================================================
#
#  Function  usage ()
#
#=============================================================================
usage()
{
cat << END_OF_USAGE
  This script is an example only to show how to start a Docker container
  on a distributed volunteer resource.

  Some environment variables, automatically set by the volunteer resource:
  - XWJOBUID : this must contain the job UID on worker side
  - XWSCRATCHPATH : this must contains the directory where drive are stored
  - XWRAMSIZE : this may contain expected RAM size
  - XWDOCKERIMAGE : this may contain docker image name
  - XWDISKSPACE : this may contain expected storage capacity
  - XWPORTS  : this may contain a comma separated ports list
               ssh  port forwarding localhost:$XWPORTS[0] to guest:22
               http port forwarding localhost:$XWPORTS[1] to guest:80

  This script does not permit the Dockerfile usage to build Docker image.
  If Dockerfile is found, this script fails.

END_OF_USAGE

  exit 0
}


#=============================================================================
#
#  Main
#
#=============================================================================
trap  fatal  INT  TERM



SCRIPTNAME="$(basename "$0")"

if [ "${SCRIPTNAME#*.sh}" ]; then
  SCRIPTNAME=xwstartdocker.sh
  VERBOSE=TRUE
  TESTINGONLY='FALSE'                         # Worker, so debug is NOT possible
else
  VERBOSE=''
  TESTINGONLY='FALSE'                         # Local machine, so debug is possible
fi

if [ "$TESTINGONLY" = "TRUE" ] ; then
  XWJOBUID="$(date '+%Y-%m-%d-%H-%M-%S')"
  XWSCRATCHPATH="$(dirname "$0")"
  SAVDIR=`pwd`
  cd "$XWSCRATCHPATH"
  XWSCRATCHPATH=`pwd`
  cd "$SAVDIR"
  XWCPULOAD=100
else
  [ -z "$XWJOBUID" ] && fatal "XWJOBUID is not set"
  [ -z "$XWSCRATCHPATH" ] && fatal "XWSCRATCHPATH is not set"
  [ -z "$XWCPULOAD" ] && fatal "XWCPULOAD is not set"
  [ -z "$XWRAMSIZE" ] && fatal "XWRAMSIZE is not set"
  [ -z "$XWDISKSPACE" ] && fatal "XWDISKSPACE is not set"
fi


IMAGENAME=""
CONTAINERNAME="xwcontainer_${XWJOBUID}"
DOCKERFILENAME="Dockerfile"


while [ $# -gt 0 ]; do

  case "$1" in

    --help )
      usage
      ;;

    --verbose | --debug )
      VERBOSE=1
      set -x
      ;;

    --xwimage )
	  shift
	  IMAGENAME=$1
      ;;

  	* )
  	  ARGS="$ARGS $1"
  	  ;;
  esac

  shift

done

[ ! -z ${XWDOCKERIMAGE} ] && IMAGENAME="${XWDOCKERIMAGE}"

if [ -f ${DOCKERFILENAME} ] ; then
#    IMAGENAME="xwimg_${XWJOBUID}"
#    docker build --force-rm --tag ${IMAGENAME} .
    fatal "Dockerfile is not supported"
fi

#
# --stop-timeout (SIGKILL)
# --memory BYTES
# --cpus 1
#

PDOARGS=""
if [ ! -z "${PDOENABLED}" ] ; then
	PDOARGS="--device=/dev/isgx -v /var/run/aesmd:/var/run/aesmd"
fi

ENVFILENAME="/tmp/env_${XWJOBUID}.list"
printenv | grep -vE "HOSTNAME|TERM|LS_COLORS|PATH|PWD|SHLVL|HOME|_|SHELL|TERM|SSH|LC_|LANG|LOG|XDG_RUNTIME_DIR|LESS|USER|MAIL"> ${ENVFILENAME}
docker pull ${IMAGENAME} 2>&1 >/dev/null
docker run ${PDOARGS} -v $(pwd):/iexec --rm --name ${CONTAINERNAME} --env-file ${ENVFILENAME} ${IMAGENAME} ${ARGS} 2>&1 |  grep -vE "Unable to find image|Pulling from|Pull complete|Digest:|Status:|: Pulling fs layer|: Verifying Checksum|: Download complete|: Already exists"



rm ${ENVFILENAME}

# clean everything
if [ "$TESTINGONLY" != "TRUE" ] ; then
  docker rmi ${IMAGENAME} > /dev/null  2>&1
fi


exit 0
###########################################################
#     EOF        EOF     EOF        EOF     EOF       EOF #
###########################################################
