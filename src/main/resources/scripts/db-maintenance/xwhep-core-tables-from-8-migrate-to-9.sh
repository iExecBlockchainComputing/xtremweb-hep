#!/bin/sh -e
#=============================================================================
#
#  Copyright 2013  E. URBAH
#                  at LAL, Univ Paris-Sud, IN2P3/CNRS, Orsay, France
#  Corrected by :  Oleg Lodygensky
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
#  XtremWeb-HEP 9.x :  DB maintenance  -  Core tables
#
#  Procedure to migrate as user "$2" the "$1":"$3" schema from version 8.x to
#  version 9.x
#
#  This uses SQL scripts which should be present in the folder containing this
#  procedure.
#
#  Parameters :  1: Database host name
#                2: Database user name
#                3: Schema to migrate
#                4: Optional parameter requesting an interactive DB password
#
#=============================================================================
set -e

if [ $# -lt 3 ]  ||  [ $# -gt 4 ]; then
  echo  "Usage :  $0  DB_HOST  DB_USER  DB_SCHEMA  [NEEDS_DB_PASSWORD]"  \
      > /dev/stderr
  exit 1
fi

DB_HOST="$1"                                                       #  xwdb
DB_USER="$2"                                                       #  xtremweb
DB_SCHEMA="$3"                                                     #  test
NEEDS_DB_PASSWORD="${4+-p}"                                        #  YES


#-----------------------------------------------------------------------------
#  First, dereference symbolic links to obtain the 'real' source folder
#-----------------------------------------------------------------------------
SCRIPT="$0"

while  [ -h "$SCRIPT" ];  do
  LINK="$(readlink  "$SCRIPT")"
  if  expr  "$LINK"  :  /  > /dev/null  2>&1;  then
    SCRIPT="$LINK"
  else
    SCRIPT="$(dirname "$SCRIPT")/$LINK"
  fi
done

cd  "$(dirname "$SCRIPT")"


#-----------------------------------------------------------------------------
#  Function  source_sql 
#  Parameters :  1    :  Filename of the SQL script to execute
#                2... :  Header to display
#-----------------------------------------------------------------------------
source_sql ()
{
	SQLSCRIPT="$1"
  
	if  [ -z "$SQLSCRIPT" ];  then
		echo  "$0: Filename of the SQL script is empty"  > /dev/stderr
		return
	fi 
  
	if  [ ! -r "$SQLSCRIPT" ];  then
		echo  "$0: '$SQLSCRIPT' NOT found"  > /dev/stderr
		exit 1 
	fi
  
  if  [ "$2" ]; then
    echo  > /dev/stderr
    while  [ "$2" ];  do
      echo  "$2"  > /dev/stderr
      shift
    done
  fi
  
	mysql  -h "$DB_HOST"  -u "$DB_USER"  $NEEDS_DB_PASSWORD  "$DB_SCHEMA"  \
      --batch  --execute "source $SQLSCRIPT;"
}


#-----------------------------------------------------------------------------
#  Execute SQL scripts
#-----------------------------------------------------------------------------
if  mysql  -h "$DB_HOST"  -u "$DB_USER"  $NEEDS_DB_PASSWORD  "$DB_SCHEMA"  \
        --batch  --execute 'select uid as "" from users'  |  \
      grep '[A-Z]'  > /dev/null  2>&1;  then 
  source_sql  xwhep-core-tables-set-users-uid-lowercase.sql  \
      'Set lowercase "users.uid" and "*.ownerUID"'
fi

source_sql  xwhep-core-tables-from-8-move-to-history-objects-having-bad-references.sql  \
    'Move to history objects from 8.x having bad references'

source_sql  xwhep-core-tables-from-8-create-new-tables-columns-fk.sql  \
    'Create new tables, columns and foreign keys'

source_sql  xwhep-core-tables-create-triggers-for-data-coming-from-8.sql  \
    'Create triggers for data copied from version 8.x'

source_sql  xwhep-core-tables-create-triggers-for-history-coming-from-8.sql  \
    'Create triggers for history copied from version 8.x'

source_sql  xwhep-core-tables-populate-new-columns-for-data-copied-from-8.sql  \
    'Populate new columns for data copied from version 8.x'

source_sql  xwhep-core-tables-populate-new-columns-for-history-copied-from-8.sql  \
    'Populate new columns for history copied from version 8.x'

source_sql  xwhep-core-tables-set-not-null-fk-referencing-new-tables.sql  \
    'Set NOT NULL the foreign keys referencing the new tables'
