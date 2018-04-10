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
#  File    : cernvm_dg.sh
#  Date    : Septembre 27th, 2012
#  Author  : Oleg Lodygensky
# 
#  OS      : Scientific Linux 6
#  Arch    : 32bits
# 
#  Purpose : this script modifies CernVM distrib to comply to XWHEP
#
#  !!!!!!!!!!!!!!!!    DO NOT EDIT    !!!!!!!!!!!!!!!!
#  
#  ******************************************************************


progname=`basename $0`
ROOTDIR=`pwd`

INITDIR="/etc/init.d"
PROLOGUE_NAME=xwcontext_prologue
EPILOGUE_NAME=xwcontext_epilogue
PROLOGUE_INITD="$INITDIR/$PROLOGUE_NAME"
EPILOGUE_INITD="$INITDIR/$EPILOGUE_NAME"

echo INITDIR=$INITDIR

#
# configure contextualization
#
if [ ! -f $ROOTDIR/$PROLOGUE_NAME ] ; then
  echo "FATAL : No prologue found"
  exit 1
fi

cp $ROOTDIR/$PROLOGUE_NAME $PROLOGUE_INITD
chmod +x $PROLOGUE_INITD


if [ ! -f $ROOTDIR/$EPILOGUE_NAME ] ; then
  echo "FATAL : No epilogue found"
  exit 1
fi

cp $ROOTDIR/$EPILOGUE_NAME $EPILOGUE_INITD
chmod +x $EPILOGUE_INITD


/sbin/chkconfig --add $PROLOGUE_NAME
/sbin/chkconfig --add $EPILOGUE_NAME


#
# Configure firewall : deny LAN access
#
if [ -f $ROOTDIR/iptables_rules.sh ] ; then
  cp $ROOTDIR/iptables_rules.sh /root/
  chmod +x /root/iptables_rules.sh
  /root/iptables_rules.sh > /root/iptables_rules.out
else
  echo "No iptables rules found : LAN access allowed"
fi

#
# Install creator pub key
#
if [ -f $ROOTDIR/id_rsa.pub ] ; then
  mkdir /root/.ssh
  chmod 600 /root/.ssh
  cp $ROOTDIR/id_rsa.pub  /root/.ssh/authorized_keys
  cp $ROOTDIR/id_rsa.pub  /root/.ssh/authorized_keys2
  chmod 600 /root/.ssh/authorized_keys*
else
  echo "No pub key found : root access not allowed"
fi


#
# configure sudoers
#
echo "vmuser ALL = PASSWD: ALL, NOPASSWD: /sbin/poweroff, NOPASSWD: /sbin/shutdown" >> /etc/sudoers



[ -x /etc/init.d/firstboot ] && /sbin/chkconfig firstboot off

