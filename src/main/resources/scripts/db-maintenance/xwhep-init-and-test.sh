#!/bin/sh
#=============================================================================
#
#  Copyright 2014  E. URBAH
#                  at LAL, Univ Paris-Sud, IN2P3/CNRS, Orsay, France
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
#  Init and test XtremWeb-HEP
#
#  Prerequisite :  Current folder must contain an 'xwconfigure.values' file
#
#=============================================================================
XWHEP_VERSION="${1:-9.x}"
echo
echo "XWHEP_VERSION='$XWHEP_VERSION'"

#-----------------------------------------------------------------------------
#  Constants which can easily be customized
#-----------------------------------------------------------------------------
XW_BIN=bin
XWHEP_APP=apptest
XWHEP_CONFIG_FILE=xwconfigure.values
XWHEP_SERVER_CONF=xtremweb.server.conf
XWHEP_SOURCE_REPO=https://svn.lal.in2p3.fr/projects/XWHEP/tags

#-----------------------------------------------------------------------------
#  Constants depending of the Operating System
#-----------------------------------------------------------------------------
case  "$(uname -s)"  in
  Darwin*)
    MYSQL_START_SCRIPT=/sw/bin/mysqld_safe
    ;;
  
  Linux*)
    MYSQL_START_SCRIPT=/etc/init.d/mysql
    
    if  [ -z "$JAVA_HOME" ];  then
      export JAVA_HOME=/usr
    fi
    
    if  type apt-get > /dev/null 2>&1;  then
      echo
      echo  "----------------------------------------------------------------"
      echo  "Under Debian based Linux, install missing packages"
      echo  "----------------------------------------------------------------"
      
      PACKAGES_TO_INSTALL=
      
      if  [ ! -r "$MYSQL_START_SCRIPT" ];  then
        PACKAGES_TO_INSTALL="$PACKAGES_TO_INSTALL  mysql-server"
      fi
      
      if  [ ! -r "$JAVA_HOME/bin/keytool" ];  then
        PACKAGES_TO_INSTALL="$PACKAGES_TO_INSTALL  default-jdk"
      fi
      
      if  ! type svn > /dev/null 2>&1;  then
        PACKAGES_TO_INSTALL="$PACKAGES_TO_INSTALL  subversion"
      fi
      
      if  ! type uuidgen > /dev/null 2>&1;  then
        PACKAGES_TO_INSTALL="$PACKAGES_TO_INSTALL  uuid-runtime"
      fi
      
      if  [ "$PACKAGES_TO_INSTALL" ];  then
        ( set -ex;  sudo  apt-get  install  $PACKAGES_TO_INSTALL )
      fi
    fi
    ;;
  
  *)
    echo "Operating system '$OSTYPE' NOT recognized"  > /dev/stderr
    exit 2
    ;;
esac

echo
echo  "----------------------------------------------------------------------"
echo  "If XtremWeb-HEP processes are running, stop them"
echo  "----------------------------------------------------------------------"
XWHEP_PIDS=$(ps  -e  -o pid,command                            |  \
             grep  '^ *[0-9]* *[^ ]*java .*/lib/xtremweb.jar'  |  \
             sed  -e 's=^ *\([0-9]*\).*$=\1=')
[ -z "$XWHEP_PIDS" ]  ||  ( set -x;  kill $XWHEP_PIDS )

echo
echo  "----------------------------------------------------------------------"
echo  "Import environment variables from $XWHEP_CONFIG_FILE"
echo  "----------------------------------------------------------------------"
if  [ ! -r "$XWHEP_CONFIG_FILE" ];  then
  echo  "Configuration file '$XWHEP_CONFIG_FILE' NOT found"  > /dev/stderr
  exit 3
fi

( set -x;  grep  '^ *[A-Za-z_][0-9A-Za-z_]*='  "$XWHEP_CONFIG_FILE"  \
               > "$XWHEP_CONFIG_FILE.sh" )

.  "./$XWHEP_CONFIG_FILE.sh"

