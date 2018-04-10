#!/bin/sh

# Acknowledgment : XtremWeb-HEP is based on XtremWeb 1.8.0 by inria : http://www.xtremweb.net/
# Web            : http://www.xtremweb-hep.org
# 
#      This file is part of XtremWeb-HEP.
#
# Copyright [2018] [CNRS] Simon Dadoun, Oleg Lodygensky
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

exec > /tmp/context.log 2>&1

date


#
# install public key to connect as "vmuser", a non privileged user
# it is the user responsibility to copy its public key 
# in the contextualization disk before launching the customizable VM
#
echo "Copying files to /home/vmuser/.ssh"
mkdir -p /home/vmuser/.ssh

# id_rsa.pub is the end user public key to connect to the VM

CONTEXTDIR=/context
# Mount point changed since Sept, 3rd, 2012 
[ -d /mnt/xwcontext ] && CONTEXTDIR=/mnt/xwcontext  

echo "Copying $CONTEXTDIR/id_rsa.pub to /home/vmuser/.ssh/authorized_keys"
cp $CONTEXTDIR/id_rsa.pub /home/vmuser/.ssh/authorized_keys
# Ubuntu LiveCD requires next
cp $CONTEXTDIR/id_rsa.pub /home/vmuser/.ssh/authorized_keys2

chmod 700 /home/vmuser/.ssh/
chmod 600 /home/vmuser/.ssh/*



#
# To create a new customized LiveCD for Scientific Linux 5
# you should call 
# either /usr/local/sbin/sl5_createlivecd.sh
# or    /usr/local/sbin/ubuntu_createlivecd.sh
#
# Please refer to these scripts inline documentation
#