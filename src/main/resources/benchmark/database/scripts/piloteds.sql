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
-- File    : piloteds.sql
-- Purpose : this file contains the needed SQL commands to 
--           retreive work run by a pilot jobs from DB
-- Usage   : mysql < whathappened.sql
--


select completeddate,count(*)
       from (
         (select date_format(completeddate,"%Y/%m/%d %H:%i") as completeddate from works , tasks, hosts 
         where tasks.workuid=works.uid 
       	    and date(completeddate)>"2009-07-14" 
 	 		and not isnull(completeddate)
 	 		and works.status="COMPLETED"
		 	and tasks.hostuid=hosts.uid 
	        and hosts.pilotjob="true")
	     union all
         (select date_format(completeddate,"%Y/%m/%d %H:%i") as completeddate from works_history , tasks_history, hosts 
         where tasks_history.workuid=works_history.uid 
       	    and date(completeddate)>"2009-07-14" 
 	 		and not isnull(completeddate)
 	 		and works_history.status="COMPLETED"
		 	and tasks_history.hostuid=hosts.uid 
	        and hosts.pilotjob="true")
 	  ) as t 
 	  group by completeddate order by completeddate;
