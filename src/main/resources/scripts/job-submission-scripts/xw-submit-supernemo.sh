#!/bin/sh
#=============================================================================
# Copyrights     : CNRS
# Author         : Etienne Urbah, Oleg Lodygensky
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
#  XtremWeb-HEP demonstration :
#  Submission of the SuperNemo application inside an Ubuntu VM to XtremWeb-HEP
#
#  Prerequisites :  - 'VirtualBox' must be installed
#                   - 'XtremWeb-HEP Client' must be installed
#
#=============================================================================
XW_BIN=$(ls -d /opt/xwhep-client-* | tail -1)/bin

XW_SUPERNEMO_APP_URI=xw://xwserv.lal.in2p3.fr/7b1c140d-12c1-49dd-ad69-2ab3a75c1607

APPNAME=ubuntu11
SSHPORT=2022

echo
echo XW_SUPERNEMO_APP_URI=$XW_SUPERNEMO_APP_URI
echo
echo '------------------------------'
echo 'xwstatus $XW_SUPERNEMO_APP_URI'
echo '------------------------------'
$XW_BIN/xwstatus $XW_SUPERNEMO_APP_URI --xwformat xml  |  perl -pwe 's/" /"\n  /g' |  grep -E  '(name|status|size|uri)='
RC=$?
if [ $RC -ne 0 ]; then exit $RC; fi
echo
echo -n 'Type Enter '; read VOID

echo
echo "-------------------------------------------------------------"
echo "# Folder 'context.d' with files 'context.sh' and 'id_rsa.pub'"
echo "-------------------------------------------------------------"

if [ ! -d context.d ]; then
  echo
  echo ---------------
  echo mkdir context.d
  echo ---------------
  mkdir context.d
  RC=$?
  if [ $RC -ne 0 ]; then exit $RC; fi
fi

if [ ! -f context.d/context.sh ]; then
  echo
  echo '-----------------------------------------------------'
  echo 'cat  << END_OF_CONTEXT_SCRIPT  > context.d/context.sh'
  echo '-----------------------------------------------------'
  cat  << END_OF_CONTEXT_SCRIPT  > context.d/context.sh
# install public key
mkdir -p /home/xwuser/.ssh
cp /context/id_rsa.pub /home/xwuser/.ssh/authorized_keys
chmod 700 /home/xwuser/.ssh/
chmod 600 /home/xwuser/.ssh/authorized_keys
END_OF_CONTEXT_SCRIPT
fi

if [ ! -f context.d/id_rsa.pub ]; then
  if   [ -r ~/.ssh/id_rsa ]; then
    echo
    echo '-----------------------------------------------------'
    echo 'ssh-keygen -y -f ~/.ssh/id_rsa > context.d/id_rsa.pub'
    echo '-----------------------------------------------------'
    ssh-keygen -y -f ~/.ssh/id_rsa > context.d/id_rsa.pub
    RC=$?
    if [ $RC -ne 0 ]; then exit $RC; fi
  elif [ ! -r ~/.ssh/id_rsa.pub ]; then
    echo
    echo ------------------------------------------
    echo ssh-keygen -t rsa -b 2048 -f ~/.ssh/id_rsa
    echo ------------------------------------------
    ssh-keygen -t rsa -b 2048 -f ~/.ssh/id_rsa
    RC=$?
    if [ $RC -ne 0 ]; then exit $RC; fi
    echo
    echo ----------------------------------
    echo cp -p ~/.ssh/id_rsa.pub context.d/
    echo ----------------------------------
    cp -p ~/.ssh/id_rsa.pub context.d/
    RC=$?
    if [ $RC -ne 0 ]; then exit $RC; fi
  else
    echo
    echo --------------------------------------
    echo ls -og ~/.ssh/id_rsa ~/.ssh/id_rsa.pub
    echo --------------------------------------
    ls -og ~/.ssh/id_rsa ~/.ssh/id_rsa.pub
    exit $RC
  fi
fi

echo
echo ----------------
echo ls -og context.d
echo ----------------
ls -og context.d
echo
echo ------------------------
echo cat context.d/context.sh
echo ------------------------
cat context.d/context.sh
echo
echo -n 'Type Enter '; read VOID

