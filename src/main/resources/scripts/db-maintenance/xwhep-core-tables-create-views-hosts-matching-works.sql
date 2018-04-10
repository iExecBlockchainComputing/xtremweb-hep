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
--  SQL script creating the views on 'hosts' matching pending 'works' :
-- 
--  Usage :  select  hostUID  from   view_hosts_matching_works_...
--                            where  workUID = '.....';
-- 
--  1 view for hosts accepting Deployable applications
--             (through the 'executables', 'oses' and 'cpuTypes' tables).
-- 
--  1 view for hosts hosting Shared applications
--             (through the 'sharedAppTypes' table),
-- 
--  Note :   The view taking into account simultaneously Deployable and Shared
--           applications can NOT use inner joins, but only left joins, and
--           is slower.  Therefore, I advise NOT to use it.
-- 
-- ===========================================================================
drop view if exists  view_hosts_matching_works_deployable;
drop view if exists  view_hosts_matching_works_shared;
drop view if exists  view_hosts_matching_works_deployable_and_shared;


-- ===========================================================================
-- 
create  view  view_hosts_matching_works_deployable  as
-- 
-- ===========================================================================
select      works.uid                as  workUID,
            works.accessrights       as  workAccessRights,
            works.arrivalDate        as  workArrivalDate,
            work_owners.login        as  workOwner,
            usergroups.label         as  workUsergroup,
            apps.name                as  appName,
            appTypes.appTypeName     as  appType,
            e_oses.osName            as  executableOsName,
            executables.osVersion    as  executableOsVersion,
            e_cpuTypes.cpuTypeName   as  executableCpuType,
            hosts.uid                as  hostUID,
            hosts.accessrights       as  hostAccessRights,
            hosts.name               as  hostName,
            hosts.project            as  hostProject,
            host_owners.login        as  hostOwner,
            h_oses.osName            as  hostOsName,
            h_cpuTypes.cpuTypeName   as  hostCpuType,
            datas.uid                as  dataUID,
            datas.name               as  dataName

-- ---------------------------------------------------------------------------
--  Tables joins for systematic matchmaking on works
-- ---------------------------------------------------------------------------
from        works
inner join  statuses                 on    statuses.statusId                 = works.statusId
inner join  apps                     on    apps.uid                          = works.appUID
inner join  appTypes                 on    appTypes.appTypeId                = apps.appTypeId
inner join  hosts                    on    ( ( works.expectedhostUID         = hosts.uid ) or 
                                             ( works.expectedhostUID        is NULL ) )

-- ---------------------------------------------------------------------------
--  Tables joins for matchmaking on Deployable applications
-- ---------------------------------------------------------------------------
inner join  executables              on    executables.appUID                = apps.uid
inner join  oses     as e_oses       on    e_oses.osId                       = executables.osId
inner join  cpuTypes as e_cpuTypes   on    e_cpuTypes.cpuTypeId              = executables.cpuTypeId

-- ---------------------------------------------------------------------------
--  Tables joins for systematic matchmaking on access rights
-- ---------------------------------------------------------------------------
inner join  users as work_owners     on    work_owners.uid                   = works.ownerUID
inner join  users as host_owners     on    host_owners.uid                   = hosts.ownerUID

-- ---------------------------------------------------------------------------
--  Tables joins for matchmaking on specific requirements
-- ---------------------------------------------------------------------------
left  join  usergroups               on    usergroups.uid                    = work_owners.usergroupUID
left  join  sharedPackageTypes       on    (sharedPackageTypes.packageTypeId = apps.packageTypeId) and
                                           (sharedPackageTypes.hostUID       = hosts.uid)

-- ---------------------------------------------------------------------------
--  Tables joins for human readable display
-- ---------------------------------------------------------------------------
left  join  oses     as h_oses       on    h_oses.osId                       = hosts.osId
left  join  cpuTypes as h_cpuTypes   on    h_cpuTypes.cpuTypeId              = hosts.cpuTypeId
left  join  datas                    on    datas.uid                         = executables.dataUID

where
-- ---------------------------------------------------------------------------
--  Selection criteria on works
-- ---------------------------------------------------------------------------
      ( statuses.statusName = 'PENDING' )                  and  --  Work status must be 'PENDING'