echo
echo  "----------------------------------------------------------------------"
echo  "MySQL: Try to drop the $DBNAME schema."
echo  "       If it fails, start MySQL and retry to drop the $DBNAME schema."
echo  "----------------------------------------------------------------------"
( set -x
  mysql  -h "$DBHOST"  -u "$DBADMINLOGIN"  ${DBADMINPASSWORD:+-p}  \
      --execute  "drop  schema  if exists  $DBNAME"  2> /dev/null )

if  [ $? -ne 0 ];  then
  sudo  -v  -p "Starting MySQL as %U:  %p's password: "
  
  ( set -x;  sudo  "$MYSQL_START_SCRIPT"  & )
  
  sleep  1
  
  ( set -x
    mysql  -h "$DBHOST"  -u "$DBADMINLOGIN"  ${DBADMINPASSWORD:+-p}  \
        --execute  "drop  schema  if exists  $DBNAME"  2> /dev/null )
fi

if  [ $? -ne 0 ];  then
  echo  'MySQL can NOT be started'  > /dev/stderr
  exit 4
fi

set -e

echo
echo  "----------------------------------------------------------------------"
echo  "From SVN, extract the XtremWeb-HEP sources"
echo  "Compile and build XtremWeb-HEP"
echo  "----------------------------------------------------------------------"
(
  set -x
  
  svn  checkout  "$XWHEP_SOURCE_REPO/$XWHEP_VERSION"
  
  cd                                "$XWHEP_VERSION/build"
  
  make  install
)

echo
echo  "----------------------------------------------------------------------"
echo  "-  Go to the $XWHEP_VERSION/build/dist/xwhep-*  folder"
echo  "-  Insert XWHEP_CONFIG_FILE in the 'conf' subfolder,"
echo  "   then execute the 'xwconfigure' script using $XWHEP_CONFIG_FILE."
echo  "----------------------------------------------------------------------"
cd  "$XWHEP_VERSION/build/dist/xwhep-"*

(
  set -x
  
  /bin/cp  -p  "../../../../$XWHEP_CONFIG_FILE"  conf/
  
  "$XW_BIN/xwconfigure"  --yes  --nopkg
)

mysql  -h "$DBHOST"  -u "$DBADMINLOGIN"  ${DBADMINPASSWORD:+-p}  "$DBNAME"  \
    --execute  'select uid, login, rights, userRightId from users'

echo
echo  "----------------------------------------------------------------------"
echo  "Patch the XtremWeb-HEP server config file to inhibit 'launcherurl'"
echo  "----------------------------------------------------------------------"
( set -x
  perl  -wpi  -e  's|^(\s*launcherurl=)|#$1|'  "conf/$XWHEP_SERVER_CONF"
)

echo
echo  "----------------------------------------------------------------------"
echo  "Start the XtremWeb-HEP server"
echo  "----------------------------------------------------------------------"
( set -x
  bin/xtremweb.server  console  --xwconfig conf/xtremweb.server.conf  \
      -Dhomedir=/tmp  >> xtremweb.server.log  2>&1  & )

sleep 5

echo
echo  "----------------------------------------------------------------------"
echo  "Start the XtremWeb-HEP worker"
echo  "----------------------------------------------------------------------"
( set -x
  bin/xtremweb.worker  console  --xwconfig conf/xtremweb.worker.conf  \
                      >> xtremweb.worker.log  2>&1  & )



#-----------------------------------------------------------------------------
#  Function  xw_list_objects  (list of XtremWeb-HEP objects)
#-----------------------------------------------------------------------------
xw_list_objects ()
{
  for  XWHEP_OBJECTS  in  "$@"; do
    echo; echo;
    echo  "------------------------------------------------------------------"
    echo "Using the XtremWeb-HEP client, list the XtremWeb-HEP $XWHEP_OBJECTS"
    echo  "------------------------------------------------------------------"
    (
      set +e
      XW_MESSAGE=$( set -x;  "$XW_BIN/xw$XWHEP_OBJECTS"  --xwformat xml )
      RC=$?
      echo  "$XW_MESSAGE"  |  perl  -wpe 's="\s+="\n  =g'
      [ $RC -eq 0 ]  ||  exit $RC
    )
  done
}

#-----------------------------------------------------------------------------
#  Using the XtremWeb-HEP client, list XtremWeb-HEP users
#-----------------------------------------------------------------------------
xw_list_objects  users

