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
--  SQL script permitting, from an 8.x schema :
--      -  to set the DB engine to 'InnoDB' for each table
--      -  to create new tables, columns, foreign keys and indexes
-- 
--  Addition of foreign keys requires that :
--      - Each referenced table  must already exist and be managed by InnoDB
--      - Each referenced column must already exist and contain initial data
-- 
-- ===========================================================================


-- ---------------------------------------------------------------------------
select 'Old table "versions"' as '';
-- Set the DB engine to 'InnoDB'
-- ---------------------------------------------------------------------------
alter table  versions
  modify column  version  varchar(254),
  engine  = InnoDB,
  comment = 'versions = Timestamps of XtremWeb-HEP versions';

show warnings;


-- ===========================================================================
-- 
-- Tables containing constant data
-- 
-- ===========================================================================

-- ---------------------------------------------------------------------------
select 'New table "userRights"' as '';
-- Constants for "users"."rights"
-- ---------------------------------------------------------------------------
create table if not exists  userRights  (
  userRightId           tinyint unsigned  not null  primary key,
  userRightName         varchar(254)      not null  unique,
  mtime                 timestamp,
  userRightDescription  varchar(254)
  )
engine  = InnoDB,
comment = 'userRights = Constants for "users"."rights"';

show warnings;


-- ---------------------------------------------------------------------------
select 'New table "statuses"' as '';
-- Constants for *."status"
-- ---------------------------------------------------------------------------
create table if not exists  statuses  (
  statusId          tinyint unsigned  not null  primary key,
  statusName        varchar(36)       not null  unique,
  mtime             timestamp,
  statusObjects     varchar(254)      not null,
  statusComment     varchar(254),
  statusDeprecated  varchar(254)
  )
engine  = InnoDB,
comment = 'statuses = Constants for *."status"';

show warnings;


-- ---------------------------------------------------------------------------
select 'New table "dataTypes"' as '';
-- Constants for "datas"."type"
-- ---------------------------------------------------------------------------
create table if not exists  dataTypes  (
  dataTypeId           tinyint unsigned  not null  primary key,
  dataTypeName         varchar(254)      not null  unique,
  mtime                timestamp,
  dataTypeDescription  varchar(254)
  )
engine  = InnoDB,
comment = 'dataTypes = Constants for "datas"."type"';

show warnings;


-- ---------------------------------------------------------------------------
select 'New table "appTypes"' as '';
-- Constants for "apps"."type"
-- ---------------------------------------------------------------------------
create table if not exists  appTypes  (
  appTypeId           tinyint unsigned  not null  primary key,
  appTypeName         varchar(254)      not null  unique,
  mtime               timestamp,
  appTypeDescription  varchar(254)
  )
engine  = InnoDB,
comment = 'appTypes = Constants for "apps"."type"';

show warnings;


-- ---------------------------------------------------------------------------
select 'New table "packageTypes"' as '';
-- Constants for "apps"."neededpackages"
-- ---------------------------------------------------------------------------
create table if not exists  packageTypes  (
  packageTypeId           tinyint unsigned  not null  primary key,
  packageTypeName         varchar(254)      not null  unique,
  mtime                   timestamp,
  packageTypeDescription  varchar(254)
  )
engine  = InnoDB,
comment = 'appTypes = Constants for "apps"."neededpackages"';

show warnings;


-- ---------------------------------------------------------------------------
select 'New table "oses" (Operating Systems)' as '';
-- Constants for *."os"
-- ---------------------------------------------------------------------------
create table if not exists  oses  (
  osId           tinyint unsigned  not null  primary key,
  osName         char(7)           not null  unique,
  mtime          timestamp,
  osDescription  varchar(254)
  )
engine  = InnoDB,
comment = 'oses = Constants for *."os"';

show warnings;


-- ---------------------------------------------------------------------------
select 'New table "cpuTypes"' as '';
-- Constants for *."cpuType"
-- ---------------------------------------------------------------------------
create table if not exists  cpuTypes  (
  cpuTypeId           tinyint unsigned  not null  primary key,
  cpuTypeName         char(7)           not null  unique,
  mtime               timestamp,
  cpuTypeDescription  varchar(254)
  )
engine  = InnoDB,
comment = 'cpuTypes = Constants for *."cpuType"';

show warnings;


-- ===========================================================================
-- 
select 'Initial data for tables containing constant data' as '';
-- 
-- ===========================================================================

