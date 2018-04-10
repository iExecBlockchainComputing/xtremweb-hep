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
--  SQL script creating the foreign keys for history tables
--
--  Addition of foreign keys requires that :
--      - Each referenced table  must already exist and be managed by InnoDB
--      - Each referenced column must already exist and contain initial data
--
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- Table "users_history"
-- ---------------------------------------------------------------------------
alter table  users_history
  add   constraint   fk_users_history_userRights
        foreign key  fk_users_history_userRights_idx (userRightId)
        references                    userRights     (userRightId)
        on delete cascade  on update restrict,
  add   constraint   fk_users_history_usergroups_history
        foreign key  fk_users_history_usergroups_history_idx (usergroupUID)
        references                    usergroups_history              (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_users_history_users_history
        foreign key  fk_users_history_users_history_idx (owneruid)
        references                    users_history          (uid)
        on delete cascade  on update restrict,
  drop  index        userRightId,
  drop  index        usergroupUID,
  drop  index        ownerUID,
  engine = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "usergroups_history"
-- ---------------------------------------------------------------------------
alter table  usergroups_history
  add   constraint   fk_usergroups_history_users_history
        foreign key  fk_usergroups_history_users_history_idx (ownerUID)
        references                         users_history          (uid)
        on delete cascade  on update restrict,
  drop  index        ownerUID,
  engine = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "memberships_history"
-- ---------------------------------------------------------------------------
alter table  memberships_history
  add   constraint   fk_memberships_history_users_history
        foreign key  fk_memberships_history_users_history_idx (userUID)
        references                          users_history         (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_memberships_history_usergroups_history
        foreign key  fk_memberships_history_usergroups_history_idx (usergroupUID)
        references                          usergroups_history              (uid)
        on delete cascade  on update restrict,
  drop  index        userUID,
  drop  index        usergroupUID,
  engine  = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "hosts_history"
-- ---------------------------------------------------------------------------
alter table  hosts_history
  add   constraint   fk_hosts_history_oses
        foreign key  fk_hosts_history_oses_idx (osId)
        references                    oses     (osId)
        on delete cascade  on update restrict,
  add   constraint   fk_hosts_history_cpuTypes
        foreign key  fk_hosts_history_cpuTypes_idx (cpuTypeId)
        references                    cpuTypes     (cpuTypeId)
        on delete cascade  on update restrict,
  add   constraint   fk_hosts_history_usergroups_history
        foreign key  fk_hosts_history_usergroups_history_idx (usergroupUID)
        references                    usergroups                      (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_hosts_history_users_history
        foreign key  fk_hosts_history_users_history_idx (ownerUID)
        references                    users_history          (uid)
        on delete cascade  on update restrict,
  drop  index        osId,
  drop  index        cpuTypeId,
  drop  index        usergroupUID,
  drop  index        ownerUID,
  engine = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "traces_history"
-- ---------------------------------------------------------------------------
alter table  traces_history
  add   constraint   fk_traces_history_hosts_history
        foreign key  fk_traces_history_hosts_history_idx (hostUID)
        references                     hosts_history         (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_traces_history_users_history
        foreign key  fk_traces_history_users_history_idx (ownerUID)
        references                     users_history          (uid)
        on delete cascade  on update restrict,
  drop  index        hostUID,
  drop  index        ownerUID,
  engine = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "sharedAppTypes_history"
-- ---------------------------------------------------------------------------
alter table  sharedAppTypes_history
  add   constraint   fk_sharedAppTypes_history_hosts_history
        foreign key  fk_sharedAppTypes_history_hosts_history_idx (hostUID)
        references                             hosts_history         (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_sharedAppTypes_history_appTypes
        foreign key  fk_sharedAppTypes_history_appTypes_idx (appTypeId)
        references                             appTypes     (appTypeId)
        on delete cascade  on update restrict,
  drop  index        hostUID,
  drop  index        appTypeId,
  engine = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "sharedPackageTypes_history"
-- ---------------------------------------------------------------------------
alter table  sharedPackageTypes
  add   constraint   fk_sharedPackageTypes_history_hosts_history
        foreign key  fk_sharedPackageTypes_history_hosts_history_idx (hostUID)
        references                                 hosts                 (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_sharedPackageTypes_history_packageTypes
        foreign key  fk_sharedPackageTypes_history_packageTypes_idx (packageTypeId)
        references                                 packageTypes     (packageTypeId)
        on delete cascade  on update restrict,
  drop  index        hostUID,
  drop  index        packageTypeId,
  engine = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "datas_history"
-- ---------------------------------------------------------------------------
alter table  datas_history
  add   constraint   fk_datas_history_statuses
        foreign key  fk_datas_history_statuses_idx (statusId)
        references                    statuses     (statusId)
        on delete cascade  on update restrict,
  add   constraint   fk_datas_history_types
        foreign key  fk_datas_history_types_idx (dataTypeId)
        references                    dataTypes (dataTypeId)
        on delete cascade  on update restrict,
  add   constraint   fk_datas_history_oses
        foreign key  fk_datas_history_oses_idx (osId)
        references                    oses     (osId)
        on delete cascade  on update restrict,
  add   constraint   fk_datas_history_cpuTypes
        foreign key  fk_datas_history_cpuTypes_idx (cpuTypeId)
        references                    cpuTypes     (cpuTypeId)
        on delete cascade  on update restrict,
  add   constraint   fk_datas_history_users_history
        foreign key  fk_datas_history_users_history_idx (ownerUID)
        references                    users_history          (uid)
        on delete cascade  on update restrict,
  drop  index        statusId,
  drop  index        dataTypeId,
  drop  index        osId,
  drop  index        cpuTypeId,
  drop  index        ownerUID, 
  engine = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "apps_history"
-- ---------------------------------------------------------------------------
alter table  apps_history
  add   constraint   fk_apps_history_appTypes
        foreign key  fk_apps_history_appTypes_idx (appTypeId)
        references                   appTypes     (appTypeId)
        on delete cascade  on update restrict,
  add   constraint   fk_apps_history_packageTypes
        foreign key  fk_apps_history_packageTypes_idx (packageTypeId)
        references                   packageTypes     (packageTypeId)
        on delete cascade  on update restrict,
  add   constraint   fk_apps_history_users_history
        foreign key  fk_apps_history_users_history_idx (ownerUID)
        references                   users_history          (uid)
        on delete cascade  on update restrict,
  drop  index        appTypeId,
  drop  index        packageTypeId,
  drop  index        ownerUID,
  engine  = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "executables_history"
-- ---------------------------------------------------------------------------
alter table  executables_history
  add   constraint   fk_executables_history_apps_history
        foreign key  fk_executables_history_apps_history_idx (appUID)
        references                          apps_history        (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_executables_history_dataTypes
        foreign key  fk_executables_history_dataTypes_idx (dataTypeId)
        references                          dataTypes     (dataTypeId)
        on delete cascade  on update restrict,
  add   constraint   fk_executables_history_oses
        foreign key  fk_executables_history_oses_idx (osId)
        references                          oses     (osId)
        on delete cascade  on update restrict,
  add   constraint   fk_executables_history_cpuTypes
        foreign key  fk_executables_history_cpuTypes_idx (cpuTypeId)
        references                          cpuTypes     (cpuTypeId)
        on delete cascade  on update restrict,
  add   constraint   fk_executables_history_datas_history
        foreign key  fk_executables_history_datas_history_idx (dataUID)
        references                          datas_history         (uid)
        on delete cascade  on update restrict,
  drop  index        appUID,
  drop  index        dataTypeId,
  drop  index        osId,
  drop  index        cpuTypeId,
  drop  index        dataUID,
engine = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "sessions_history"
-- ---------------------------------------------------------------------------
alter table  sessions_history
  add   constraint   fk_sessions_history_users_history
        foreign key  fk_sessions_history_users_history_idx (ownerUID)
        references                       users_history          (uid)
        on delete cascade  on update restrict,
  drop  index        ownerUID,
  engine  = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "groups_history"
-- ---------------------------------------------------------------------------
alter table  groups_history
  add   constraint   fk_groups_history_sessions_history
        foreign key  fk_groups_history_sessions_history_idx (sessionUID)
        references                     sessions_history            (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_groups_history_users_history
        foreign key  fk_groups_history_users_history_idx (ownerUID)
        references                     users_history          (uid)
        on delete cascade  on update restrict,
  drop  index        sessionUID,
  drop  index        ownerUID,
  engine  = InnoDB;

show warnings;

-- ---------------------------------------------------------------------------
-- Table "works_history"
-- ---------------------------------------------------------------------------
alter table  works_history
  add   constraint   fk_works_history_apps_history
        foreign key  fk_works_history_apps_history_idx (appUID)
        references                    apps_history        (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_works_history_statuses
        foreign key  fk_works_history_statuses_idx (statusId)
        references                    statuses     (statusId)
        on delete cascade  on update restrict,
  add   constraint   fk_works_history_sessions_history
        foreign key  fk_works_history_sessions_history_idx (sessionUID)
        references                    sessions_history            (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_works_history_groups_history
        foreign key  fk_works_history_groups_history_idx (groupUID)
        references                    groups_history          (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_works_history_hosts_history
        foreign key  fk_works_history_hosts_history_idx (expectedhostUID)
        references                    hosts_history                 (uid)
        on delete set null  on update restrict,
  add   constraint   fk_works_history_users_history
        foreign key  fk_works_history_users_history_idx (ownerUID)
        references                    users_history          (uid)
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
-- Table "tasks_history"
-- ---------------------------------------------------------------------------
alter table  tasks_history
  add   constraint   fk_tasks_history_works_history
        foreign key  fk_tasks_history_works_history_idx (workUID)
        references                    works_history         (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_tasks_history_hosts_history
        foreign key  fk_tasks_history_hosts_history_idx (hostUID)
        references                    hosts_history         (uid)
        on delete cascade  on update restrict,
  add   constraint   fk_tasks_history_statuses
        foreign key  fk_tasks_history_statuses_idx (statusId)
        references                    statuses     (statusId)
        on delete cascade  on update restrict,
  add   constraint   fk_tasks_history_users_history
        foreign key  fk_tasks_history_users_history_idx (ownerUID)
        references                    users_history          (uid)
        on delete cascade  on update restrict,
  drop  index        workUID,
  drop  index        hostUID,
  drop  index        statusId,
  drop  index        ownerUID,
  engine  = InnoDB;

show warnings;
