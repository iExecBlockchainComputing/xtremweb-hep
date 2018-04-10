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
--
-- Since : 7.4.0
--
-- File    : timings.sql
-- Purpose : this file contains the needed SQL commands to 
--           retreive timings (pending delay, running delay etc.)
-- Usage   : mysql < timings.sql
--

select completeddate,
       laststartdate,
       startdate,
       insertiondate,
       arrivaldate,
       run,
       restart,
       pending,
       waiting
       from (
         (select date_format(completeddate,"%Y/%m/%d %H:%i") as completeddate,
		       laststartdate,
		       startdate,
		       insertiondate,
		       arrivaldate,
		       unix_timestamp(completeddate)-unix_timestamp(laststartdate) as run,
		       unix_timestamp(laststartdate)-unix_timestamp(startdate) as restart,
		       unix_timestamp(startdate)-unix_timestamp(insertiondate) as pending,
		       unix_timestamp(insertiondate)-unix_timestamp(arrivaldate) as waiting
            from works, tasks
            where works.uid=tasks.workuid
	       	  and works.status="COMPLETED" 
 	 		  and not isnull(completeddate))
	     union all
         (select date_format(completeddate,"%Y/%m/%d %H:%i") as completeddate,
		       laststartdate,
		       startdate,
		       insertiondate,
		       arrivaldate,
		       unix_timestamp(completeddate)-unix_timestamp(laststartdate) as run,
		       unix_timestamp(laststartdate)-unix_timestamp(startdate) as restart,
		       unix_timestamp(startdate)-unix_timestamp(insertiondate) as pending,
		       unix_timestamp(insertiondate)-unix_timestamp(arrivaldate) as waiting
            from works_history, tasks_history
            where works_history.uid=tasks_history.workuid
	       	  and works_history.status="COMPLETED" 
 	 		  and not isnull(completeddate))
 	  ) as t 
 	  group by completeddate order by completeddate;