start transaction;
-- ---------------------------------------------------------------------------
-- Data for table "userRights"
-- ---------------------------------------------------------------------------
insert into userRights (userRightId, userRightName, userRightDescription) values ( 0, 'NONE',            null);
insert into userRights (userRightId, userRightName, userRightDescription) values ( 1, 'LISTJOB',         null);
insert into userRights (userRightId, userRightName, userRightDescription) values ( 2, 'INSERTJOB',       null);
insert into userRights (userRightId, userRightName, userRightDescription) values ( 3, 'GETJOB',          null);
insert into userRights (userRightId, userRightName, userRightDescription) values ( 4, 'DELETEJOB',       null);
insert into userRights (userRightId, userRightName, userRightDescription) values ( 5, 'LISTDATA',        null);
insert into userRights (userRightId, userRightName, userRightDescription) values ( 6, 'INSERTDATA',      null);
insert into userRights (userRightId, userRightName, userRightDescription) values ( 7, 'GETDATA',         null);
insert into userRights (userRightId, userRightName, userRightDescription) values ( 8, 'DELETEDATA',      null);
insert into userRights (userRightId, userRightName, userRightDescription) values ( 9, 'LISTGROUP',       null);
insert into userRights (userRightId, userRightName, userRightDescription) values (10, 'INSERTGROUP',     null);
insert into userRights (userRightId, userRightName, userRightDescription) values (11, 'GETGROUP',        null);
insert into userRights (userRightId, userRightName, userRightDescription) values (12, 'DELETEGROUP',     null);
insert into userRights (userRightId, userRightName, userRightDescription) values (13, 'LISTSESSION',     null);
insert into userRights (userRightId, userRightName, userRightDescription) values (14, 'INSERTSESSION',   null);
insert into userRights (userRightId, userRightName, userRightDescription) values (15, 'GETSESSION',      null);
insert into userRights (userRightId, userRightName, userRightDescription) values (16, 'DELETESESSION',   null);
insert into userRights (userRightId, userRightName, userRightDescription) values (17, 'LISTHOST',        null);
insert into userRights (userRightId, userRightName, userRightDescription) values (18, 'GETHOST',         null);
insert into userRights (userRightId, userRightName, userRightDescription) values (19, 'LISTUSER',        null);
insert into userRights (userRightId, userRightName, userRightDescription) values (20, 'GETUSER',         null);
insert into userRights (userRightId, userRightName, userRightDescription) values (21, 'LISTUSERGROUP',   null);
insert into userRights (userRightId, userRightName, userRightDescription) values (22, 'GETUSERGROUP',    null);
insert into userRights (userRightId, userRightName, userRightDescription) values (23, 'LISTAPP',         null);
insert into userRights (userRightId, userRightName, userRightDescription) values (24, 'GETAPP',          null);
insert into userRights (userRightId, userRightName, userRightDescription) values (25, 'STANDARD_USER',   null);
insert into userRights (userRightId, userRightName, userRightDescription) values (26, 'UPDATEWORK',      null);
insert into userRights (userRightId, userRightName, userRightDescription) values (27, 'WORKER_USER',     null);
insert into userRights (userRightId, userRightName, userRightDescription) values (28, 'INSERTUSER',      null);
insert into userRights (userRightId, userRightName, userRightDescription) values (29, 'DELETEUSER',      null);
insert into userRights (userRightId, userRightName, userRightDescription) values (30, 'INSERTAPP',       null);
insert into userRights (userRightId, userRightName, userRightDescription) values (31, 'DELETEAPP',       null);
insert into userRights (userRightId, userRightName, userRightDescription) values (32, 'ADVANCED_USER',   null);
insert into userRights (userRightId, userRightName, userRightDescription) values (33, 'DELEGATION_USER', null);
insert into userRights (userRightId, userRightName, userRightDescription) values (34, 'INSERTHOST',      null);
insert into userRights (userRightId, userRightName, userRightDescription) values (35, 'DELETEHOST',      null);
insert into userRights (userRightId, userRightName, userRightDescription) values (36, 'INSERTUSERGROUP', null);
insert into userRights (userRightId, userRightName, userRightDescription) values (37, 'DELETEUSERGROUP', null);
insert into userRights (userRightId, userRightName, userRightDescription) values (38, 'SUPER_USER',      null);

-- ---------------------------------------------------------------------------
-- Data for table "statuses"
-- ---------------------------------------------------------------------------
insert into statuses (statusId, statusName, statusObjects, statusComment, statusDeprecated) values ( 0, 'NONE',          'none',                null,                                                                                null);
insert into statuses (statusId, statusName, statusObjects, statusComment, statusDeprecated) values ( 1, 'ANY',           'any',                 null,                                                                                null);
insert into statuses (statusId, statusName, statusObjects, statusComment, statusDeprecated) values ( 2, 'WAITING',       'works',               'The object is stored on server but not in the server queue yet',                    null);
insert into statuses (statusId, statusName, statusObjects, statusComment, statusDeprecated) values ( 3, 'PENDING',       'works, tasks',        'The object is stored and inserted in the server queue',                             null);
insert into statuses (statusId, statusName, statusObjects, statusComment, statusDeprecated) values ( 4, 'RUNNING',       'works, tasks',        'The object is being run by a worker',                                               null);
insert into statuses (statusId, statusName, statusObjects, statusComment, statusDeprecated) values ( 5, 'ERROR',         'any',                 'The object is erroneous',                                                           null);
insert into statuses (statusId, statusName, statusObjects, statusComment, statusDeprecated) values ( 6, 'COMPLETED',     'works, tasks',        'The job has been successfully computed',                                            null);
insert into statuses (statusId, statusName, statusObjects, statusComment, statusDeprecated) values ( 7, 'ABORTED',       'works, tasks',        'NOT used anymore', 'Since XWHEP, aborted objects are set to PENDING');
insert into statuses (statusId, statusName, statusObjects, statusComment, statusDeprecated) values ( 8, 'LOST',          'works, tasks',        'NOT used anymore', 'Since XWHEP, lost objects are set to PENDING');
insert into statuses (statusId, statusName, statusObjects, statusComment, statusDeprecated) values ( 9, 'DATAREQUEST',   'datas, works, tasks', 'The server is unable to store the uploaded object. Waiting for another upload try', null);
insert into statuses (statusId, statusName, statusObjects, statusComment, statusDeprecated) values (10, 'RESULTREQUEST', 'works',               'The worker should retry to upload the results',                                     null);
insert into statuses (statusId, statusName, statusObjects, statusComment, statusDeprecated) values (11, 'AVAILABLE',     'datas',               'The data is available and can be downloaded on demand',                             null);
insert into statuses (statusId, statusName, statusObjects, statusComment, statusDeprecated) values (12, 'UNAVAILABLE',   'datas',               'The data is not available and can not be downloaded on demand',                     null);
insert into statuses (statusId, statusName, statusObjects, statusComment, statusDeprecated) values (13, 'REPLICATING',   'works',               'The object is being replicated',                                                   null);

-- ---------------------------------------------------------------------------
-- Data for table "dataTypes"
-- ---------------------------------------------------------------------------
insert into dataTypes (dataTypeId, dataTypeName, dataTypeDescription) values ( 0, 'NONE',           'Unknown data type');
insert into dataTypes (dataTypeId, dataTypeName, dataTypeDescription) values ( 1, 'BINARY',         'Binary data');
insert into dataTypes (dataTypeId, dataTypeName, dataTypeDescription) values ( 2, 'LIBRARY',        'Library data');
insert into dataTypes (dataTypeId, dataTypeName, dataTypeDescription) values ( 3, 'JAVA',           'Java byte code');
insert into dataTypes (dataTypeId, dataTypeName, dataTypeDescription) values ( 4, 'TEXT',           'Text data');
insert into dataTypes (dataTypeId, dataTypeName, dataTypeDescription) values ( 5, 'ZIP',            'Compressed data');
insert into dataTypes (dataTypeId, dataTypeName, dataTypeDescription) values ( 6, 'X509',           'X509 certificate');
insert into dataTypes (dataTypeId, dataTypeName, dataTypeDescription) values ( 7, 'URIPASSTHROUGH', 'Text file containing a list of uri, one per line');
insert into dataTypes (dataTypeId, dataTypeName, dataTypeDescription) values ( 8, 'UDPPACKET',      'Data that should be send using udp protocol');
insert into dataTypes (dataTypeId, dataTypeName, dataTypeDescription) values ( 9, 'STREAM',         'Data that should be send using tcp protocol');
insert into dataTypes (dataTypeId, dataTypeName, dataTypeDescription) values (10, 'ISO',            'Disk image');
insert into dataTypes (dataTypeId, dataTypeName, dataTypeDescription) values (11, 'VMDK',           'Virtual machine disk');
insert into dataTypes (dataTypeId, dataTypeName, dataTypeDescription) values (12, 'VDI',            'Virtual disk image');
insert into dataTypes (dataTypeId, dataTypeName, dataTypeDescription) values (13, 'SH',             'SH script');
insert into dataTypes (dataTypeId, dataTypeName, dataTypeDescription) values (14, 'BAT',            'CMD script');