-- ---------------------------------------------------------------------------
--  Selection criteria on Deployable applications
-- ---------------------------------------------------------------------------
      ( ( appTypes.appTypeName     = 'DEPLOYABLE' )    and      --  Application type must be 'DEPLOYABLE'

        ( ( executables.osId       = hosts.osId ) or            --  Executable OS name must match Host OS name, or be 'JAVA'
          ( e_oses.osName          = 'JAVA' ) )        and

        ( ( executables.osVersion  = hosts.osversion ) or       --  Executable OS version must match Host OS version, or be NULL
          ( executables.osVersion is null ) )          and

        ( ( executables.cpuTypeId  = hosts.cpuTypeId ) or       --  Executable CPU type must match Host CPU type, or be 'ALL'
          ( e_cpuTypes.cpuTypeName = 'ALL' ) ) )           and

-- ---------------------------------------------------------------------------
--  Selection criteria on access rights
-- ---------------------------------------------------------------------------
      ( (    hosts.ownerUID = works.ownerUID ) or               --  Work owner must match Host owner, or

        ( (  hosts.accessrights       & 5 = 5 ) and             --  Work and Host must both be 'rx' by others, or
          (  works.accessrights       & 5 = 5 ) ) or

        ( ( (hosts.accessrights >> 4) & 5 = 5 ) and             --  Work and Host must both be 'rx' by group members, and
          ( (works.accessrights >> 4) & 5 = 5 ) and

          ( ( host_owners.usergroupUID =
              work_owners.usergroupUID ) or                         --  Work usergroup must match Host usergroup, or

            ( host_owners.usergroupUID in                           --  One Work group membership must match Host usergroup
              ( select usergroupUID from memberships
                where  userUID = works.ownerUID ) ) ) ) )  and

-- ---------------------------------------------------------------------------
--  Selection criteria on specific requirements
-- ---------------------------------------------------------------------------
      ( ( apps.packageTypeId               is NULL ) or         --  Application needed package must be NULL
        ( sharedPackageTypes.packageTypeId is NOT NULL ) ) and  --  or in 'hosts.sharedpackages'

      ( ( hosts.project  is NULL ) or                           --  Host project must be NULL, '' or match Usergroup label
        ( hosts.project  = ''    ) or
        ( hosts.project  = usergroups.label ) )

-- ---------------------------------------------------------------------------
order by  workArrivalDate;


-- ===========================================================================
-- 
create  view  view_hosts_matching_works_shared  as
-- 
-- ===========================================================================
select      works.uid                as  workUID,
            works.accessrights       as  workAccessRights,
            works.arrivalDate        as  workArrivalDate,
            work_owners.login        as  workOwner,
            usergroups.label         as  workUsergroup,
            apps.name                as  appName,
            appTypes.appTypeName     as  appType,
            null                     as  executableOsName,
            null                     as  executableOsVersion,
            null                     as  executableCpuType,
            hosts.uid                as  hostUID,
            hosts.accessrights       as  hostAccessRights,
            hosts.name               as  hostName,
            hosts.project            as  hostProject,
            host_owners.login        as  hostOwner,
            h_oses.osName            as  hostOsName,
            h_cpuTypes.cpuTypeName   as  hostCpuType,
            null                     as  dataUID,
            null                     as  dataName

-- ---------------------------------------------------------------------------
--  Tables joins for systematic matchmaking on works
-- ---------------------------------------------------------------------------
from        works
inner join  statuses                 on    statuses.statusId                 = works.statusId
inner join  apps                     on    apps.uid                          = works.appUID
inner join  appTypes                 on    appTypes.appTypeId                = apps.appTypeId
inner join  hosts                    on    ( ( works.expectedhostUID         = hosts.uid ) or 
                                             ( works.expectedhostUID        is NULL ) )

-- ---------------------------------------------------------------------------
--  Tables joins for matchmaking on Shared applications
-- ---------------------------------------------------------------------------
inner join  sharedAppTypes           on    sharedAppTypes.appTypeId          = apps.appTypeId

-- ---------------------------------------------------------------------------
--  Tables joins for systematic matchmaking on access rights
-- ---------------------------------------------------------------------------
inner join  users as work_owners     on    work_owners.uid                   = works.ownerUID
inner join  users as host_owners     on    host_owners.uid                   = hosts.ownerUID

