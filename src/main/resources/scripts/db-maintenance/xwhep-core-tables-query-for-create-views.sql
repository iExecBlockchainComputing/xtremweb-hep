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
--  XtremWeb-HEP Core Tables :  SQL Query on 'information_schema.columns'
--  generating an SQL script for the creation of views in the current schema
--
-- ===========================================================================
      select '-- VIEWS for XtremWeb-HEP core tables' as "";

      select '-- DROP'  as ""
union select 'drop view if exists view_users;'
union select 'drop view if exists view_usergroups;'
union select 'drop view if exists view_hosts;'
union select 'drop view if exists view_traces;'
union select 'drop view if exists view_datas;'
union select 'drop view if exists view_apps;'
union select 'drop view if exists view_sessions;'
union select 'drop view if exists view_groups;'
union select 'drop view if exists view_works;'
union select 'drop view if exists view_tasks;'
;
-- ---------------------------------------------------------------------------
set @mytable = 'users';
-- ---------------------------------------------------------------------------
      select concat('-- ', ucase(@mytable)) as ""
union select concat('create view  view_', @mytable, '  as')
union select 'select'
union select
                 case
                   when column_name like               'userRightId'
                     then '             userRights.userRightName as "userRightName",'
                   when column_name like               'usergroupUID'
                     then '             usergroups.label         as "usergroup",'
                   when column_name like               'ownerUID'
                     then '             users2.login             as "owner",'
                   else
                     concat('             ', table_name, '1.', column_name, ',')
                 end 
      from   information_schema.columns
      where  (table_schema = schema())   and
             (table_name   = @mytable)
union select '             null'
union select concat('from         ', @mytable, ' as users1')
union select concat('left join    userRights       on  ', @mytable, '1.userRightId  = userRights.userRightId')
union select concat('left join    usergroups       on  ', @mytable, '1.usergroupUID = usergroups.uid')
union select concat('left join    users as users2  on  ', @mytable, '1.ownerUID     = users2.uid;')
union select 'show warnings;';

-- ---------------------------------------------------------------------------
set @mytable = 'usergroups';
-- ---------------------------------------------------------------------------
      select concat('-- ', ucase(@mytable)) as ""
union select concat('create view  view_', @mytable, '  as')
union select 'select'
union select
                 if(column_name like 'ownerUID',
                    '             users.login as "owner",',
                    concat('             ', table_name, '.', column_name, ','))
      from   information_schema.columns
      where  (table_schema = schema())   and
             (table_name   = @mytable)
union select '             null'
union select concat('from         ', @mytable)
union select concat('left join    users  on  ', @mytable, '.ownerUID = users.uid;')
union select 'show warnings;';

-- ---------------------------------------------------------------------------
set @mytable = 'hosts';
-- ---------------------------------------------------------------------------
      select concat('-- ', ucase(@mytable)) as ""
union select concat('create view  view_', @mytable, '  as')
union select 'select'
union select
                 case
                   when column_name like           'osI,'
                     then '             oses.osName,'
                   when column_name like           'cpuTypeId'
                     then '             cpuTypes.cpuTypeName,'
                   when column_name like           'usergroupUID'
                     then '             usergroups1.label    as "projectName",'
                   when column_name like           'ownerUID'
                     then '             users.login          as "owner",  usergroups2.label as "usergroup",'
                   else
                     concat('             ', table_name, '.', column_name, ',')
                 end 
      from   information_schema.columns
      where  (table_schema = schema())   and
             (table_name   = @mytable)
union select '             null'
union select concat('from         ', @mytable)
union select concat('left join    oses                       on  ', @mytable, '.osId         = oses.osId')
union select concat('left join    cpuTypes                   on  ', @mytable, '.cpuTypeId    = cpuTypes.cpuTypeId')
union select concat('left join    usergroups as usergroups1  on  ', @mytable, '.usergroupUID = usergroups1.uid')
union select concat('left join    users                      on  ', @mytable, '.ownerUID     = users.uid')
union select concat('left join    usergroups as usergroups2  on  ', 'users',  '.usergroupUID = usergroups2.uid;')
union select 'show warnings;';

-- ---------------------------------------------------------------------------
set @mytable = 'traces';
-- ---------------------------------------------------------------------------
      select concat('-- ', ucase(@mytable)) as ""
