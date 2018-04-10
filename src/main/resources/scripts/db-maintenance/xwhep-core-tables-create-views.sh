#!/bin/sh -e
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
#  XtremWeb-HEP 9.x :  DB maintenance  -  Core tables
#
#  Procedure to create views for :
#  - Tables,
#  - Tables 'sessions' and 'groups' with work counts,
#  - Pure relationships,
#  - Provisioning and billing.
#
#  This uses SQL scripts which should be present in the folder containing this
#  procedure.
#
#  Parameters :  1: Database host name
#                2: Database user name
#                3: Schema containing tables, and where views will be created
#                4: Optional parameter requesting an interactive DB password
#
#=============================================================================
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
#  First, dereference symbolic links to obtain the 'real' file folder
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
  
  if  [ "$2" ];  then
    echo  > /dev/stderr
    while  [ "$2" ];  do
      echo  "$2"  > /dev/stderr
      shift
    done
  fi
  
	mysql  -h "$DB_HOST"  -u "$DB_USER"  $NEEDS_DB_PASSWORD  "$DB_SCHEMA"  \
      --batch  --execute "source $SQLSCRIPT"
}


#-----------------------------------------------------------------------------
#  Execute SQL scripts
#-----------------------------------------------------------------------------
source_sql  xwhep-core-tables-query-for-create-views.sql  \
    'Create the SQL script for the creation in the current schema of views for tables'  |  \
    perl  -0777  -wpe  's=,\s*null$==gm'  >  xwhep-core-tables-create-views-for-tables.sql

source_sql  xwhep-core-tables-create-views-for-tables.sql  \
    'Using the SQL script just created above, create views for tables'

source_sql  xwhep-core-tables-query-for-create-users-views.sql  \
    'Create the SQL script for the creation in the current schema of users views'  |  \
    perl  -0777  -wpe  's=,\s*null$==gm'  >  xwhep-core-tables-create-users-views.sql

source_sql  xwhep-core-tables-create-users-views.sql  \
    'Using the SQL script just created above, create users views'

source_sql  xwhep-core-tables-create-views-for-sessions-and-groups.sql  \
    'Creation of views for tables "sessions" and "groups" with work counts'

source_sql  xwhep-core-tables-create-views-for-relationships.sql  \
    'Creation of views for pure relationships :  executables,  sharedAppTypes'

source_sql  xwhep-core-tables-create-view-hosts-matching-works.sql  \
    'Creation of a view for matchmaking between hosts and works'

source_sql  xwhep-core-tables-create-views-for-offering-and-billing.sql  \
    'Creation of views for offering and billing'
