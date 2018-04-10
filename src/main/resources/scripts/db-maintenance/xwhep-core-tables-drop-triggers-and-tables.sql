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
--  XtremWeb-HEP Core Tables :
--  SQL script permitting, for an 9.x schema, to drop all triggers and tables
--
-- ===========================================================================
drop trigger if exists trig_tasks_history_update_status;
drop trigger if exists trig_tasks_history_insert_status;
drop trigger if exists trig_works_history_update_status;
drop trigger if exists trig_works_history_insert_status;
drop trigger if exists trig_apps_history_before_delete;
drop trigger if exists trig_apps_history_after_update;
drop trigger if exists trig_apps_history_before_update;
drop trigger if exists trig_apps_history_before_insert;
drop trigger if exists trig_apps_history_after_insert;
drop trigger if exists trig_datas_history_update_status_type_os_cpu;
drop trigger if exists trig_datas_history_insert_status_type_os_cpu;
drop trigger if exists trig_hosts_history_delete_shared;
drop trigger if exists trig_hosts_history_update_shared;
drop trigger if exists trig_hosts_history_insert_shared;
drop trigger if exists trig_hosts_history_update_os_cpu_project;
drop trigger if exists trig_hosts_history_insert_os_cpu_project;
drop trigger if exists trig_users_history_update_rights;
drop trigger if exists trig_users_history_insert_rights;

drop trigger if exists trig_tasks_update_status;
drop trigger if exists trig_tasks_insert_status;
drop trigger if exists trig_works_update_status;
drop trigger if exists trig_works_insert_status;
drop trigger if exists trig_apps_before_delete;
drop trigger if exists trig_apps_after_update;
drop trigger if exists trig_apps_before_update;
drop trigger if exists trig_apps_before_insert;
drop trigger if exists trig_apps_after_insert;
drop trigger if exists trig_datas_update_status_type_os_cpu;
drop trigger if exists trig_datas_insert_status_type_os_cpu;
drop trigger if exists trig_hosts_delete_shared;
drop trigger if exists trig_hosts_update_shared;
drop trigger if exists trig_hosts_insert_shared;
drop trigger if exists trig_hosts_update_os_cpu_project;
drop trigger if exists trig_hosts_insert_os_cpu_project;
drop trigger if exists trig_users_update_rights;
drop trigger if exists trig_users_insert_rights;

drop table if exists tasks_history;
drop table if exists works_history;
drop table if exists groups_history;
drop table if exists sessions_history;
drop table if exists executables_history;
drop table if exists apps_history;
drop table if exists datas_history;
drop table if exists sharedPackageTypes_history;
drop table if exists sharedAppTypes_history;
drop table if exists traces_history;
drop table if exists hosts_history;
drop table if exists memberships_history;
drop table if exists usergroups_history;
drop table if exists users_history;

drop table if exists tasks;
drop table if exists works;
drop table if exists groups;
drop table if exists sessions;
drop table if exists executables;
drop table if exists apps;
drop table if exists datas;
drop table if exists sharedPackageTypes;
drop table if exists sharedAppTypes;
drop table if exists traces;
drop table if exists hosts;
drop table if exists memberships;
set @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
drop table if exists usergroups;
drop table if exists users;
set FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
drop table if exists cpuTypes;
drop table if exists oses;
drop table if exists packageTypes;
drop table if exists appTypes;
drop table if exists dataTypes;
drop table if exists statuses;
drop table if exists userRights;
drop table if exists versions;