-- ---------------------------------------------------------------------------
--  Tables joins for matchmaking on specific requirements
-- ---------------------------------------------------------------------------
left  join  usergroups               on    usergroups.uid                    = work_owners.usergroupUID
left  join  sharedPackageTypes       on    (sharedPackageTypes.packageTypeId = apps.packageTypeId) and
                                           (sharedPackageTypes.hostUID       = hosts.uid)

-- ---------------------------------------------------------------------------
--  Tables joins for human readable display
-- ---------------------------------------------------------------------------
left  join  oses     as h_oses       on    h_oses.osId                       = hosts.osId
left  join  cpuTypes as h_cpuTypes   on    h_cpuTypes.cpuTypeId              = hosts.cpuTypeId

where
-- ---------------------------------------------------------------------------
--  Selection criteria on works
-- ---------------------------------------------------------------------------
      ( statuses.statusName = 'PENDING' )                  and  --  Work status must be 'PENDING'

-- ---------------------------------------------------------------------------
--  Selection criteria on Shared applications
-- ---------------------------------------------------------------------------
      ( hosts.uid         = sharedAppTypes.hostUID )       and  --  Host must have the required Shared application

-- ---------------------------------------------------------------------------
--  Selection criteria on access rights
-- ---------------------------------------------------------------------------
      ( (    hosts.ownerUID = works.ownerUID ) or               --  Work owner must match Host owner, or

        ( (  hosts.accessrights       & 5 = 5 ) and             --  Work and Host must both be 'rx' by others, or
          (  works.accessrights       & 5 = 5 ) ) or

        ( ( (hosts.accessrights >> 4) & 5 = 5 ) and             --  Work and Host must both be 'rx' by group members, and
          ( (works.accessrights >> 4) & 5 = 5 ) and

          ( ( host_owners.usergroupUID =
              work_owners.usergroupUID ) or                         --  Work usergroup must match Host usergroup, or

            ( host_owners.usergroupUID in                           --  One Work group membership must match Host usergroup
              ( select usergroupUID from memberships
                where  userUID = works.ownerUID ) ) ) ) )  and

-- ---------------------------------------------------------------------------
--  Selection criteria on specific requirements
-- ---------------------------------------------------------------------------
      ( ( apps.packageTypeId               is NULL ) or         --  Application needed package must be NULL
        ( sharedPackageTypes.packageTypeId is NOT NULL ) ) and  --  or in 'hosts.sharedpackages'

      ( ( hosts.project  is NULL ) or                           --  Host project must be NULL, '' or match Usergroup label
        ( hosts.project  = ''    ) or
        ( hosts.project  = usergroups.label ) )

-- ---------------------------------------------------------------------------
order by  workArrivalDate;


-- ===========================================================================
-- 
create  view  view_hosts_matching_works_deployable_and_shared  as
-- 
-- ===========================================================================
select      works.uid                as  workUID,
            works.accessrights       as  workAccessRights,
            works.arrivalDate        as  workArrivalDate,
            work_owners.login        as  workOwner,
            usergroups.label         as  workUsergroup,
            apps.name                as  appName,
            appTypes.appTypeName     as  appType,
            e_oses.osName            as  executableOsName,
            executables.osVersion    as  executableOsVersion,
            e_cpuTypes.cpuTypeName   as  executableCpuType,
            hosts.uid                as  hostUID,
            hosts.accessrights       as  hostAccessRights,
            hosts.name               as  hostName,
            hosts.project            as  hostProject,
            host_owners.login        as  hostOwner,
            h_oses.osName            as  hostOsName,
            h_cpuTypes.cpuTypeName   as  hostCpuType,
            datas.uid                as  dataUID,
            datas.name               as  dataName

-- ---------------------------------------------------------------------------
--  Tables joins for systematic matchmaking on works
-- ---------------------------------------------------------------------------
from        works
inner join  statuses                 on    statuses.statusId                 = works.statusId
inner join  apps                     on    apps.uid                          = works.appUID
inner join  appTypes                 on    appTypes.appTypeId                = apps.appTypeId
inner join  hosts                    on    ( ( works.expectedhostUID         = hosts.uid ) or 
                                             ( works.expectedhostUID        is NULL ) )

