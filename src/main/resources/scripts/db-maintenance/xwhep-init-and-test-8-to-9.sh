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
#  Init and test XtremWeb-HEP version 8.x with a database migrated to 9.x
#
#  Prerequisite :  Current folder must contain an 'xwconfigure.values' file
#
#  Parameter :     Version of XtremWeb-HEP to be tested (8.x.y)
#
#=============================================================================
if  [ $# -ne 1 ];  then
  echo  "Usage :  $0  XWHEP_VERSION"  > /dev/stderr
  exit 1
fi

XWHEP_VERSION="$1"

#-----------------------------------------------------------------------------
#  Constants which can easily be customized
#-----------------------------------------------------------------------------
XWHEP_OLD_VERSION=8.3.2
XW_BIN=bin
XWHEP_APP=apptest
XWHEP_CONFIG_FILE=xwconfigure.values
XWHEP_SERVER_CONF=xtremweb.server.conf
XWHEP_BINARY_REPO=http://www.xtremweb-hep.org/lal/
XWHEP_MIGRATION_REPO=https://svn.lal.in2p3.fr/projects/XWHEP/tags/9.x/src/scripts/db-maintenance

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
echo  "If necessary, download Xtremweb-HEP binary"
echo  "Extract Xtremweb-HEP binary in the current folder,"
echo  "then go to the "XWHEP-$XWHEP_VERSION" folder."
echo  "----------------------------------------------------------------------"
(
  set -x
  
  /bin/rm  -rf   "xwhep-$XWHEP_VERSION"
  
  if  [ ! -r "XWHEP-$XWHEP_VERSION-bin.tar.gz" ];  then
    if  type wget > /dev/null 2>&1;  then
      wget  -q      "$XWHEP_BINARY_REPO/XWHEP-$XWHEP_VERSION-bin.tar.gz"
    else
      curl  -s  -O  "$XWHEP_BINARY_REPO/XWHEP-$XWHEP_VERSION-bin.tar.gz"
    fi
  fi
  
  tar  -xzf      "XWHEP-$XWHEP_VERSION-bin.tar.gz"
)

cd               "xwhep-$XWHEP_VERSION"

set +e

if  [ "$XWHEP_VERSION" = "$XWHEP_OLD_VERSION" ];  then
  echo
  echo  "--------------------------------------------------------------------"
  echo  "Here the current XtremWeb-HEP version is '8.3.2' :"
  echo  "Replace 'lib/xtremweb.jar' by the latest XtremWeb-HEP jar."
  echo  "--------------------------------------------------------------------"
  XWHEP_NEW_JAR="$(ls  -1t  ../xwhep-*.jar  |  head  -1)"
  (
    set -ex
    
    if  [ "$XWHEP_NEW_JAR" ]  &&  [ -r "$XWHEP_NEW_JAR" ];  then
      rm                        lib/xtremweb.jar
      cp  -p  "$XWHEP_NEW_JAR"  lib/xtremweb.jar
    fi
  )  
fi

echo
echo  "----------------------------------------------------------------------"
echo  "-  Insert XWHEP_CONFIG_FILE in the 'conf' subfolder,"
echo  "   then execute the 'xwconfigure' script using $XWHEP_CONFIG_FILE."
echo  "----------------------------------------------------------------------"
(
  set -x
  
  /bin/cp  -p  "../$XWHEP_CONFIG_FILE"  "conf/"  ||  exit $?
  
  "$XW_BIN/xwconfigure"  --yes  --nopkg
)

if  [ $? -ne 0 ];  then
  echo
  echo  "--------------------------------------------------------------------"
  echo  "First try of 'xwconfigure' has failed."
  echo  "So, execute 'xwsetversion.sql' and retry 'xwconfigure'."
  echo  "--------------------------------------------------------------------"
  (
    set -ex
    
    mysql  -h "$DBHOST"  -u "$DBADMINLOGIN"  ${DBADMINPASSWORD:+-p}  \
        "$DBNAME"  --execute  "source  $XW_BIN/xwsetversion.sql"
    
    "$XW_BIN/xwconfigure"  --yes  --nopkg
  )
fi

set -e

mysql  -h "$DBHOST"  -u "$DBADMINLOGIN"  ${DBADMINPASSWORD:+-p}  "$DBNAME"  \
    --execute  'select uid, login, rights from users'

echo
echo  "----------------------------------------------------------------------"
echo  "Patch the XtremWeb-HEP server config file to inhibit 'launcherurl'"
echo  "----------------------------------------------------------------------"
( set -x
  perl  -wpi  -e  's|^(\s*launcherurl=)|#$1|'  "conf/$XWHEP_SERVER_CONF"
)

echo
echo  "----------------------------------------------------------------------"
echo  "Migration of the database from 8.x to 9.x :"
echo  "-  From SVN, extract the required scripts in the current folder,"
echo  "-  Execute the main migration script,"
echo  "-  Execute the script creating useful views,"
echo  "-  Execute a simple query on 'view_users'."
echo  "----------------------------------------------------------------------"
(
  set -x
  
  svn  checkout  "$XWHEP_MIGRATION_REPO"  .
  
  sh  xwhep-core-tables-from-8-migrate-to-9.sh  "$DBHOST"  \
      "$DBADMINLOGIN"  ${DBADMINPASSWORD:+-p}   "$DBNAME"
  
  sh  xwhep-core-tables-create-views.sh         "$DBHOST"  \
      "$DBADMINLOGIN"  ${DBADMINPASSWORD:+-p}   "$DBNAME"
  
  mysql  -h "$DBHOST"  -u "$DBADMINLOGIN"  ${DBADMINPASSWORD:+-p}  "$DBNAME"  --execute  \
      'select uid, login, userRightName, usergroup, owner from view_users order by login'
)

if  [ "$XWHEP_VERSION" = "$XWHEP_OLD_VERSION" ]  &&  \
    [ -z "$XWHEP_NEW_JAR" ];  then
  echo
  echo  "--------------------------------------------------------------------"
  echo  "Here the current XtremWeb-HEP version is '8.3.2', and we could NOT"
  echo  "replace 'lib/xtremweb.jar' by the latest XtremWeb-HEP jar."
  echo  "Patch the database for accepting that following columns are null :"
  echo  "-  Table 'works' :  'ownerUID', 'status' and 'statusId',"
  echo  "-  Table 'datas' :              'status' and 'statusId'."
  echo  "--------------------------------------------------------------------"
  (
    set -x
    
    mysql  -h "$DBHOST"  -u "$DBADMINLOGIN"  ${DBADMINPASSWORD:+-p}  "$DBNAME"  --execute  \
        'alter table works modify column ownerUID char(50) null'
    
    mysql  -h "$DBHOST"  -u "$DBADMINLOGIN"  ${DBADMINPASSWORD:+-p}  "$DBNAME"  --execute  \
        'alter table works modify column status   char(20) null default "NONE"'
    
    mysql  -h "$DBHOST"  -u "$DBADMINLOGIN"  ${DBADMINPASSWORD:+-p}  "$DBNAME"  --execute  \
        'alter table works modify column statusId tinyint unsigned null default "0"'
    
    mysql  -h "$DBHOST"  -u "$DBADMINLOGIN"  ${DBADMINPASSWORD:+-p}  "$DBNAME"  --execute  \
        'alter table datas modify column status   char(20) null default "NONE"'
    
    mysql  -h "$DBHOST"  -u "$DBADMINLOGIN"  ${DBADMINPASSWORD:+-p}  "$DBNAME"  --execute  \
        'alter table datas modify column statusId tinyint unsigned null default "0"'
  )
fi

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
