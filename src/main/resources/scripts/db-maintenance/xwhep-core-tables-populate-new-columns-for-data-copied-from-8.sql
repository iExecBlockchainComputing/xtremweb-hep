-- ===========================================================================
--
--  Copyright 2013-2014  E. URBAH
--                       at LAL, Univ Paris-Sud, IN2P3/CNRS, Orsay, France
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
--  XtremWeb-HEP Core Tables :  SQL script to populate the new columns
--                              for data copied from an 8.x schema
--
-- ===========================================================================

-- -----------------------------------------------------
select 'Table "users"' as '';
-- -----------------------------------------------------
update users
   set userRightId   = ( select userRights.userRightId
                         from   userRights
                         where  userRights.userRightName = users.rights );


-- -----------------------------------------------------
select 'Table "hosts"' as '';
-- -----------------------------------------------------
update hosts
   set osId         = ( select oses.osId
                        from   oses
                        where  oses.osName          = hosts.os ),
       cpuTypeId    = ( select cpuTypes.cpuTypeId
                        from   cpuTypes
                        where  cpuTypes.cpuTypeName = hosts.cputype ),
       usergroupUID = ( select usergroups.uid
                        from   usergroups
                        where  usergroups.label     = hosts.project );


-- -----------------------------------------------------
select 'Table "sharedAppTypes"' as '';
-- -----------------------------------------------------
insert into sharedAppTypes ( hostUID, appTypeId )
select hosts.uid, appTypes.appTypeId
from   hosts inner join appTypes
on     (hosts.sharedapps is not null) and (hosts.sharedapps <> '') and
       locate(appTypes.appTypeName, hosts.sharedapps);


-- -----------------------------------------------------
select 'Table "sharedPackageTypes"' as '';
-- -----------------------------------------------------
insert into sharedPackageTypes ( hostUID, packageTypeId )
select hosts.uid, packageTypes.packageTypeId
from   hosts inner join packageTypes
on     (hosts.sharedpackages is not null) and (hosts.sharedpackages <> '') and
       locate(packageTypes.packageTypeName, hosts.sharedpackages);


-- -----------------------------------------------------
select 'Table "datas"' as '';
-- -----------------------------------------------------
update datas
   set statusId   = ( select statuses.statusId
                      from   statuses
                      where  statuses.statusName    = datas.status ),
       dataTypeId = ( select dataTypes.dataTypeId
                      from   dataTypes
                      where  dataTypes.dataTypeName = datas.type ),
       osId       = ( select oses.osId
                      from   oses
                      where  oses.osName            = datas.os ),
       cpuTypeId  = ( select cpuTypes.cpuTypeId
                      from   cpuTypes
                      where  cpuTypes.cpuTypeName   = datas.cpu );


-- -----------------------------------------------------
select 'Table "apps"' as '';
-- -----------------------------------------------------
update apps
   set appTypeId =     ( select appTypes.appTypeId
                         from   appTypes
                         where  appTypes.appTypeName = apps.type ),
       packageTypeId = ( select packageTypes.packageTypeId
                         from   packageTypes
                         where  packageTypes.packageTypeName = apps.neededpackages );


-- -----------------------------------------------------
select 'Table "works"' as '';
-- -----------------------------------------------------
update works
   set statusId = ( select statuses.statusId
                    from   statuses
                    where  statuses.statusName  = works.status );


-- -----------------------------------------------------
select 'Table "tasks"' as '';
-- -----------------------------------------------------
update tasks
   set statusId = ( select statuses.statusId
                    from   statuses
                    where  statuses.statusName  = tasks.status );


-- -----------------------------------------------------
select 'Table "executables"' as '';
-- -----------------------------------------------------
set @DATA_TYPE_BINARY = ( select dataTypeId
                          from   dataTypes
                          where  dataTypeName = 'BINARY' );

delimiter //

drop   procedure if exists proc_populate_app_os_cpu_uri_in_executables //

create procedure           proc_populate_app_os_cpu_uri_in_executables()
begin
  insert into executables ( appUID, dataTypeId, osId, cpuTypeId, dataUID, dataURI )
  select      appUID, @DATA_TYPE_BINARY, oses.osId, cpuTypes.cpuTypeId, datas.uid, dataURI
  from        temp_view_apps
  inner join  oses      on  oses.osName          = temp_view_apps.osName
  inner join  cpuTypes  on  cpuTypes.cpuTypeName = temp_view_apps.cpuTypeName
  left  join  datas     on  datas.uid            = right(dataURI, 36)
  where       dataURI is not null;
end;//

delimiter ;

create or replace view temp_view_apps as
select uid as appUID, 'LINUX'  as osName, 'IX86'   as cpuTypeName, linux_ix86URI   as dataURI from apps;
call proc_populate_app_os_cpu_uri_in_executables();

create or replace view temp_view_apps as
select uid as appUID, 'LINUX'  as osName, 'AMD64'  as cpuTypeName, linux_amd64URI  as dataURI from apps;
call proc_populate_app_os_cpu_uri_in_executables();

create or replace view temp_view_apps as
select uid as appUID, 'LINUX'  as osName, 'X86_64' as cpuTypeName, linux_x86_64URI as dataURI from apps;
call proc_populate_app_os_cpu_uri_in_executables();

create or replace view temp_view_apps as
select uid as appUID, 'MACOSX' as osName, 'IX86'   as cpuTypeName, macos_ix86URI   as dataURI from apps;
call proc_populate_app_os_cpu_uri_in_executables();

create or replace view temp_view_apps as
select uid as appUID, 'MACOSX' as osName, 'X86_64' as cpuTypeName, macos_x86_64URI as dataURI from apps;
call proc_populate_app_os_cpu_uri_in_executables();

create or replace view temp_view_apps as
select uid as appUID, 'WIN32'  as osName, 'IX86'   as cpuTypeName, win32_ix86URI   as dataURI from apps;
call proc_populate_app_os_cpu_uri_in_executables();

create or replace view temp_view_apps as
select uid as appUID, 'WIN32'  as osName, 'AMD64'  as cpuTypeName, win32_amd64URI  as dataURI from apps;
call proc_populate_app_os_cpu_uri_in_executables();

create or replace view temp_view_apps as
select uid as appUID, 'WIN32'  as osName, 'X86_64' as cpuTypeName, win32_x86_64URI as dataURI from apps;
call proc_populate_app_os_cpu_uri_in_executables();

create or replace view temp_view_apps as
select uid as appUID, 'JAVA'   as osName, 'ALL'    as cpuTypeName, javaURI         as dataURI from apps;
call proc_populate_app_os_cpu_uri_in_executables();

drop view temp_view_apps;