-- ---------------------------------------------------------------------------
--  Tables joins for matchmaking depending on application type
-- ---------------------------------------------------------------------------
left  join  sharedAppTypes           on    sharedAppTypes.appTypeId          = apps.appTypeId
left  join  executables              on    executables.appUID                = apps.uid
left  join  oses     as e_oses       on    e_oses.osId                       = executables.osId
left  join  cpuTypes as e_cpuTypes   on    e_cpuTypes.cpuTypeId              = executables.cpuTypeId

-- ---------------------------------------------------------------------------
--  Tables joins for systematic matchmaking on access rights
-- ---------------------------------------------------------------------------
inner join  users as work_owners     on    work_owners.uid                   = works.ownerUID
inner join  users as host_owners     on    host_owners.uid                   = hosts.ownerUID

-- ---------------------------------------------------------------------------
--  Tables joins for matchmaking on specific requirements
-- ---------------------------------------------------------------------------
left  join  usergroups               on    usergroups.uid                    = work_owners.usergroupUID
left  join  sharedPackageTypes       on    (sharedPackageTypes.packageTypeId = apps.packageTypeId) and
                                           (sharedPackageTypes.hostUID       = hosts.uid)

-- ---------------------------------------------------------------------------
--  Tables joins for human readable display
-- ---------------------------------------------------------------------------
left  join  oses     as h_oses       on    h_oses.osId                       = hosts.osId
left  join  cpuTypes as h_cpuTypes   on    h_cpuTypes.cpuTypeId              = hosts.cpuTypeId
left  join  datas                    on    datas.uid                         = executables.dataUID

where
-- ---------------------------------------------------------------------------
--  Selection criteria on works
-- ---------------------------------------------------------------------------
      ( statuses.statusName = 'PENDING' )                  and  --  Work status must be 'PENDING'

-- ---------------------------------------------------------------------------
--  Selection criteria on Shared applications      (OR)
-- ---------------------------------------------------------------------------
      (
        ( hosts.uid         = sharedAppTypes.hostUID ) or       --  Host must have the required Shared application

-- ---------------------------------------------------------------------------
--  Selection criteria on Deployable applications  (OR)
-- ---------------------------------------------------------------------------
        ( ( appTypes.appTypeName     = 'DEPLOYABLE' )    and    --  Application type must be 'DEPLOYABLE'

          ( ( executables.osId       = hosts.osId ) or          --  Executable OS name must match Host OS name, or be 'JAVA'
            ( e_oses.osName          = 'JAVA' ) )        and

          ( ( executables.osVersion  = hosts.osversion ) or     --  Executable OS version must match Host OS version, or be NULL
            ( executables.osVersion is null ) )          and

          ( ( executables.cpuTypeId  = hosts.cpuTypeId ) or     --  Executable CPU type must match Host CPU type, or be 'ALL'
            ( e_cpuTypes.cpuTypeName = 'ALL' ) ) )
      )                                                    and

-- ---------------------------------------------------------------------------
--  Selection criteria on access rights
-- ---------------------------------------------------------------------------
      ( (    hosts.ownerUID = works.ownerUID ) or               --  Work owner must match Host owner, or

        ( (  hosts.accessrights       & 5 = 5 ) and             --  Work and Host must both be 'rx' by others, or
          (  works.accessrights       & 5 = 5 ) ) or

        ( ( (hosts.accessrights >> 4) & 5 = 5 ) and             --  Work and Host must both be 'rx' by group members, and
          ( (works.accessrights >> 4) & 5 = 5 ) and

          ( ( host_owners.usergroupUID =
              work_owners.usergroupUID ) or                         --  Work usergroup must match Host usergroup, or

            ( host_owners.usergroupUID in                           --  One Work group membership must match Host usergroup
              ( select usergroupUID from memberships
                where  userUID = works.ownerUID ) ) ) ) )  and

-- ---------------------------------------------------------------------------
--  Selection criteria on specific requirements
-- ---------------------------------------------------------------------------
      ( ( apps.packageTypeId               is NULL ) or         --  Application needed package must be NULL
        ( sharedPackageTypes.packageTypeId is NOT NULL ) ) and  --  or in 'hosts.sharedpackages'

      ( ( hosts.project  is NULL ) or                           --  Host project must be NULL, '' or match Usergroup label
        ( hosts.project  = ''    ) or
        ( hosts.project  = usergroups.label ) )

-- ---------------------------------------------------------------------------
order by  workArrivalDate;
-- ---------------------------------------------------------------------------