-- ---------------------------------------------------------------------------
-- Data for table "appTypes"
-- ---------------------------------------------------------------------------
insert into appTypes (appTypeId, appTypeName, appTypeDescription) values (0, 'NONE',       'Unknown application type.');
insert into appTypes (appTypeId, appTypeName, appTypeDescription) values (1, 'DEPLOYABLE', 'Type for an application that must be deployed :  Its binary must be downloaded by volunteer resources.');
insert into appTypes (appTypeId, appTypeName, appTypeDescription) values (2, 'SHARED',     'Type for an application that is shared by volunteer resources :  Its binary should not be downloaded by volunteer resources.');
insert into appTypes (appTypeId, appTypeName, appTypeDescription) values (3, 'VIRTUALBOX', 'Type for a VirtualBox image of a virtual machine');
insert into appTypes (appTypeId, appTypeName, appTypeDescription) values (4, 'DOCKER',     'Type for a Docker image of a container');

-- ---------------------------------------------------------------------------
-- Data for table "packageTypes"
-- ---------------------------------------------------------------------------
insert into packageTypes (packageTypeId, packageTypeName, packageTypeDescription) values (0, 'NONE',   'Unknown package type');
insert into packageTypes (packageTypeId, packageTypeName, packageTypeDescription) values (1, 'GEANT4', 'Package for High Energy Physics');
insert into packageTypes (packageTypeId, packageTypeName, packageTypeDescription) values (2, 'ROOT',   'Package for High Energy Physics');

-- ---------------------------------------------------------------------------
-- Data for table "oses"
-- ---------------------------------------------------------------------------
insert into oses (osId, osName, osDescription) values (0, 'NONE',    'Unknown');
insert into oses (osId, osName, osDescription) values (1, 'ANDROID', 'Android');
insert into oses (osId, osName, osDescription) values (2, 'JAVA',    'Java Virtual Machine');
insert into oses (osId, osName, osDescription) values (3, 'LINUX',   'Linux');
insert into oses (osId, osName, osDescription) values (4, 'MACOSX',  'MacOS-X ');
insert into oses (osId, osName, osDescription) values (5, 'WIN32',   'MS-Windows 32 bits');
insert into oses (osId, osName, osDescription) values (6, 'WIN64',   'MS-Windows 64 bits');

-- ---------------------------------------------------------------------------
-- Data for table "cpuTypes"
-- ---------------------------------------------------------------------------
insert into cpuTypes (cpuTypeId, cpuTypeName, cpuTypeDescription) values (0, 'NONE',   'Unknown');
insert into cpuTypes (cpuTypeId, cpuTypeName, cpuTypeDescription) values (1, 'ALL',    'Architecture independant');
insert into cpuTypes (cpuTypeId, cpuTypeName, cpuTypeDescription) values (2, 'AMD64',  'AMD - 64 bits');
insert into cpuTypes (cpuTypeId, cpuTypeName, cpuTypeDescription) values (3, 'ARM',    'Advanced RISC Machines');
insert into cpuTypes (cpuTypeId, cpuTypeName, cpuTypeDescription) values (4, 'IA64',   'Intel Itanium - 64 bits');
insert into cpuTypes (cpuTypeId, cpuTypeName, cpuTypeDescription) values (5, 'IX86',   'Intel x86 - 32 bits');
insert into cpuTypes (cpuTypeId, cpuTypeName, cpuTypeDescription) values (6, 'PPC',    'Power PC');
insert into cpuTypes (cpuTypeId, cpuTypeName, cpuTypeDescription) values (7, 'X86_64', 'Intel x86 - 64 bits');

commit;


-- ===========================================================================
-- 
-- Tables referencing :
--        - Tables containing constant data
--        - Other tables
-- 
-- ===========================================================================

-- ---------------------------------------------------------------------------
select 'Old table "users"' as '';
-- Add columns  - "userRightId" :  Foreign key to "userRights"."userRightId"
--              - "errorMsg"
-- Add foreign key  to table  "userRights"
-- The foreign keys to tables "usergroups" and "users" have to be created AFTER
-- both tables are managed by InnoDB
-- ---------------------------------------------------------------------------
alter table  users
  modify column  uid           char(36)      not null,
  modify column  login         varchar(254)  not null  unique          after uid,
  add    column  userRightId   tinyint unsigned                        after login,
  modify column  rights        varchar(254)  not null  default 'NONE'  after userRightId,
  modify column  usergroupUID  char(36)                                after rights,
  modify column  ownerUID      char(36)      not null                  after usergroupUID,
  add    column  mtime         timestamp                               after ownerUID,
  modify column  certificate   varchar(32760),
  modify column  password      varchar(254)  not null  default '',
  modify column  email         varchar(254)  not null  default '',
  modify column  fname         varchar(254),
  modify column  lname         varchar(254),
  modify column  country       varchar(254),
  add    column  errorMsg      varchar(254),
  add    constraint    fk_users_userRights
         foreign key   fk_users_userRights_idx (userRightId)
         references             userRights     (userRightId)
         on delete cascade  on update restrict,
  engine  = InnoDB,
  comment = 'users = Owners of objects.  Most users may submit works';

show warnings;

alter table  users_history
  modify column  uid           char(36)      not null,
  modify column  login         varchar(254)                            after uid,
  add    column  userRightId   tinyint unsigned                        after login,
  modify column  rights        varchar(254)            default 'NONE'  after userRightId,
  modify column  usergroupUID  char(36)                                after rights,
  modify column  ownerUID      char(36)                                after usergroupUID,
  add    column  mtime         timestamp                               after ownerUID,
  modify column  certificate   varchar(32760),
  modify column  password      varchar(254)            default '',
  modify column  email         varchar(254)            default '',
  modify column  fname         varchar(254),
  modify column  lname         varchar(254),
  modify column  country       varchar(254),
  add    column  errorMsg      varchar(254),
  add    index   users_history_userRights_idx (userRightId),
  engine  = InnoDB,
  comment = 'users_history = Owners of objects';

