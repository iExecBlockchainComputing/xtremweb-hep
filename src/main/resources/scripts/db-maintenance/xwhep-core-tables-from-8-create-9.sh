#!/bin/sh -e
#=============================================================================
#
#  Copyright 2013  E. URBAH
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
#  XtremWeb-HEP 9.x :  DB maintenance  -  Core tables
#
#  Procedure to copy the contents of the "$1":"$3" schema for version 8.x
#  as user "$2" into the "$1":"$4" schema for version 9.x
#
#  This uses SQL scripts which should be present in the folder containing this
#  procedure.
#
#  Parameters :  1: Database host name
#                2: Database user name
#                3: Source schema holding 8.x tables
#                4: Existing schema to hold the 9.x tables
#                5: Optional parameter requesting an interactive DB password
#
#=============================================================================
set -e

if  [ $# -lt 4 ]  ||  [ $# -gt 5 ];  then
  echo "Usage :  $0  DB_HOST  DB_USER  SOURCE_SCHEMA  TARGET_SCHEMA "  \
                    "[NEEDS_DB_PASSWORD]"  > /dev/stderr
  exit 1
fi

DB_HOST="$1"                                                       #  xwdb
DB_USER="$2"                                                       #  xtremweb
SOURCE_SCHEMA="$3"                                                 #  xwservpub
TARGET_SCHEMA="$4"                                                 #  test
NEEDS_DB_PASSWORD="${5+-p}"                                        #  YES


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

	mysql  -h "$DB_HOST"  -u "$DB_USER"  $NEEDS_DB_PASSWORD  "$TARGET_SCHEMA"  \
      --batch  --execute  \
          "set @SOURCE_SCHEMA='$SOURCE_SCHEMA';  source $SQLSCRIPT;"
}


#-----------------------------------------------------------------------------
#  Execute SQL scripts
#-----------------------------------------------------------------------------
source_sql  xwhep-core-tables-drop-triggers-and-tables.sql  \
    'Drop all triggers and tables which have to be created'

source_sql  xwhep-core-tables-from-8-create-initial-tables.sql  \
    'Create all tables for version 8.x'

source_sql  xwhep-core-tables-from-8-create-new-tables-columns-fk.sql  \
    'Create new tables, columns and foreign keys'

source_sql  xwhep-core-tables-set-not-null-fk-referencing-new-tables.sql  \
    'Set NOT NULL the foreign keys referencing the new tables'

source_sql  xwhep-core-tables-create-triggers-for-data-coming-from-8.sql  \
    'Create triggers for data copied from version 8.x'

source_sql  xwhep-core-tables-create-triggers-for-history-coming-from-8.sql  \
    'Create triggers for history copied from version 8.x'

source_sql  xwhep-core-tables-query-for-copy-contents-from-source-schema.sql      \
    "Create the SQL script permitting to copy the contents of the"                \
    "'$SOURCE_SCHEMA' schema '(with history)' into the '$TARGET_SCHEMA' schema"   \
    > xwhep-core-tables-copy-contents-from-source-schema.sql

source_sql  xwhep-core-tables-copy-contents-from-source-schema.sql                \
    "Using the SQL script just created above, copy the contents of the"           \
    "'$SOURCE_SCHEMA' schema '(with history)' into the '$TARGET_SCHEMA' schema."  \
    "This lasts several hours."
