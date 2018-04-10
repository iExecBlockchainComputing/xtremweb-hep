# Copyrights     : CNRS
# Author         : Oleg Lodygensky
# Acknowledgment : XtremWeb-HEP is based on XtremWeb 1.8.0 by inria : http://www.xtremweb.net/
# Web            : http://www.xtremweb-hep.org
#
#      This file is part of XtremWeb-HEP.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#     http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

File   : benchmark/database/REAME.txt
Date   : 28 juin 2011
Author : Oleg Lodygensky


This directory contains necessary make files and tools to extract statistics from XtremWeb databases
and generate graphs.

To do so:
-1- copy a dump of the database to databases/
-2- create the database just like
   $> mysql -e "create database [your database name]"
   $> mysql [your database name] < databases/[yourdatabasename.sql]
-3- set some environment variables
  - DBHOST
  - DBUSER
  - DBPASSWD

-4- run the make tool 
     $> make

The extracted statistics are then in csv/ and generated graphs in fig/