show warnings;


-- ---------------------------------------------------------------------------
select 'Old table "usergroups"' as '';
-- Add column "errorMsg"
-- Add foreign key to table "users"
-- ---------------------------------------------------------------------------
alter table  usergroups
  modify column  uid       char(36)      not null,
  modify column  label     varchar(254)  not null  after uid,
  modify column  ownerUID  char(36)      not null  after label,
  add    column  mtime     timestamp               after ownerUID,
  modify column  webpage   varchar(254),
  add    column  errorMsg  varchar(254),
  add    constraint  fk_usergroups_users
         foreign key fk_usergroups_users_idx (ownerUID)
         references                users          (uid)
         on delete cascade  on update restrict,
  drop   index       owner,
  engine  = InnoDB,
  comment = 'usergroups = Groups of users';

show warnings;

alter table  usergroups_history
  modify column  uid       char(36)      not null,
  modify column  label     varchar(254)            after uid,
  modify column  ownerUID  char(36)                after label,
  add    column  mtime     timestamp               after ownerUID,
  modify column  webpage   varchar(254),
  add    column  errorMsg  varchar(254),
  engine  = InnoDB,
  comment = 'usergroups_history = Groups of users';

show warnings;


-- ---------------------------------------------------------------------------
select 'Old table "users"' as '';
-- Now that both referenced table "usergroups" and "users" are managed by InnoDB :
-- Add foreign keys to tables :  - "usergroups"
--                               - "users"
-- ---------------------------------------------------------------------------
alter table  users
  add    constraint    fk_users_usergroups
         foreign key   fk_users_usergroups_idx (usergroupUID)
         references             usergroups              (uid)
         on delete cascade  on update restrict,
  add    constraint    fk_users_users
         foreign key   fk_users_users_idx (owneruid)
         references             users          (uid)
         on delete cascade  on update restrict,
  drop   index         users_uniq_login,
  drop   index         groupuid,
  drop   index         owner;


-- ---------------------------------------------------------------------------
select 'New table "memberships"' as '';
-- Pure n-n relationship between "users" and "usergoups"
-- ---------------------------------------------------------------------------
create table if not exists  memberships  (
  userUID       char(36)   not null,
  usergroupUID  char(36)   not null,
  mtime         timestamp,
  primary key  (userUID, usergroupUID),
  constraint   fk_memberships_users
  foreign key  fk_memberships_users_idx (userUID)
   references                 users         (uid)
   on delete cascade  on update restrict,
  constraint   fk_memberships_usergroups
  foreign key  fk_memberships_usergroups_idx (usergroupUID)
   references                 usergroups              (uid)
   on delete cascade  on update restrict
   )
engine  = InnoDB,
comment = 'memberships = n-n relationship "users" - "usergoups"';

show warnings;

create table if not exists  memberships_history  (
  userUID       char(36)   not null,
  usergroupUID  char(36)   not null,
  mtime         timestamp,
  primary key   (userUID, usergroupUID),
  index         memberships_history_users_idx      (userUID),
  index         memberships_history_usergroups_idx (usergroupUID)
  )
engine  = InnoDB,
comment = 'memberships_history = n-n relationship "users" - "usergoups"';

show warnings;


-- ---------------------------------------------------------------------------
select 'Old table "hosts"' as '';
-- Insert new values of :      - "os" and "osversion" into table "oses"
--                             - "cputype"            into table "cpuTypes"
-- Add column "errorMsg"
-- Add foreign keys to tables  - "oses"
--                             - "cpuTypes"
--                             - "users"
-- ---------------------------------------------------------------------------
insert into     oses (osName)
select distinct       os
from   hosts
where  os not in (select oses.osName from oses);

insert into     cpuTypes (cpuTypeName)
select distinct           cputype
from   hosts
where  cputype not in (select cpuTypes.cpuTypeName from cpuTypes);
  

alter table  hosts
  modify column  uid             char(36)          not null,
  add    column  osId            tinyint unsigned  comment 'Maybe unknown yet'  after uid,
  modify column  os              char(7)           comment 'Maybe unknown yet'  after osId,
  modify column  osversion       varchar(36)       comment 'Maybe unknown yet'  after os,
  add    column  cpuTypeId       tinyint unsigned  comment 'Maybe unknown yet'  after osVersion,
  modify column  cputype         char(7)           comment 'Maybe unknown yet'  after cpuTypeId,
  add    column  usergroupUID    char(36)                                       after cputype,
  modify column  project         varchar(254)                                   after usergroupUID,
  modify column  sharedapps      varchar(254)                                   after project,
  modify column  sharedpackages  varchar(254)                                   after sharedapps,
  modify column  shareddatas     varchar(254)                                   after sharedpackages,
  modify column  ownerUID        char(36)          not null                     after shareddatas,
  modify column  name            varchar(254)                                   after ownerUID,
  add    column  mtime           timestamp                                      after name,
  modify column  natedipaddr     varchar(50),
  modify column  ipaddr          varchar(50),
  modify column  hwaddr          varchar(36),
  modify column  timezone        varchar(254),
  modify column  javaversion     varchar(254),
  modify column  cpumodel        varchar(50),
  modify column  version         varchar(254)      not null,
  modify column  sgid            varchar(254),
  modify column  jobid           varchar(254),
  modify column  batchid         varchar(254),
  modify column  userproxy       varchar(254),
  add    column  errorMsg        varchar(254),
  add    constraint    fk_hosts_oses
         foreign key   fk_hosts_oses_idx (osId)
         references             oses     (osId)
         on delete cascade  on update restrict,
  add    constraint    fk_hosts_cpuTypes
         foreign key   fk_hosts_cpuTypes_idx (cpuTypeId)
         references             cpuTypes     (cpuTypeId)
         on delete cascade  on update restrict,
  add    constraint    fk_hosts_users
         foreign key   fk_hosts_users_idx (ownerUID)
         references             users          (uid)
         on delete cascade  on update restrict,
  add    constraint    fk_hosts_usergroups
         foreign key   fk_hosts_usergroups_idx (usergroupUID)
         references             usergroups              (uid)
         on delete cascade  on update restrict,
  add    index         hosts_characteristics (name, ipaddr, hwaddr, cpuTypeId,
                                              cpunb, cpumodel, osId, osversion),
  drop   index         owner,
  engine  = InnoDB,
  comment = 'hosts = Computing resources where an XWHEP worker may run';

show warnings;