XW_CONTEXT_VDI=xwcontext_disk_$USER.vdi
rm -f $XW_CONTEXT_VDI
if [ '(' -f xwcontext ')' -a  '(' -r xwcontext ')' -a '(' -x xwcontext ')' ]; then
  XW_CONTEXT_SCRIPT=./xwcontext
else
  XW_CONTEXT_SCRIPT=$XW_BIN/xwcontext
fi
echo -------------------------------------
echo $XW_CONTEXT_SCRIPT --source context.d
echo -------------------------------------
$XW_CONTEXT_SCRIPT --source context.d
RC=$?
if [ $RC -ne 0 ]; then exit $RC; fi
echo
echo -n 'Type Enter '; read VOID

echo --------------------------
echo xwsenddata $XW_CONTEXT_VDI
echo --------------------------
XW_CONTEXT_DISK_URI=$($XW_BIN/xwsenddata $XW_CONTEXT_VDI)
RC=$?
if [ $RC -ne 0 ]; then exit $RC; fi
XW_CONTEXT_DISK_URI=$(echo $XW_CONTEXT_DISK_URI  |  sed -e 's/^.* WARN .* xw:/xw:/')
echo XW_CONTEXT_DISK_URI=$XW_CONTEXT_DISK_URI
echo
echo -n 'Type Enter '; read VOID

echo '-------------------------------'
echo 'xwsubmit $APPNAME --sshport $SSHPORT \
   --xwlistenport $SSHPORT \
   --context $XW_CONTEXT_VDI \
   --xwenv $XW_CONTEXT_DISK_URI \
   --scratch aahd2.vdi \
   --xwenv $XW_SUPERNEMO_APP_URI'
echo '-------------------------------'
XW_SUPERNEMO_VM=$($XW_BIN/xwsubmit $APPNAME --sshport $SSHPORT \
   --xwlistenport $SSHPORT \
   --context $XW_CONTEXT_VDI \
   --xwenv $XW_CONTEXT_DISK_URI \
   --scratch aahd2.vdi \
   --xwenv $XW_SUPERNEMO_APP_URI)
RC=$?
if [ $RC -ne 0 ]; then exit $RC; fi
echo XW_SUPERNEMO_VM=$XW_SUPERNEMO_VM
echo

echo '------------------------'
echo 'xwworks $XW_SUPERNEMO_VM'
echo '------------------------'
XW_VM_STATUS=
while [ "$XW_VM_STATUS" != "RUNNING" ]; do
  sleep 2
  XW_VM_STATUS=$($XW_BIN/xwworks $XW_SUPERNEMO_VM  |  \
                 sed -e "s/^.* STATUS='//; s/'.*//")
  echo XW_VM_STATUS=$XW_VM_STATUS
done
echo
echo -n 'Type Enter '; read VOID

echo '----------------------------------------------'
echo 'xwproxy $XW_SUPERNEMO_VM --xwlistenport 4444 &'
echo '----------------------------------------------'
while true; do
  $XW_BIN/xwproxy $XW_SUPERNEMO_VM --xwlistenport 4444 &
  sleep 5
  if ps -p $! > /dev/null 2>&1; then break; fi
  echo '#  VM was NOT listening yet.  Retrying ...'
done
echo '----------------------'
echo '# VM is listening now.'
echo '----------------------'
echo
echo -n 'Type Enter '; read VOID

echo ----------------------------
echo To connect to your VM, you must open a new terminal and type:
echo '$> ssh -p 4444 xwuser@localhost'
echo ----------------------------
#ssh -p 4444 xwuser@localhost
echo
echo -n 'Type Enter (This will stop your VM) '; read VOID

echo '------------------------------------------'
echo 'xwrm $XW_CONTEXT_DISK_URI $XW_SUPERNEMO_VM'
echo '------------------------------------------'
$XW_BIN/xwrm $XW_CONTEXT_DISK_URI $XW_SUPERNEMO_VM
XW_SUPERNEMO_VM=$(echo $XW_SUPERNEMO_VM  |  sed -e 's=^.*/==')

/bin/rm $XW_CONTEXT_VDI $XW_SUPERNEMO_VM.txt
