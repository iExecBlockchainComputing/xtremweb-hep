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
--  Creation of views for following relationships :  - executables
--                                                   - sharedAppTypes
--                                                   - sharedPackageTypes
--
-- ===========================================================================
drop view if exists view_sharedPackageTypes;
drop view if exists view_sharedAppTypes;
drop view if exists view_executables;

-- ---------------------------------------------------------------------------
create view     view_executables as
-- ---------------------------------------------------------------------------
select          executables.executableId  as executableId,
                apps.name                 as application,
                appTypes.appTypeName      as appType,
                dataTypes.dataTypeName    as dataType,
                oses.osName,
                executables.osVersion,
                cpuTypes.cpuTypeName      as cpuType,
                datas.name                as dataName,
                datas.size                as dataSize,
                statuses.statusName,
                users.login               as owner,
                usergroups.label          as ownergroup,
                executables.dataURI,
                executables.mtime
from            executables
left join       apps        on  apps.uid             = executables.appUID
left join       appTypes    on  appTypes.appTypeId   = apps.appTypeId
left join       dataTypes   on  dataTypes.dataTypeId = executables.dataTypeId
left join       oses        on  oses.osId            = executables.osId
left join       cpuTypes    on  cpuTypes.cpuTypeId   = executables.cpuTypeId
left join       datas       on  datas.uid            = executables.dataUID
left join       statuses    on  statuses.statusId    = datas.statusId
left join       users       on  users.uid            = apps.ownerUID
left join       usergroups  on  usergroups.uid       = users.usergroupUID
order by        application, appType, dataType,
                osName, osversion, cpuType, dataName;

show warnings;

-- ---------------------------------------------------------------------------
create view     view_sharedAppTypes as
-- ---------------------------------------------------------------------------
select distinct appTypes.appTypeName,
                hosts.name            as "hostname",
                hosts.ipaddr,
                hosts.hwaddr,
                cpuTypes.cpuTypeName  as "cputype",
                hosts.cpunb,
                hosts.cpumodel,
                oses.osName,
                hosts.osversion,
                appTypes.mtime,
                appTypes.appTypeDescription
from            sharedAppTypes
inner join      appTypes       on  appTypes.appTypeId = sharedAppTypes.appTypeId
inner join      hosts          on  hosts.uid          = sharedAppTypes.hostUID
left  join      cpuTypes       on  cpuTypes.cpuTypeId = hosts.cpuTypeId
left  join      oses           on  oses.osId          = hosts.osId
order by        appTypes.appTypeName, hosts.name,
                hosts.ipaddr,         hosts.hwaddr,
                hosts.cputype,        hosts.cpunb,  hosts.cpumodel,
                hosts.os,  hosts.osversion;

show warnings;

-- ---------------------------------------------------------------------------
create view     view_sharedPackageTypes as
-- ---------------------------------------------------------------------------
select distinct packageTypes.packageTypeName,
                hosts.name            as "hostname",
                hosts.ipaddr,
                hosts.hwaddr,
                cpuTypes.cpuTypeName  as "cputype",
                hosts.cpunb,
                hosts.cpumodel,
                oses.osName,
                hosts.osversion,
                packageTypes.mtime,
                packageTypes.packageTypeDescription
from            sharedPackageTypes
inner join      packageTypes   on  packageTypes.packageTypeId = sharedPackageTypes.packageTypeId
inner join      hosts          on  hosts.uid                  = sharedPackageTypes.hostUID
left  join      cpuTypes       on  cpuTypes.cpuTypeId         = hosts.cpuTypeId
left  join      oses           on  oses.osId                  = hosts.osId
order by        packageTypes.packageTypeName, hosts.name,
                hosts.ipaddr,         hosts.hwaddr,
                hosts.cputype,        hosts.cpunb,  hosts.cpumodel,
                hosts.os,  hosts.osversion;

show warnings;