alter table  hosts_history
  modify column  uid             char(36)          not null,
  add    column  osId            tinyint unsigned  comment 'Maybe unknown yet'  after uid,
  modify column  os              char(7)           comment 'Maybe unknown yet'  after osId,
  modify column  osversion       varchar(36)       comment 'Maybe unknown yet'  after os,
  add    column  cpuTypeId       tinyint unsigned  comment 'Maybe unknown yet'  after osVersion,
  modify column  cputype         char(7)           comment 'Maybe unknown yet'  after cpuTypeId,
  add    column  usergroupUID    char(36)                                       after cputype,
  modify column  project         varchar(254)                                   after usergroupUID,
  modify column  sharedapps      varchar(254)                                   after project,
  modify column  sharedpackages  varchar(254)                                   after sharedapps,
  modify column  shareddatas     varchar(254)                                   after sharedpackages,
  modify column  ownerUID        char(36)                                       after shareddatas,
  modify column  name            varchar(254)                                   after ownerUID,
  add    column  mtime           timestamp                                      after name,
  modify column  natedipaddr     varchar(50),
  modify column  ipaddr          varchar(50),
  modify column  hwaddr          varchar(36),
  modify column  timezone        varchar(254),
  modify column  javaversion     varchar(254),
  modify column  cpumodel        varchar(50),
  modify column  version         varchar(254),
  modify column  sgid            varchar(254),
  modify column  jobid           varchar(254),
  modify column  batchid         varchar(254),
  modify column  userproxy       varchar(254),
  add    column  errorMsg        varchar(254),
  add    index   hosts_history_oses_idx       (osId),
  add    index   hosts_history_cputype_idx    (cpuTypeId),
  add    index   hosts_history_usergroups_idx (usergroupUID),
  add    index   hosts_characteristics        (name, ipaddr, hwaddr, cpuTypeId,
                                              cpunb, cpumodel, osId, osversion),
  engine  = InnoDB,
  comment = 'hosts_history = Computing resources for XWHEP workers';

show warnings;


-- ---------------------------------------------------------------------------
select 'Old table "traces"' as '';
-- Place column "ownerUID" just after "hostUID"
-- Add foreign keys to tables  - "hosts"
--                             - "users"
-- ---------------------------------------------------------------------------
alter table  traces
  modify column  uid       char(36)      not null,
  modify column  hostUID   char(36)      not null  after uid,
  modify column  ownerUID  char(36)      not null  after hostUID,
  add    column  mtime     timestamp               after ownerUID,
  modify column  login     varchar(254)  not null  default '',
  modify column  data      varchar(254)  not null  default '',
  add    constraint   fk_traces_hosts
         foreign key  fk_traces_hosts_idx (hostUID)
         references             hosts         (uid)
         on delete cascade  on update restrict,
  add    constraint   fk_traces_users
         foreign key  fk_traces_users_idx (owneruid)
         references             users          (uid)
         on delete cascade  on update restrict,
  drop   index        hostuid,
  drop   index        owner,
  engine  = InnoDB,
  comment = 'traces (NOT USED ANYMORE)';

show warnings;

alter table  traces_history
  modify column  uid       char(36)      not null,
  modify column  hostUID   char(36)                after uid,
  modify column  ownerUID  char(36)                after hostUID,
  add    column  mtime     timestamp               after ownerUID,
  modify column  login     varchar(254)            default '',
  modify column  data      varchar(254)            default '',
  engine  = InnoDB,
  comment = 'traces_history (NOT USED ANYMORE)';

show warnings;


-- ---------------------------------------------------------------------------
select 'New table "sharedAppTypes"' as '';
-- Pure n-n relationship between "hosts" and "appTypes"
-- ---------------------------------------------------------------------------
create table if not exists  sharedAppTypes  (
  hostUID     char(36)            not null,
  appTypeId   tinyint    unsigned not null,
  mtime       timestamp,
  primary key (hostUID, appTypeId),
  constraint  fk_sharedAppTypes_hosts
  foreign key fk_sharedAppTypes_hosts_idx (hostUID)
   references                   hosts         (uid)
   on delete cascade  on update restrict,
  constraint  fk_sharedAppTypes_appTypes
  foreign key fk_sharedAppTypes_appTypes_idx (appTypeId)
   references                   appTypes     (appTypeId)
   on delete cascade  on update restrict
   )
engine  = InnoDB,
comment = 'sharedAppTypes = n-n relationship "hosts" - "appTypes"';

show warnings;

create table if not exists  sharedAppTypes_history  (
  hostUID     char(36)            not null,
  appTypeId   tinyint    unsigned not null,
  mtime       timestamp,
  primary key (hostUID, appTypeId),
  index       sharedAppTypes_history_hosts_idx    (hostUID),
  index       sharedAppTypes_history_appTypes_idx (appTypeId)
  )
engine  = InnoDB,
comment = 'sharedAppTypes_history = n-n relationship "hosts"-"appTypes"';

show warnings;


-- ---------------------------------------------------------------------------
select 'New table "sharedPackageTypes"' as '';
-- Pure n-n relationship between "hosts" and "packageTypes"
-- ---------------------------------------------------------------------------
create table if not exists  sharedPackageTypes  (
  hostUID        char(36)            not null,
  packageTypeId  tinyint    unsigned not null,
  mtime          timestamp,
  primary key (hostUID, packageTypeId),
  constraint  fk_sharedPackageTypes_hosts
  foreign key fk_sharedPackageTypes_hosts_idx (hostUID)
   references                       hosts         (uid)
   on delete cascade  on update restrict,
  constraint  fk_sharedPackageTypes_packageTypes
  foreign key fk_sharedPackageTypes_packageTypes_idx (packageTypeId)
   references                       packageTypes     (packageTypeId)
   on delete cascade  on update restrict
   )
engine  = InnoDB,
comment = 'sharedPackageTypes = n-n relationship "hosts"-"packageTypes"';

show warnings;

create table if not exists  sharedPackageTypes_history  (
  hostUID        char(36)            not null,
  packageTypeId  tinyint    unsigned not null,
  mtime          timestamp,
  primary key (hostUID, packageTypeId),
  index       sharedPackageTypes_history_hosts_idx        (hostUID),
  index       sharedPackageTypes_history_packageTypes_idx (packageTypeId)
  )
engine  = InnoDB,
comment = 'sharedPackageTypes_history = n-n rel. "hosts"-"packageTypes"';

show warnings;


