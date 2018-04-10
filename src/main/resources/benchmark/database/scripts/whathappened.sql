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
-- Since : 13.0.0
--
-- Purpose : this file contains the needed SQL commands to
--           jobs from DB
-- Usage   : mysql < whathappened3.sql
--


-- select "create a temporary table for completed jobs"

create temporary table completedjobs (
       thedate datetime,
       completeds int(10) default 0,
       retries int(10) default 0
);

-- select "fill temporary table completedjobs";

insert into completedjobs (thedate,completeds, retries)
       select date(completeddate),count(*), sum(retry)
         from works 
	 where not isnull(completeddate)
	   and status="COMPLETED"
	 group by date(completeddate);

--  since 5.9.0 deleted rows go to works_history

insert into completedjobs (thedate,completeds)
       select date(completeddate),count(*) 
         from works_history 
	 where not isnull(completeddate)
	   and status="COMPLETED"
	 group by date(completeddate);

-- select "create a temporary table for error jobs"

create temporary table errorjobs (
       thedate datetime,
       errs int(10) default 0
);

-- select "fill temporary table errorjobs";

insert into errorjobs (thedate,errs)
       select date(arrivaldate),count(*) 
         from works 
	 where not isnull(arrivaldate)
	   and status="ERROR"
	 group by date(arrivaldate);

-- since 5.9.0 deleted rows go to works_history

insert into errorjobs (thedate,errs)
       select date(arrivaldate),count(*) 
         from works_history 
	 where not isnull(arrivaldate)
	   and status="ERROR"
	 group by date(arrivaldate);

-- select "create temporary table for pilot jobs (EGEE ressources)";

create temporary table resources (
       thedate datetime,
       resources int(10) default 0
);

-- select "fill temporary table pilotedjobs";

insert into resources (thedate,resources)
       select date(lastalive),count(*)
       from hosts
       where not isnull(lastalive)
       group by date(lastalive);



-- select "create temporary table stats";

create temporary table stats (
       thedate datetime,
       completeds int(10) default 0,
       errs int(10) default 0,
       resources int(10) default 0,
       retry int(10) default 0
);

-- select "inserta all possible dates"

insert into stats (thedate,completeds,retry)
       select date(thedate),completeds,retries
       from completedjobs;
insert into stats (thedate,errs)
       select date(completedjobs.thedate),errorjobs.errs
       from completedjobs
	      join errorjobs
	      on date(errorjobs.thedate)=date(completedjobs.thedate);
insert into stats (thedate,resources)
       select date(resources.thedate),resources
       from resources
	      join completedjobs
	      on date(resources.thedate)=date(completedjobs.thedate);


select date(thedate),
       max(completeds) as completed,
       max(retry) as retry,
       max(errs) as errors,
       max(resources) as resources
       from stats 
       group by thedate
       order by thedate;
