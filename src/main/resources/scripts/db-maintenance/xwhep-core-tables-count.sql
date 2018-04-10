-- ===========================================================================
--
--  Copyright 2013  E. URBAH
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
--  XtremWeb-HEP Core Tables :  SQL script counting the rows of each table
--
-- ===========================================================================
select table_name                                             as "Table",
       if(nb_live_total = nb_live_ok, '',
                  cast((nb_live_total - nb_live_ok) as char)) as "# Live Bad",
       if(nb_live_ok    = 0, '', cast(nb_live_ok    as char)) as "# Live OK",
       if(nb_live_total = 0, '', cast(nb_live_total as char)) as "# Live Total",
       if(nb_history    = 0, '', cast(nb_history    as char)) as "# History",
       if(nb_live_total + nb_history = 0, '',
                  cast((nb_live_total + nb_history) as char)) as "# Total"
from
(
select concat('XtremWeb-HEP    Schema = ', schema(), '    ', now())
       as table_name, '' as nb_live_ok, '' as nb_live_total, '' as nb_history
union
select 'versions',           ( select count(*) from versions ),
                             ( select count(*) from versions ),
                             ''
union
select 'userRights',         ( select count(*) from userRights ),
                             ( select count(*) from userRights ),
                             ''
union
select 'statuses',           ( select count(*) from statuses ),
                             ( select count(*) from statuses ),
                             ''
union
select 'dataTypes',          ( select count(*) from dataTypes ),
                             ( select count(*) from dataTypes ),
                             ''
union
select 'appTypes',           ( select count(*) from appTypes ),
                             ( select count(*) from appTypes ),
                             ''
union
select 'packageTypes',       ( select count(*) from packageTypes ),
                             ( select count(*) from packageTypes ),
                             ''
union
select 'oses',               ( select count(*) from oses ),
                             ( select count(*) from oses ),
                             ''
union
select 'cpuTypes',           ( select count(*) from cpuTypes ),
                             ( select count(*) from cpuTypes ),
                             ''
union
select 'users',              ( select count(*) from users
                               where   (ownerUID in (select users2.uid from users as users2)) and
                                     ( (usergroupUID is null) or
                                       (usergroupUID in (select uid from groups)) ) ),
                             ( select count(*) from users ),
                             ( select count(*) from users_history )
union
select 'usergroups',         ( select count(*) from usergroups
                               where   (ownerUID in (select uid from users)) ),
                             ( select count(*) from usergroups ),
                             ( select count(*) from usergroups_history )
union
select 'hosts',              ( select count(*) from hosts
                               where   (ownerUID in (select uid from users)) and
                                     ( (project  is null) or
                                       (project  =  '')   or
                                       (project  in (select label from usergroups)) ) ),
                             ( select count(*) from hosts ),
                             ( select count(*) from hosts_history )
union
select 'traces',             ( select count(*) from traces
                               where   (ownerUID in (select uid from users)) and
                                     ( (hostUID is null) or
                                       (hostUID in (select uid from hosts)) ) ),
                             ( select count(*) from traces),
                             ( select count(*) from traces_history )
union
select 'sharedAppTypes',     ( select count(*) from sharedAppTypes
                               where   (hostUID   in (select uid       from hosts   )) and
                                       (appTypeId in (select appTypeId from appTypes)) ),
                             ( select count(*) from sharedAppTypes ),
                             ( select count(*) from sharedAppTypes_history )
union
select 'sharedPackageTypes', ( select count(*) from sharedPackageTypes
                               where   (hostUID   in (select uid       from hosts   )) and
                                       (packageTypeId in (select packageTypeId from packageTypes)) ),
                             ( select count(*) from sharedPackageTypes ),
                             ( select count(*) from sharedPackageTypes_history )
union
select 'datas',              ( select count(*) from datas
                               where   (ownerUID   in (select        uid from users    )) ),
                             ( select count(*) from datas ),
                             ( select count(*) from datas_history )
union
select 'apps',               ( select count(*) from apps
                               where   (ownerUID in (select uid from users)) ),
                             ( select count(*) from apps ),
                             ( select count(*) from apps_history )
union
select 'executables',        ( select count(*) from executables
                               where   (dataTypeId  in (select dataTypeId     from dataTypes)) and
                                       (appUID      in (select uid            from apps     )) and
                                       (osId        in (select osId           from oses     )) and
                                       (cpuTypeId   in (select cpuTypeId      from cpuTypes )) ),
                             ( select count(*) from executables ),
                             ( select count(*) from executables_history )
union
select 'sessions',           ( select count(*) from sessions
                               where   (ownerUID in (select uid from users)) ),
                             ( select count(*) from sessions ),
                             ( select count(*) from sessions_history )
union
select 'groups',             ( select count(*) from groups
                               where   (ownerUID in (select uid from users)) and
                                     ( (sessionUID is null) or
                                       (sessionUID in (select uid from sessions)) ) ),
                             ( select count(*) from groups ),
                             ( select count(*) from groups_history )
union
select 'works',              ( select count(*) from works
                               where   (ownerUID        in (select uid from users))      and
                                       (appUID          in (select uid from apps ))      and
                                     ( (expectedhostUID is null) or
                                       (expectedhostUID in (select uid from hosts   )) ) and
                                     ( (sessionUID      is null) or
                                       (sessionUID      in (select uid from sessions)) ) and
                                     ( (groupUID        is null) or
                                       (groupUID        in (select uid from groups  )) ) ),
                             ( select count(*) from works ),
                             ( select count(*) from works_history )
union
select 'tasks',              ( select count(*) from tasks
                               where   (ownerUID   in (select uid from users)) and
                                       (workUID    in (select uid from works)) and
                                     ( (hostUID    is null) or
                                       (hostUID    in (select uid from hosts)) ) ),
                             ( select count(*) from tasks ),
                             ( select count(*) from tasks_history )
) as xwhep_core_tables_count;
