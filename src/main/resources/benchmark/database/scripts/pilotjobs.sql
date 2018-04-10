--  Copyrights     : CNRS
--  Author         : Oleg Lodygensky
--  Acknowledgment : XtremWeb-HEP is based on XtremWeb 1.8.0 by inria : http://www.xtremweb.net/
--  Web            : http://www.xtremweb-hep.org
-- 
--       This file is part of XtremWeb-HEP.
-- 
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--     http://www.apache.org/licenses/LICENSE-2.0
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-- 
--
-- Since : 5.8.0
--
-- File    : pilotjobs.sql
-- Purpose : this file contains the needed SQL commands to 
--           retreive pilot jobs from DB
-- Usage   : mysql < whathappened.sql
--

select lastalive,count(*)
       from (
         (select date_format(lastalive,"%Y/%m/%d %H:%i") as lastalive from hosts 
         where not isnull(lastalive)
	        and   pilotjob="true")
	     union all
         (select date_format(lastalive,"%Y/%m/%d %H:%i") as lastalive from hosts_history
         where not isnull(lastalive)
	        and   pilotjob="true")
 	  ) as t 
 	  group by lastalive order by lastalive;
