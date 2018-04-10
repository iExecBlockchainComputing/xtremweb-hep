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
--                              for history copied from an 8.x schema
--
-- ===========================================================================

-- -----------------------------------------------------
select 'Table "users_history"' as '';
-- -----------------------------------------------------
update users_history
   set userRightId   = ( select userRights.userRightId
                         from   userRights
                         where  userRights.userRightName = users_history.rights );


-- -----------------------------------------------------
select 'Table "hosts_history"' as '';
-- -----------------------------------------------------
update hosts_history
   set osId      =    ( select oses.osId
                        from   oses
                        where  oses.osName          = hosts_history.os ),
       cpuTypeId =    ( select cpuTypes.cpuTypeId
                        from   cpuTypes
                        where  cpuTypes.cpuTypeName = hosts_history.cputype ),
       usergroupUID = ( select usergroups.uid
                        from   usergroups
                        where  usergroups.label     = hosts_history.project );


-- -----------------------------------------------------
select 'Table "sharedAppTypes_history"' as '';
-- -----------------------------------------------------
insert into sharedAppTypes_history ( hostUID, appTypeId )
select hosts_history.uid, appTypes.appTypeId
from   hosts_history inner join appTypes
on     (hosts_history.sharedApps is not null) and (hosts_history.sharedApps <> '') and
       locate(appTypes.appTypeName, hosts_history.sharedApps);


-- -----------------------------------------------------
select 'Table "sharedPackageTypes_history"' as '';
-- -----------------------------------------------------
insert into sharedPackageTypes_history ( hostUID, packageTypeId )
select hosts_history.uid, packageTypes.packageTypeId
from   hosts_history inner join packageTypes
on     (hosts_history.sharedpackages is not null) and (hosts_history.sharedpackages <> '') and
       locate(packageTypes.packageTypeName, hosts_history.sharedpackages);


-- -----------------------------------------------------
select 'Table "datas_history"' as '';
-- -----------------------------------------------------
update datas_history
   set statusId   = ( select statuses.statusId
                      from   statuses
                      where  statuses.statusName    = datas_history.status ),
       dataTypeId = ( select dataTypes.dataTypeId
                      from   dataTypes
                      where  dataTypes.dataTypeName = datas_history.type ),
       osId       = ( select oses.osId
                      from   oses
                      where  oses.osName            = datas_history.os ),
       cpuTypeId  = ( select cpuTypes.cpuTypeId
                      from   cpuTypes
                      where  cpuTypes.cpuTypeName   = datas_history.cpu );


-- -----------------------------------------------------
select 'Table "apps_history"' as '';
-- -----------------------------------------------------
update apps_history
   set appTypeId     = ( select appTypes.appTypeId
                         from   appTypes
                         where  appTypes.appTypeName = apps_history.type ),
       packageTypeId = ( select packageTypes.packageTypeId
                         from   packageTypes
                         where  packageTypes.packageTypeName = apps_history.neededpackages );


-- -----------------------------------------------------
select 'Table "works_history"' as '';
-- -----------------------------------------------------
update works_history
   set statusId = ( select statuses.statusId
                    from   statuses
                    where  statuses.statusName  = works_history.status );


-- -----------------------------------------------------
select 'Table "tasks_history"' as '';
-- -----------------------------------------------------
update tasks_history
   set statusId = ( select statuses.statusId
                    from   statuses
                    where  statuses.statusName  = tasks_history.status );


-- -----------------------------------------------------
select 'Table "executables_history"' as '';
-- -----------------------------------------------------
set @DATA_TYPE_BINARY = ( select dataTypeId
                          from   dataTypes
                          where  dataTypeName = 'BINARY' );

delimiter //

drop   procedure if exists proc_populate_app_hist_os_cpu_uri_in_exec_hist //

create procedure           proc_populate_app_hist_os_cpu_uri_in_exec_hist()
begin
  insert into executables_history ( appUID, dataTypeId, osId, cpuTypeId, dataUID, dataURI )
  select      appUID, @DATA_TYPE_BINARY, oses.osId, cpuTypes.cpuTypeId, datas_history.uid, dataURI
  from        temp_view_apps_history
  inner join  oses          on  oses.osName          = temp_view_apps_history.osName
  inner join  cpuTypes      on  cpuTypes.cpuTypeName = temp_view_apps_history.cpuTypeName
  left join   datas_history on  datas_history.uid    = right(dataURI, 36)
  where       dataURI is not null;
end;//

delimiter ;

create or replace view temp_view_apps_history as
select uid as appUID, 'LINUX'  as osName, 'IX86'   as cpuTypeName, linux_ix86URI   as dataURI from apps_history;
call proc_populate_app_hist_os_cpu_uri_in_exec_hist();

create or replace view temp_view_apps_history as
select uid as appUID, 'LINUX'  as osName, 'AMD64'  as cpuTypeName, linux_amd64URI  as dataURI from apps_history;
call proc_populate_app_hist_os_cpu_uri_in_exec_hist();

create or replace view temp_view_apps_history as
select uid as appUID, 'LINUX'  as osName, 'X86_64' as cpuTypeName, linux_x86_64URI as dataURI from apps_history;
call proc_populate_app_hist_os_cpu_uri_in_exec_hist();

create or replace view temp_view_apps_history as
select uid as appUID, 'MACOSX' as osName, 'IX86'   as cpuTypeName, macos_ix86URI   as dataURI from apps_history;
call proc_populate_app_hist_os_cpu_uri_in_exec_hist();

create or replace view temp_view_apps_history as
select uid as appUID, 'MACOSX' as osName, 'X86_64' as cpuTypeName, macos_x86_64URI as dataURI from apps_history;
call proc_populate_app_hist_os_cpu_uri_in_exec_hist();

create or replace view temp_view_apps_history as
select uid as appUID, 'WIN32'  as osName, 'IX86'   as cpuTypeName, win32_ix86URI   as dataURI from apps_history;
call proc_populate_app_hist_os_cpu_uri_in_exec_hist();

create or replace view temp_view_apps_history as
select uid as appUID, 'WIN32'  as osName, 'AMD64'  as cpuTypeName, win32_amd64URI  as dataURI from apps_history;
call proc_populate_app_hist_os_cpu_uri_in_exec_hist();

create or replace view temp_view_apps_history as
select uid as appUID, 'WIN32'  as osName, 'X86_64' as cpuTypeName, win32_x86_64URI as dataURI from apps_history;
call proc_populate_app_hist_os_cpu_uri_in_exec_hist();

create or replace view temp_view_apps_history as
select uid as appUID, 'JAVA'   as osName, 'ALL'    as cpuTypeName, javaURI         as dataURI from apps_history;
call proc_populate_app_hist_os_cpu_uri_in_exec_hist();

drop view temp_view_apps_history;
