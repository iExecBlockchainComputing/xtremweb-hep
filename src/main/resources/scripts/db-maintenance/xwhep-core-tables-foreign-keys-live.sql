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
--  SQL script creating the foreign keys for live tables
--
--  Addition of foreign keys requires that :
--      - Each referenced table  must already exist and be managed by InnoDB
--      - Each referenced column must already exist and contain initial data
--
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- Table "users"
-- ---------------------------------------------------------------------------
alter table  users
  add   constraint   fk_users_userRights
        foreign key  fk_users_userRights_idx (userRightId)
        references            userRights     (userRightId)
        on delete cascade  on update restrict,
  add   constraint   fk_users_usergroups
        foreign key  fk_users_usergroups_idx (usergroupUID)
        references            usergroups              (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_users_users
        foreign key  fk_users_users_idx (owneruid)
        references            users          (uid)
        on delete cascade  on update restrict,
  drop  index        userRightId,
  drop  index        usergroupUID,
  drop  index        ownerUID,
  engine = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "usergroups"
-- ---------------------------------------------------------------------------
alter table  usergroups
  add   constraint   fk_usergroups_users
        foreign key  fk_usergroups_users_idx (ownerUID)
        references                 users          (uid)
        on delete cascade  on update restrict,
  drop  index        ownerUID,
  engine = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "memberships"
-- ---------------------------------------------------------------------------
alter table  memberships
  add   constraint   fk_memberships_users
        foreign key  fk_memberships_users_idx (userUID)
        references                  users         (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_memberships_usergroups
        foreign key  fk_memberships_usergroups_idx (usergroupUID)
        references                  usergroups              (uid)
        on delete cascade  on update restrict,
  drop  index        userUID,
  drop  index        usergroupUID,
  engine  = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "hosts"
-- ---------------------------------------------------------------------------
alter table  hosts
  add   constraint   fk_hosts_oses
        foreign key  fk_hosts_oses_idx (osId)
        references            oses     (osId)
        on delete cascade  on update restrict,
  add   constraint   fk_hosts_cpuTypes
        foreign key  fk_hosts_cpuTypes_idx (cpuTypeId)
        references            cpuTypes     (cpuTypeId)
        on delete cascade  on update restrict,
  add   constraint   fk_hosts_usergroups
        foreign key  fk_hosts_usergroups_idx (usergroupUID)
        references            usergroups              (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_hosts_users
        foreign key  fk_hosts_users_idx (ownerUID)
        references            users          (uid)
        on delete cascade  on update restrict,
  drop  index        osId,
  drop  index        cpuTypeId,
  drop  index        usergroupUID,
  drop  index        ownerUID,
  engine = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "traces"
-- ---------------------------------------------------------------------------
alter table  traces
  add   constraint   fk_traces_hosts
        foreign key  fk_traces_hosts_idx (hostUID)
        references             hosts         (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_traces_users
        foreign key  fk_traces_users_idx (ownerUID)
        references             users          (uid)
        on delete cascade  on update restrict,
  drop  index        hostUID,
  drop  index        ownerUID,
  engine = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "sharedAppTypes"
-- ---------------------------------------------------------------------------
alter table  sharedAppTypes
  add   constraint   fk_sharedAppTypes_hosts
        foreign key  fk_sharedAppTypes_hosts_idx (hostUID)
        references                     hosts         (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_sharedAppTypes_appTypes
        foreign key  fk_sharedAppTypes_appTypes_idx (appTypeId)
        references                     appTypes     (appTypeId)
        on delete cascade  on update restrict,
  drop  index        hostUID,
  drop  index        appTypeId,
  engine = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "sharedPackageTypes"
-- ---------------------------------------------------------------------------
alter table  sharedPackageTypes
  add   constraint   fk_sharedPackageTypes_hosts
        foreign key  fk_sharedPackageTypes_hosts_idx (hostUID)
        references                         hosts         (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_sharedPackageTypes_packageTypes
        foreign key  fk_sharedPackageTypes_packageTypes_idx (packageTypeId)
        references                         packageTypes     (packageTypeId)
        on delete cascade  on update restrict,
  drop  index        hostUID,
  drop  index        packageTypeId,
  engine = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "datas"
-- ---------------------------------------------------------------------------
alter table  datas
  add   constraint   fk_datas_statuses
        foreign key  fk_datas_statuses_idx (statusId)
        references            statuses     (statusId)
        on delete cascade  on update restrict,
  add   constraint   fk_datas_types
        foreign key  fk_datas_types_idx (dataTypeId)
        references            dataTypes (dataTypeId)
        on delete cascade  on update restrict,
  add   constraint   fk_datas_oses
        foreign key  fk_datas_oses_idx (osId)
        references            oses     (osId)
        on delete cascade  on update restrict,
  add   constraint   fk_datas_cpuTypes
        foreign key  fk_datas_cpuTypes_idx (cpuTypeId)
        references            cpuTypes     (cpuTypeId)
        on delete cascade  on update restrict,
  add   constraint   fk_datas_users
        foreign key  fk_datas_users_idx (ownerUID)
        references            users          (uid)
        on delete cascade  on update restrict,
  drop  index        statusId,
  drop  index        dataTypeId,
  drop  index        osId,
  drop  index        cpuTypeId,
  drop  index        ownerUID, 
  engine = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "apps"
-- ---------------------------------------------------------------------------
alter table  apps
  add   constraint   fk_apps_appTypes
        foreign key  fk_apps_appTypes_idx (appTypeId)
        references           appTypes     (appTypeId)
        on delete cascade  on update restrict,
  add   constraint   fk_apps_packageTypes
        foreign key  fk_apps_packageTypes_idx (packageTypeId)
        references           packageTypes     (packageTypeId)
        on delete cascade  on update restrict,
  add   constraint   fk_apps_users
        foreign key  fk_apps_users_idx (ownerUID)
        references           users          (uid)
        on delete cascade  on update restrict,
  drop  index        appTypeId,
  drop  index        packageTypeId,
  drop  index        ownerUID,
  engine  = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "executables"
-- ---------------------------------------------------------------------------
alter table  executables
  add   constraint   fk_executables_apps
        foreign key  fk_executables_apps_idx (appUID)
        references                  apps        (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_executables_dataTypes
        foreign key  fk_executables_dataTypes_idx (dataTypeId)
        references                  dataTypes     (dataTypeId)
        on delete cascade  on update restrict,
  add   constraint   fk_executables_oses
        foreign key  fk_executables_oses_idx (osId)
        references                  oses     (osId)
        on delete cascade  on update restrict,
  add   constraint   fk_executables_cpuTypes
        foreign key  fk_executables_cpuTypes_idx (cpuTypeId)
        references                  cpuTypes     (cpuTypeId)
        on delete cascade  on update restrict,
  add   constraint   fk_executables_datas
        foreign key  fk_executables_datas_idx (dataUID)
        references                  datas         (uid)
        on delete cascade  on update restrict,
  drop  index        appUID,
  drop  index        dataTypeId,
  drop  index        osId,
  drop  index        cpuTypeId,
  drop  index        dataUID,
engine = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "sessions"
-- ---------------------------------------------------------------------------
alter table  sessions
  add   constraint   fk_sessions_users
        foreign key  fk_sessions_users_idx (ownerUID)
        references              users          (uid)
        on delete cascade  on update restrict,
  drop  index        ownerUID,
  engine  = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "groups"
-- ---------------------------------------------------------------------------
alter table  groups
  add   constraint   fk_groups_sessions
        foreign key  fk_groups_sessions_idx (sessionUID)
        references             sessions            (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_groups_users
        foreign key  fk_groups_users_idx (ownerUID)
        references             users          (uid)
        on delete cascade  on update restrict,
  drop  index        sessionUID,
  drop  index        ownerUID,
  engine  = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "works"
-- ---------------------------------------------------------------------------
alter table  works
  add   constraint   fk_works_apps
        foreign key  fk_works_apps_idx (appUID)
        references            apps        (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_works_statuses
        foreign key  fk_works_statuses_idx (statusId)
        references            statuses     (statusId)
        on delete cascade  on update restrict,
  add   constraint   fk_works_sessions
        foreign key  fk_works_sessions_idx (sessionUID)
        references            sessions            (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_works_groups
        foreign key  fk_works_groups_idx (groupUID)
        references            groups          (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_works_hosts
        foreign key  fk_works_hosts_idx (expectedhostUID)
        references            hosts                 (uid)
        on delete set null  on update restrict,
  add   constraint   fk_works_users
        foreign key  fk_works_users_idx (ownerUID)
        references            users          (uid)
        on delete cascade  on update restrict,
  drop  index        appUID,
  drop  index        statusId,
  drop  index        sessionUID,
  drop  index        groupUID,
  drop  index        expectedhostUID,
  drop  index        ownerUID,
  engine  = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "tasks"
-- ---------------------------------------------------------------------------
alter table  tasks
  add   constraint   fk_tasks_works
        foreign key  fk_tasks_works_idx (workUID)
        references            works         (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_tasks_hosts
        foreign key  fk_tasks_hosts_idx (hostUID)
        references            hosts         (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_tasks_statuses
        foreign key  fk_tasks_statuses_idx (statusId)
        references            statuses     (statusId)
        on delete cascade  on update restrict,
  add   constraint   fk_tasks_users
        foreign key  fk_tasks_users_idx (ownerUID)
        references            users          (uid)
        on delete cascade  on update restrict,
  drop  index        workUID,
  drop  index        hostUID,
  drop  index        statusId,
  drop  index        ownerUID,
  engine  = InnoDB;

show warnings;