echo
echo  "----------------------------------------------------------------------"
echo  "Using the XtremWeb-HEP client, insert the '$XWHEP_APP' application"
echo  "----------------------------------------------------------------------"
case  "$(uname -s)"  in
  Darwin*)
    ( set -x;  "$XW_BIN/xwsendapp"  $XWHEP_APP  DEPLOYABLE  x86_64  MACOSX  \
                                    $XWHEP_APP/$XWHEP_APP-macosx-x86_64 )
    ;;
  
  Linux*)
    (
      set -x
      "$XW_BIN/xwsendapp"  $XWHEP_APP  DEPLOYABLE     x86_64  LINUX  \
                           $XWHEP_APP/$XWHEP_APP-linux-amd64
      "$XW_BIN/xwsendapp"  $XWHEP_APP  DEPLOYABLE      amd64  LINUX  \
                           $XWHEP_APP/$XWHEP_APP-linux-amd64
    )
    ;;
  
  *)
    echo "Operating system '$OSTYPE' NOT recognized"  > /dev/stderr
    exit 5
    ;;
esac

set +e
XW_MESSAGE=$( set -x;  "$XW_BIN/xwapps"  --xwformat xml )
RC=$?
echo  "$XW_MESSAGE"  |  perl  -wpe 's="\s+="\n  =g'
[ $RC -eq 0 ]  ||  exit $RC


echo
echo  "----------------------------------------------------------------------"
echo  "Using the XtremWeb-HEP client, submit an '$XWHEP_APP' work"
echo  "----------------------------------------------------------------------"
XW_MESSAGE=$( set -x;  "$XW_BIN/xwsubmit"  $XWHEP_APP )  ||  \
  { RC=$?;  echo "$XW_MESSAGE"  > /dev/stderr;  exit $RC; }

XW_WORK_URI=$(echo  "$XW_MESSAGE"  |  grep  '^xw://')

#-----------------------------------------------------------------------------
#  Work detailed status
#-----------------------------------------------------------------------------
echo
"$XW_BIN/xwstatus"  "$XW_WORK_URI"  --xwformat xml  |  \
  perl  -wpe  's/"\s+/"\n  /g'

echo
#-----------------------------------------------------------------------------
echo "Looping on work status.   You can interrupt the loop with (ctrl)C if"  \
     "you wish so."
#-----------------------------------------------------------------------------
XW_STATUS_OLD=''
while true; do
  sleep 5
  XW_MESSAGE=$("$XW_BIN/xwresults"  "$XW_WORK_URI")
  XW_STATUS_NEW=$(echo  "$XW_MESSAGE"  |  \
              perl  -wpe  "s/^.*STATUS='([^']*)'.*\$/\$1/")
  if  expr "$XW_MESSAGE" : '.*ERROR.*:' > /dev/null  2>&1 || \
      [ "$XW_STATUS_NEW"  = "ERROR"     ]                 || \
      [ "$XW_STATUS_NEW"  = "COMPLETED" ];  then break;  fi
  if  [ "$XW_STATUS_NEW" != "$XW_STATUS_OLD" ];  then
    /bin/echo
    /bin/echo  -n  "$XW_MESSAGE "  |  perl  -wpe  's/, *[A-Za-z_]*=NULL//g'
    XW_STATUS_OLD="$XW_STATUS_NEW"
  else
    /bin/echo  -n  .
  fi
done

echo
echo  "$XW_MESSAGE"

#-----------------------------------------------------------------------------
#  Using the XtremWeb-HEP client, display the results of the work
#-----------------------------------------------------------------------------
set -e
XW_WORK_UID=$(echo  "$XW_WORK_URI"  |  sed  -e  's=^.*/==')
echo
unzip  -l  *"_ResultsOf_$XW_WORK_UID.zip"
echo
unzip  -c  *"_ResultsOf_$XW_WORK_UID.zip"  stderr.txt
echo
unzip  -c  *"_ResultsOf_$XW_WORK_UID.zip"  TestResults.txt

#-----------------------------------------------------------------------------
#  Using the XtremWeb-HEP client, list XtremWeb-HEP hosts
#-----------------------------------------------------------------------------
xw_list_objects  workers