-- ---------------------------------------------------------------------------
select 'Old table "datas"' as '';
-- Add columns  - "statusId"  :  Foreign key to "statuses"."statusId"
--              - "osVersion"
--              - "errorMsg"
-- Add foreign keys to tables  - "statuses"
--                             - "oses"
--                             - "cpuTypes"
--                             - "users"
-- ---------------------------------------------------------------------------
alter table  datas
  modify column  uid        char(36)     not null,
  add    column  statusId   tinyint unsigned                       after uid,
  modify column  status     varchar(36)  not null  default 'NONE'  after statusId,
  add    column  dataTypeId tinyint unsigned                       after status,
  modify column  type       varchar(254)                           after dataTypeId,
  add    column  osId       tinyint unsigned                       after type,
  modify column  os         char(7)                                after osId,
  add    column  osVersion  varchar(36)                            after os,
  add    column  cpuTypeId  tinyint unsigned                       after osVersion,
  modify column  cpu        char(7)                                after cpuTypeId,
  modify column  ownerUID   char(36)     not null                  after cpu,
  modify column  name       varchar(254)                           after ownerUID,
  add    column  mtime      timestamp                              after name,
  modify column  uri        varchar(254),
  modify column  md5        varchar(254),
  add    column  errorMsg   varchar(254),
  add    constraint    fk_datas_statuses
         foreign key   fk_datas_statuses_idx (statusId)
         references             statuses     (statusId)
         on delete cascade  on update restrict,
  add    constraint    fk_datas_types
         foreign key   fk_datas_types_idx (dataTypeId)
         references             dataTypes (dataTypeId)
         on delete cascade  on update restrict,
  add    constraint    fk_datas_oses
         foreign key   fk_datas_oses_idx (osId)
         references             oses     (osId)
         on delete cascade  on update restrict,
  add    constraint    fk_datas_cpuTypes
         foreign key   fk_datas_cpuTypes_idx (cpuTypeId)
         references             cpuTypes     (cpuTypeId)
         on delete cascade  on update restrict,
  add    constraint    fk_datas_users
         foreign key   fk_datas_users_idx (ownerUID)
         references             users          (uid)
         on delete cascade  on update restrict,
  drop   index         owner,
  engine  = InnoDB,
  comment = 'datas = Files (binaries, input files, results)';

show warnings;

alter table  datas_history
  modify column  uid        char(36)     not null,
  add    column  statusId   tinyint unsigned                       after uid,
  modify column  status     varchar(36)                            after statusId,
  add    column  dataTypeId tinyint unsigned                       after status,
  modify column  type       varchar(254)                           after dataTypeId,
  add    column  osId       tinyint unsigned                       after type,
  modify column  os         char(7)                                after osId,
  add    column  osVersion  varchar(36)                            after os,
  add    column  cpuTypeId  tinyint unsigned                       after osVersion,
  modify column  cpu        char(7)                                after cpuTypeId,
  modify column  ownerUID   char(36)                               after cpu,
  modify column  name       varchar(254)                           after ownerUID,
  add    column  mtime      timestamp                              after name,
  modify column  uri        varchar(254),
  modify column  md5        varchar(254),
  add    column  errorMsg   varchar(254),
  add    index   datas_history_statuses_idx (statusId),
  add    index   datas_history_types_idx    (dataTypeId),
  add    index   datas_history_oses_idx     (osId),
  add    index   datas_history_cpuTypes_idx (cpuTypeId),
  engine  = InnoDB,
  comment = 'datas_history = Files (binaries, input files, results)';

show warnings;


-- ---------------------------------------------------------------------------
select 'Old table "apps"' as '';
-- Add columns  - "appTypeId" :  Foreign key to "appTypes"."appTypeId"
--              - "minFreeMassStorage"
--              - "errorMsg"
-- Add foreign keys to tables  - "appTypes"
--                             - "users"
-- ---------------------------------------------------------------------------
alter table  apps
  modify column  uid                 char(36)      not null,
  modify column  name                varchar(254)  not null  unique  after uid,
  add    column  appTypeId           tinyint unsigned                after name,
  modify column  type                varchar(254)  not null          after appTypeId,
  add    column  packageTypeId       tinyint unsigned                after type,
  modify column  neededpackages      varchar(254)                    after packageTypeId,
  modify column  ownerUID            char(36)      not null          after neededpackages,
  add    column  mtime               timestamp                       after ownerUID,
  modify column  envvars             varchar(254)                    after mtime,
  modify column  isdeleted           char(5)       default 'false'   after envvars,
  add    column  minFreeMassStorage  int(10)       default 0         after minCPUSpeed,
  modify column  webpage             varchar(254),
  modify column  defaultStdinURI     varchar(254),
  modify column  baseDirinURI        varchar(254),
  modify column  defaultDirinURI     varchar(254),
  modify column  launchscriptshuri   varchar(254)                    after defaultDirinURI,
  modify column  launchscriptcmduri  varchar(254)                    after launchscriptshuri,
  modify column  unloadscriptshuri   varchar(254)                    after launchscriptcmduri,
  modify column  unloadscriptcmduri  varchar(254)                    after unloadscriptshuri,
  add    column  errorMsg            varchar(254)                    after unloadscriptcmduri,
  add    constraint     fk_apps_appTypes
         foreign key    fk_apps_appTypes_idx (appTypeId)
         references             appTypes     (appTypeId)
         on delete cascade  on update restrict,
  add    constraint     fk_apps_packageTypes
         foreign key    fk_apps_packageTypes_idx (packageTypeId)
         references             packageTypes     (packageTypeId)
         on delete cascade  on update restrict,
  add    constraint     fk_apps_users
         foreign key    fk_apps_users_idx (ownerUID)
         references             users          (uid)
         on delete cascade  on update restrict,
  drop   index          apps_uniq_name,
  drop   index          owner,
  engine  = InnoDB,
  comment = 'apps = Applications, submitted by users inside works';

show warnings;

