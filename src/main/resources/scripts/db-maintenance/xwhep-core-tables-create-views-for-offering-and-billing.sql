-- ===========================================================================
-- 
--  Copyright 2014  E. URBAH
--                  at LAL, Univ Paris-Sud, IN2P3/CNRS, Orsay, France
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
--  XtremWeb-HEP Core Tables :
--  SQL script creating the views for offering and billing :
--             -  view_apps_for_offering
--             -  view_apps_for_offering_with_file_sizes
--             -  view_works_for_billing
--             -  view_works_for_billing_with_file_sizes
-- 
-- ===========================================================================
drop view if exists  view_works_for_billing_with_file_sizes;
drop view if exists  view_works_for_billing;

drop view if exists  view_apps_for_offering_with_file_sizes;
drop view if exists  view_apps_for_offering;


-- ---------------------------------------------------------------------------
create  view  view_apps_for_offering  as
-- ---------------------------------------------------------------------------
select        apps.uid,
              apps.name,
              appTypes.appTypeName             as "appTypeName",
              users.login                      as "owner",
              apps.minFreeMassStorage
from          apps
left join     appTypes         on  apps.appTypeId = appTypes.appTypeId
left join     users            on  apps.ownerUID  = users.uid
group by      apps.uid, apps.name, appTypeName, owner
order by      apps.name;

show warnings;

-- ---------------------------------------------------------------------------
create  view  view_apps_for_offering_with_file_sizes  as
-- ---------------------------------------------------------------------------
select        apps.uid,
              apps.name,
              appTypes.appTypeName             as "appTypeName",
              users.login                      as "owner",
              ( ifnull(    datas1.size,  0) +
                ifnull(    datas2.size,  0) +
                ifnull(    datas3.size,  0) +
                ifnull(max(datas4.size), 0) )  as "appFilesTotalSize",
              apps.minFreeMassStorage
from          apps
left join     appTypes         on  apps.appTypeId = appTypes.appTypeId
left join     users            on  apps.ownerUID  = users.uid
left join     datas as datas1  on  datas1.uid     = right(defaultStdinURI, 36)
left join     datas as datas2  on  datas2.uid     = right(baseDirinURI,    36)
left join     datas as datas3  on  datas3.uid     = right(defaultDirinURI, 36)
left join     executables      on  apps.uid       = executables.appUID
left join     datas as datas4  on  datas4.uid     = executables.dataUID
group by      apps.uid, apps.name, appTypeName, owner
order by      apps.name;

show warnings;


-- ---------------------------------------------------------------------------
create  view  view_works_for_billing  as
-- ---------------------------------------------------------------------------
select        works.uid,
              statuses.statusName,
              users.login                           as "owner",
              works.minFreeMassStorage,
              works.completedDate,
              (unix_timestamp(works.compenddate) -
               unix_timestamp(works.compstartdate)) as "compDuration"
from          works         force index (completedDate)
left join     statuses  on  works.statusId = statuses.statusId
left join     users     on  works.ownerUID = users.uid;

show warnings;

-- ---------------------------------------------------------------------------
create  view  view_works_for_billing_with_file_sizes  as
-- ---------------------------------------------------------------------------
select        works.uid,
              statuses.statusName,
              users.login                           as "owner",
              works.minFreeMassStorage,
              datas1.size                           as "stdinSize",
              datas2.size                           as "dirinSize",
              datas3.size                           as "resultSize",
              works.completedDate,
              (unix_timestamp(works.compenddate) -
               unix_timestamp(works.compstartdate)) as "compDuration"
from          works                force index (completedDate)
left join     statuses         on  works.statusId = statuses.statusId
left join     users            on  works.ownerUID = users.uid
left join     datas as datas1  on  datas1.uid     = right(works.stdinURI,  36)
left join     datas as datas2  on  datas2.uid     = right(works.dirinURI,  36)
left join     datas as datas3  on  datas3.uid     = right(works.resultURI, 36)
;

show warnings;