union select concat('create view  view_', @mytable, '  as')
union select 'select'
union select
                 case
                   when column_name like  'hostUID'
                     then '             hosts.name  as "host",'
                   when column_name like  'ownerUID'
                     then '             users.login as "owner",  usergroups.label as "usergroup",'
                   else
                     concat('             ', table_name, '.', column_name, ',')
                 end 
      from   information_schema.columns
      where  (table_schema = schema())   and
             (table_name   = @mytable)
union select '             null'
union select concat('from         ', @mytable)
union select concat('left join    hosts       on  ', @mytable, '.hostUID      = hosts.uid')
union select concat('left join    users       on  ', @mytable, '.ownerUID     = users.uid')
union select concat('left join    usergroups  on  ', 'users',  '.usergroupUID = usergroups.uid;')
union select 'show warnings;';

-- ---------------------------------------------------------------------------
set @mytable = 'datas';
-- ---------------------------------------------------------------------------
      select concat('-- ', ucase(@mytable)) as ""
union select concat('create view  view_', @mytable, '  as')
union select 'select'
union select
                 case
                   when column_name like             'statusId'
                     then '             statuses.statusName,'
                   when column_name like             'dataTypeId'
                     then '             dataTypes.dataTypeName,'
                   when column_name like             'osId'
                     then '             oses.osName,'
                   when column_name like             'cpuTypeId'
                     then '             cpuTypes.cpuTypeName,'
                   when column_name like             'ownerUID'
                     then '             users.login as "owner",  usergroups.label as "usergroup",'
                   else
                     concat('             ', table_name, '.', column_name, ',')
                 end 
      from   information_schema.columns
      where  (table_schema = schema())   and
             (table_name   = @mytable)
union select '             null'
union select concat('from         ', @mytable)
union select concat('left join    statuses    on  ', @mytable, '.statusId     = statuses.statusId')
union select concat('left join    dataTypes   on  ', @mytable, '.dataTypeId   = dataTypes.dataTypeId')
union select concat('left join    oses        on  ', @mytable, '.osId         = oses.osId')
union select concat('left join    cpuTypes    on  ', @mytable, '.cpuTypeId    = cpuTypes.cpuTypeId')
union select concat('left join    users       on  ', @mytable, '.ownerUID     = users.uid')
union select concat('left join    usergroups  on  ', 'users',  '.usergroupUID = usergroups.uid;')
union select 'show warnings;';

-- ---------------------------------------------------------------------------
set @mytable = 'apps';
-- ---------------------------------------------------------------------------
      select concat('-- ', ucase(@mytable)) as ""
union select concat('create view  view_', @mytable, '  as')
union select 'select'
union select
                 case
                   when column_name like           'appTypeId'
                     then '             appTypes.appTypeName,'
                   when column_name like           'packageTypeId'
                     then '             packageTypes.packageTypeName,'
                   when column_name like           'ownerUID'
                     then '             users.login as "owner",  usergroups.label as "usergroup",'
                   else
                     concat('             ', table_name, '.', column_name, ',')
                 end 
      from   information_schema.columns
      where  (table_schema = schema())   and
             (table_name   = @mytable)
union select '             null'
union select concat('from         ', @mytable)
union select concat('left join    appTypes     on  ', @mytable, '.appTypeId     = appTypes.appTypeId')
union select concat('left join    packageTypes on  ', @mytable, '.packageTypeId = packageTypes.packageTypeId')
union select concat('left join    users        on  ', @mytable, '.ownerUID      = users.uid')
union select concat('left join    usergroups   on  ', 'users',  '.usergroupUID  = usergroups.uid;')
union select 'show warnings;';

-- ---------------------------------------------------------------------------
set @mytable = 'sessions';
-- ---------------------------------------------------------------------------
      select concat('-- ', ucase(@mytable)) as ""
union select concat('create view  view_', @mytable, '  as')
union select 'select'
union select
                 if(column_name like 'ownerUID',
                    '             users.login as "owner",  usergroups.label as "usergroup",',
                    concat('             ', table_name, '.', column_name, ','))
      from   information_schema.columns
      where  (table_schema = schema())   and
             (table_name   = @mytable)
union select '             null'
union select concat('from         ', @mytable)
union select concat('left join    users       on  ', @mytable, '.ownerUID     = users.uid')
union select concat('left join    usergroups  on  ', 'users',  '.usergroupUID = usergroups.uid;')
union select 'show warnings;';