alter table  apps_history
  modify column  uid                 char(36)      not null,
  modify column  name                varchar(254)                    after uid,
  add    column  appTypeId           tinyint unsigned                after name,
  modify column  type                varchar(254)                    after appTypeId,
  add    column  packageTypeId       tinyint unsigned                after type,
  modify column  neededpackages      varchar(254)                    after packageTypeId,
  modify column  ownerUID            char(36)                        after neededpackages,
  add    column  mtime               timestamp                       after ownerUID,
  modify column  envvars             varchar(254)                    after mtime,
  modify column  isdeleted           char(5)       default 'false'   after envvars,
  add    column  minFreeMassStorage  int(10)       default 0         after minCPUSpeed,
  modify column  webpage             varchar(254),
  modify column  defaultStdinURI     varchar(254),
  modify column  baseDirinURI        varchar(254),
  modify column  defaultDirinURI     varchar(254),
  modify column  launchscriptshuri   varchar(254)                    after defaultDirinURI,
  modify column  launchscriptcmduri  varchar(254)                    after launchscriptshuri,
  modify column  unloadscriptshuri   varchar(254)                    after launchscriptcmduri,
  modify column  unloadscriptcmduri  varchar(254)                    after unloadscriptshuri,
  add    column  errorMsg            varchar(254)                    after unloadscriptcmduri,
  add    index   apps_history_appTypes_idx     (appTypeId),
  add    index   apps_history_packageTypes_idx (packageTypeId),
  engine  = InnoDB,
  comment = 'apps_history = Applications, submitted by users inside works';

show warnings;


-- ---------------------------------------------------------------------------
select 'New table "executables"' as '';
-- Application files (which may be stored outside XtremWeb-HEP)
-- ---------------------------------------------------------------------------
create table if not exists  executables  (
  executableId  int unsigned      not null  auto_increment  primary key,
  appUID        char(36)          not null,
  dataTypeId    tinyint unsigned  not null,
  osId          tinyint unsigned  not null,
  osVersion     varchar(36)           null  comment 'May be NULL',
  cpuTypeId     tinyint unsigned      null  comment 'May be NULL for Java byte code',
  dataUID       char(36)              null  comment 'May be NULL if data is external',
  dataURI       varchar(254)      not null,
  mtime         timestamp,
  constraint    fk_executables_apps
  foreign key   fk_executables_apps_idx (appUID)
   references                  apps        (uid)
   on delete cascade  on update restrict,
  constraint    fk_executables_dataTypes
  foreign key   fk_executables_dataTypes_idx (dataTypeId)
   references                  dataTypes     (dataTypeId)
   on delete cascade  on update restrict,
  constraint    fk_executables_oses
  foreign key   fk_executables_oses_idx (osId)
   references                  oses     (osId)
   on delete cascade  on update restrict,
  constraint    fk_executables_cpuTypes
  foreign key   fk_executables_cpuTypes_idx (cpuTypeId)
   references                  cpuTypes     (cpuTypeId)
   on delete cascade  on update restrict,
  constraint    fk_executables_datas
  foreign key   fk_executables_datas_idx (dataUID)
   references                  datas         (uid)
   on delete cascade  on update restrict,
  unique index  unique_executables (appUID, dataTypeId, osId, osVersion, cpuTypeId)
   )
engine  = InnoDB,
comment = 'executables = Application files';

show warnings;

create table if not exists  executables_history  (
  executableId  int unsigned      not null  auto_increment  primary key,
  appUID        char(36)          not null,
  dataTypeId    tinyint unsigned  not null,
  osId          tinyint unsigned  not null,
  osVersion     varchar(36)           null  comment 'May be NULL',
  cpuTypeId     tinyint unsigned      null  comment 'May be NULL for Java byte code',
  dataUID       char(36)              null  comment 'May be NULL if data is external',
  dataURI       varchar(254)      not null,
  mtime         timestamp,
  index         executables_history_apps_idx      (appUID),
  index         executables_history_dataTypes_idx (dataTypeId),
  index         executables_history_oses_idx      (osId),
  index         executables_history_cpuTypes_idx  (cpuTypeId),
  index         executables_history_datas_idx     (dataUID)
  )
engine  = InnoDB,
comment = 'executables_history = Application files';

show warnings;


-- ---------------------------------------------------------------------------
select 'Old table "sessions"' as '';
-- Add column "errorMsg"
-- Add foreign key to table "users"
-- ---------------------------------------------------------------------------
alter table  sessions
  modify column  uid       char(36)      not null,
  modify column  ownerUID  char(36)      not null  after uid,
  modify column  name      varchar(254)  not null  after ownerUID,
  add    column  mtime     timestamp               after name,
  add    column  errorMsg  varchar(254),
  add    constraint  fk_sessions_users
         foreign key fk_sessions_users_idx (ownerUID)
         references              users          (uid)
         on delete cascade  on update restrict,
  drop   index       owner,
  engine  = InnoDB,
  comment = 'work-sessions = Sessions for transient grouping of works';

show warnings;


alter table  sessions_history
  modify column  uid       char(36)      not null,
  modify column  ownerUID  char(36)                after uid,
  modify column  name      varchar(254)            after ownerUID,
  add    column  mtime     timestamp               after name,
  add    column  errorMsg  varchar(254),
  engine  = InnoDB,
  comment = 'work-sessions_history = Sessions for transient work grouping';

show warnings;


-- ---------------------------------------------------------------------------
select 'Old table "groups"' as '';
-- Add column "errorMsg"
-- Add foreign keys to tables  - "sessions"
--                             - "users"
-- ---------------------------------------------------------------------------
alter table  groups
  modify column  uid         char(36)      not null,
  modify column  sessionUID  char(36)                after uid,
  modify column  ownerUID    char(36)      not null  after sessionUID,
  modify column  name        varchar(254)  not null  after ownerUID,
  add    column  mtime       timestamp               after name,
  add    column  errorMsg    varchar(254),
  add    constraint   fk_groups_sessions
         foreign key  fk_groups_sessions_idx (sessionUID)
         references             sessions            (uid)
         on delete cascade  on update restrict,
  add    constraint   fk_groups_users
         foreign key  fk_groups_users_idx (ownerUID)
         references             users          (uid)
         on delete cascade  on update restrict,
  drop   index        sessionuid,
  drop   index        owner,
  engine  = InnoDB,
  comment = 'work-groups = Persistent groups of works';

show warnings;

alter table  groups_history
  modify column  uid         char(36)      not null,
  modify column  sessionUID  char(36)                after uid,
  modify column  ownerUID    char(36)                after sessionUID,
  modify column  name        varchar(254)            after ownerUID,
  add    column  mtime       timestamp               after name,
  add    column  errorMsg    varchar(254),
  engine  = InnoDB,
  comment = 'work-groups_history = Persistent groups of works';

show warnings;


