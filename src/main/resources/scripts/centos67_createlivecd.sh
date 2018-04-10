#!/bin/sh

# Acknowledgment : XtremWeb-HEP is based on XtremWeb 1.8.0 by inria : http://www.xtremweb.net/
# Web            : http://www.xtremweb-hep.org
#
#      This file is part of XtremWeb-HEP.
#
# Copyright [2018] [CNRS] Oleg Lodygensky
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

#  ******************************************************************
#  File    : centos67_createlivecd.sh 
#  Date    : Feb 29th, 2016
#  Author  : Oleg Lodygensky
# 
#  OS      : CentOS 6.7
#  Arch    : 64bits
# 
#  Purpose : this script creates a new Scientific Linux LiveCD
#
#  See     : misc/*.ks; xwcontext_prologue; xwcontext_epilogue
#
#  Requirements: xwcontext_prologue and xwcontext_epilogue scripts
#
# -1- The created Live CD which content is define in the kickstart file
#
# -2- The created Live CD is configured as follow:
#   - Root access denied except if authorized_keys is provided at LiveCD creation time
#     (see -3- below)
#   - network access customized if iptables_rules.sh provided
#     (see -3- below)
#   - a non privileged user "vmuser"
#   - mount points : see xwcontext_prologue.sh
#
#  -3- Optional files may be installed in the resulted LiveCD
#     - authorized_keys installed in /root/.ssh/authorized_keys2 to allow root connection
#     - iptables_rules.sh installed in /root/
#     - user.packages, a text file, containing a list of optional packages to install
#     - user.hostname, a text file, containing the expected host name
#     - *.rpm are installed
# 
# Changelog:
#
#
#
#  !!!!!!!!!!!!!!!!    DO NOT EDIT    !!!!!!!!!!!!!!!!
#  
#  ******************************************************************

CURRENTDIR=`pwd`
ROOTDIR=`dirname $0`
KSFILE=$ROOTDIR/centos67_createlivecd.ks

PROLOGUE_NAME=xwcontext_prologue
PROLOGUE_FILE=$ROOTDIR/$PROLOGUE_NAME
EPILOGUE_NAME=xwcontext_epilogue
EPILOGUE_FILE=$ROOTDIR/$EPILOGUE_NAME

[ "$1" != "" ] && KSFILE=$1
 
if [ ! -f $KSFILE ] ; then
  echo "FATAL : kickstart file not found ($KSFILE)"
  exit 1
fi

echo "Kickstart file = $KSFILE"

if [ ! -f $PROLOGUE_FILE ] ; then
  echo "WARN : prologue not found ($PROLOGUE_FILE)"
fi
if [ ! -f $EPILOGUE_FILE ] ; then
  echo "WARN : epilogue not found ($EPILOGUE_FILE)"
fi

if [ ! -f $ROOTDIR/authorized_keys ] ; then
  echo "WARN : pub key not found ($ROOTDIR/authorized_keys) : root access not allowed"
fi

if [ ! -f $ROOTDIR/iptables_rules.sh ] ; then
  echo "WARN : iptables rules not found ($ROOTDIR/iptables_rules.sh) : LAN access allowed"
fi


#
# Custom host name
#
USERHOSTNAME="user.hostname"
USERHOSTNAMEFILE=$CURRENTDIR/$USERHOSTNAME
CUSTOMHOSTNAME="xwlivecd_centos67.localdomain"
if [ -r $USERHOSTNAMEFILE ] ; then
  CUSTOMHOSTNAME=`cat $USERHOSTNAMEFILE` 
  echo "INFO : custom host name found : $CUSTOMHOSTNAME"
else
  echo "INFO : custom host name not found; using default $CUSTOMHOSTNAME"
fi

KSCFILENAME=$ROOTDIR/centos_createlivecd.ks_$CUSTOMHOSTNAME
KSCFILE=/tmp/$KSCFILENAME

cat $KSFILE | sed "s/%CUSTOMHOSTNAME%/$CUSTOMHOSTNAME/g" > $KSCFILE

#setarch amd64 LANG=C livecd-creator --config=$KSCFILE --fslabel=XWCD-centos7_$CUSTOMHOSTNAME --tmpdir=$LIVETMPDIR

yum -y update
yum -y install epel-release
yum -y install livecd-tools xz pyliblzma cyrus-sasl-lib



LIVETMPDIR=/mnt/xwscratch/livecdcentos67/$CUSTOMHOSTNAME
rm -Rf $LIVETMPDIR 
mkdir -p $LIVETMPDIR
if [ $? -eq 0 ] ; then
  echo "LANG=C livecd-creator --config=$KSCFILE --fslabel=$CUSTOMHOSTNAME --tmpdir=$LIVETMPDIR"
  sleep 1
  LANG=C livecd-creator --config=$KSCFILE --fslabel=$CUSTOMHOSTNAME --tmpdir=$LIVETMPDIR
else
  echo "FATAL : can't create dir $LIVETMPDIR"
fi