-- ---------------------------------------------------------------------------
set @mytable = 'groups';
-- ---------------------------------------------------------------------------
      select concat('-- ', ucase(@mytable)) as ""
union select concat('create view  view_', @mytable, '  as')
union select 'select'
union select
                 case
                   when column_name like    'sessionUID'
                     then '             sessions.name as "session",'
                   when column_name like    'ownerUID'
                     then '             users.login   as "owner",  usergroups.label as "usergroup",'
                   else
                     concat('             ', table_name, '.', column_name, ',')
                 end 
      from   information_schema.columns
      where  (table_schema = schema())   and
             (table_name   = @mytable)
union select '             null'
union select concat('from         ', @mytable)
union select concat('left join    sessions    on  ', @mytable, '.sessionUID   = sessions.uid')
union select concat('left join    users       on  ', @mytable, '.ownerUID     = users.uid')
union select concat('left join    usergroups  on  ', 'users',  '.usergroupUID = usergroups.uid;')
union select 'show warnings;';

-- ---------------------------------------------------------------------------
set @mytable = 'works';
-- ---------------------------------------------------------------------------
      select concat('-- ', ucase(@mytable)) as ""
union select concat('create view  view_', @mytable, '  as')
union select 'select'
union select
                 case
                   when column_name like          'appUID'
                     then '             apps.name           as "application",'
                   when column_name like          'statusId'
                     then '             statuses.statusName as "statusName",'
                   when column_name like          'sessionUID'
                     then '             sessions.name       as "session",'
                   when column_name like          'groupUID'
                     then '             groups.name         as "group",'
                   when column_name like          'expectedhostUID'
                     then '             hosts.name          as "expectedhost",'
                   when column_name like          'ownerUID'
                     then '             users.login         as "owner",  usergroups.label as "usergroup",'
                   else
                     concat('             ', table_name, '.', column_name, ',')
                 end 
      from   information_schema.columns
      where  (table_schema = schema())   and
             (table_name   = @mytable)
union select '             null'
union select concat('from         ', @mytable)
union select concat('left join    apps        on  ', @mytable, '.appUID          = apps.uid')
union select concat('left join    statuses    on  ', @mytable, '.statusId        = statuses.statusId')
union select concat('left join    sessions    on  ', @mytable, '.sessionUID      = sessions.uid')
union select concat('left join    groups      on  ', @mytable, '.groupUID        = groups.uid')
union select concat('left join    hosts       on  ', @mytable, '.expectedhostUID = hosts.uid')
union select concat('left join    users       on  ', @mytable, '.ownerUID        = users.uid')
union select concat('left join    usergroups  on  ', 'users',  '.usergroupUID    = usergroups.uid;')
union select 'show warnings;';

-- ---------------------------------------------------------------------------
set @mytable = 'tasks';
-- ---------------------------------------------------------------------------
      select concat('-- ', ucase(@mytable)) as ""
union select concat('create view  view_', @mytable, '  as')
union select 'select'
union select
                 case
                   when column_name like          'workUID'
                     then concat('apps.name    as "application",  ', table_name, '.', column_name, ',')
                   when column_name like          'hostUID'
                     then '             hosts.name          as "host",'
                   when column_name like          'statusId'
                     then '             statuses.statusName as "statusName",'
                   when column_name like          'ownerUID'
                     then '             users.login         as "owner",  usergroups.label as "usergroup",'
                   else
                     concat('             ', table_name, '.', column_name, ',')
                 end 
      from   information_schema.columns
      where  (table_schema = schema())   and
             (table_name   = @mytable)
union select '             null'
union select concat('from         ', @mytable)
union select concat('left join    works       on  ', @mytable, '.workUID      = works.uid')
union select concat('left join    apps        on  ', 'works',  '.appUID       = apps.uid')
union select concat('left join    hosts       on  ', @mytable, '.hostUID      = hosts.uid')
union select concat('left join    statuses    on  ', @mytable, '.statusId     = statuses.statusId')
union select concat('left join    users       on  ', @mytable, '.ownerUID     = users.uid')
union select concat('left join    usergroups  on  ', 'users',  '.usergroupUID = usergroups.uid;')
union select 'show warnings;';