-- ---------------------------------------------------------------------------
select 'Old table "works"' as '';
-- Add columns  - "statusId"           :  Foreign key to "statuses"."statusId"
--              - "minFreeMassStorage"
--              - "errorMsg"
--              - "maxWallClockTime"  (it will replace  "wallclocktime") later
-- Add foreign keys to tables  - "apps"
--                             - "statuses"
--                             - "sessions"
--                             - "groups"
--                             - "hosts"
--                             - "users"
-- ---------------------------------------------------------------------------
alter table  works
  modify column  uid                 char(36)     not null,
  modify column  appUID              char(36)     not null                  after uid,
  add    column  statusId            tinyint unsigned                       after appUID,
  modify column  status              varchar(36)  not null  default 'NONE'  after statusId,
  modify column  sessionUID          char(36)                               after status,
  modify column  groupUID            char(36)                               after sessionUID,
  modify column  expectedhostUID     char(36)                               after groupUID,
  modify column  ownerUID            char(36)     not null                  after expectedhostUID,
  modify column  label               varchar(254)                           after ownerUID,
  add    column  mtime               timestamp                              after label,
  add    column  minFreeMassStorage  int(10)                default 0       after minCPUSpeed,
  add    column  maxWallClockTime    int(10)                                after minFreeMassStorage,
  modify column  userproxy           varchar(254),
  modify column  sgid                varchar(254),
  modify column  server              varchar(254),
  modify column  cmdLine             varchar(32760),
  modify column  listenport          varchar(254),
  modify column  smartsocketaddr     varchar(8190),
  modify column  smartsocketclient   varchar(8190),
  modify column  stdinURI            varchar(254),
  modify column  dirinURI            varchar(254),
  modify column  resultURI           varchar(254),
  modify column  envvars             varchar(254),
  add    column  errorMsg            varchar(254),
  add    constraint    fk_works_apps
         foreign key   fk_works_apps_idx (appUID)
         references             apps        (uid)
         on delete cascade  on update restrict,
  add    constraint    fk_works_statuses
         foreign key   fk_works_statuses_idx (statusId)
         references             statuses     (statusId)
         on delete cascade  on update restrict,
  add    constraint    fk_works_sessions
         foreign key   fk_works_sessions_idx (sessionUID)
         references             sessions            (uid)
         on delete cascade  on update restrict,
  add    constraint    fk_works_groups
         foreign key   fk_works_groups_idx (groupUID)
         references             groups          (uid)
         on delete cascade  on update restrict,
  add    constraint    fk_works_hosts
         foreign key   fk_works_hosts_idx (expectedhostUID)
         references             hosts                 (uid)
         on delete set null
         on update restrict,
  add    constraint    fk_works_users
         foreign key   fk_works_users_idx (ownerUID)
         references             users          (uid)
         on delete cascade  on update restrict,
  drop   index         app,
  drop   index         session_uid,
  drop   index         group_uid,
  drop   index         owner,
  add    index         completedDate (completedDate),
  engine  = InnoDB,
  comment = 'works = Jobs submitted by a user with app and input data';

show warnings;

alter table  works_history
  modify column  uid                 char(36)     not null,
  modify column  appUID              char(36)                               after uid,
  add    column  statusId            tinyint unsigned                       after appUID,
  modify column  status              varchar(36)                            after statusId,
  modify column  sessionUID          char(36)                               after status,
  modify column  groupUID            char(36)                               after sessionUID,
  modify column  expectedhostUID     char(36)                               after groupUID,
  modify column  ownerUID            char(36)                               after expectedhostUID,
  modify column  label               varchar(254)                           after ownerUID,
  add    column  mtime               timestamp                              after label,
  add    column  minFreeMassStorage  int(10)                default 0       after minCPUSpeed,
  add    column  maxWallClockTime    int(10)                                after minFreeMassStorage,
  modify column  userproxy           varchar(254),
  modify column  sgid                varchar(254),
  modify column  server              varchar(254),
  modify column  cmdLine             varchar(32760),
  modify column  listenport          varchar(254),
  modify column  smartsocketaddr     varchar(8190),
  modify column  smartsocketclient   varchar(8190),
  modify column  stdinURI            varchar(254),
  modify column  dirinURI            varchar(254),
  modify column  resultURI           varchar(254),
  modify column  envvars             varchar(254),
  add    column  errorMsg            varchar(254),
  add    index   works_history_statuses_idx (statusId),
  add    index   completedDate              (completedDate),
  engine  = InnoDB,
  comment = 'works_history = Jobs submitted by users';

show warnings;


-- ---------------------------------------------------------------------------
select 'Old table "tasks"' as '';
-- Add columns  - "statusId" :  Foreign key to "statuses"."statusId"
--              - "errorMsg"
-- Add foreign keys to tables  - "works"
--                             - "hosts"
--                             - "statuses"
--                             - "users"
-- ---------------------------------------------------------------------------
alter table  tasks
  modify column  uid       char(36)     not null,
  modify column  workUID   char(36)     not null                  after uid,
  modify column  hostUID   char(36)                               after workUID,
  add    column  statusId  tinyint unsigned                       after hostUID,
  modify column  status    varchar(36)  not null  default 'NONE'  after statusId,
  modify column  ownerUID  char(36)     not null                  after status,
  add    column  mtime     timestamp                              after ownerUID,
  add    column  errorMsg  varchar(254),
  add    constraint    fk_tasks_works
         foreign key   fk_tasks_works_idx (workUID)
         references             works         (uid)
         on delete cascade  on update restrict,
  add    constraint    fk_tasks_hosts
         foreign key   fk_tasks_hosts_idx (hostUID)
         references             hosts         (uid)
         on delete cascade  on update restrict,
  add    constraint    fk_tasks_statuses
         foreign key   fk_tasks_statuses_idx (statusId)
         references             statuses     (statusId)
         on delete cascade  on update restrict,
  add    constraint    fk_tasks_users
         foreign key   fk_tasks_users_idx (owneruid)
         references             users          (uid)
         on delete cascade  on update restrict,
  drop   index         hostuid,
  drop   index         owner,
  engine  = InnoDB,
  comment = 'tasks = Work sent to a host with the adequate app binary';

show warnings;

alter table  tasks_history
  modify column  uid       char(36)     not null,
  modify column  workUID   char(36)                               after uid,
  modify column  hostUID   char(36)                               after workUID,
  add    column  statusId  tinyint unsigned                       after hostUID,
  modify column  status    varchar(36)                            after statusId,
  modify column  ownerUID  char(36)                               after status,
  add    column  mtime     timestamp                              after ownerUID,
  add    column  errorMsg  varchar(254),
  add    index   tasks_history_statuses_idx (statusId),
  engine  = InnoDB,
  comment = 'tasks_history = Work sent to a host with adequate app binary';

show warnings;
