-- 
--  Copyrights     : CNRS
--  Author         : Oleg Lodygensky
--  Acknowledgment : XtremWeb-HEP is based on XtremWeb 1.8.0 by inria : http://www.xtremweb.net/
--  Web            : http://www.xtremweb-hep.org
--  
--       This file is part of XtremWeb-HEP.
-- 
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
-- 



-- 
-- Version : 8.3.1
--           Modified by E. URBAH as stand-alone script for the current schema
-- 
-- File    : xwsetupdb.sql
-- Purpose : this file contains the needed SQL commands to 
--           create the XWHEP database
-- Usage   : this is used by xtremweb.database
-- 

-- 
-- you must uncomment the two next commands and set the appropriate
-- database name

--   CREATE DATABASE IF NOT EXISTS @DBNAME@;

--   USE @DBNAME@;

--   connect @DBNAME@;

-- 
-- please don't forget to look at the end of this file
-- to set your xtremweb database user privileges
-- 


-- 
-- Since XWHEP 5.7.3, next table contains XWHEP versions
-- 
create table if not exists  versions  (
  version       char(20)  ,
  installation  datetime  
  )
ENGINE = InnoDB,  comment = 'Versions';

show warnings;


-- 
-- next contains applications
-- 
create table if not exists  apps  (
  uid                 char(50)      not null                   comment 'Primary key',
  ownerUID            char(50)                                 comment 'Optionnal. user UID',
  name                char(100)                                comment 'Secondary key. if your change length, don t forget to change AppInterface.APPNAMELENGTH',
  isService           char(5)                 default 'false'  comment 'Optionnal. true if app is a service',
  accessRights        int(4)                  default 0x700    comment 'This defines access rights "a la" linux FS  See xtremweb.common.XWAccessRights.java',
  avgExecTime         int(15)                 default 0        comment 'Average execution time. updated on work completion',
  minMemory           int(10)                 default 0        comment 'Optionnal. minimum memory need',
  minCPUSpeed         int(10)                 default 0        comment 'Optionnal. minimum CPU speed need',
  nbJobs              int(15)                 default 0        comment 'Completed jobs counter. updated on work completion',
  pendingJobs         int(15)                 default 0        comment 'Pending jobs counter. updated on work submission',
  runningJobs         int(15)                 default 0        comment 'Running jobs counter. updated on work request',
  errorJobs           int(15)                 default 0        comment 'Error jobs counter. updated on job error',
  webpage             char(200)                                comment 'Application web page',
  defaultStdinURI     char(200)                                comment 'Optionnal. this is the default STDIN, if any. this is an URI  If set, this is automatically provided to any job, except if job defines its own STDIN  Works.stdin may be set to NULLUID to force no STDIN even if this apps.stdin is defined',
  baseDirinURI        char(200)                                comment 'Optionnal. this is the base environments, if any. this is an URI  If set, this is automatically provided to any job  If set, this is installed **after** defaultdirin or works.dirin  To ensure those last do not override any file contained in basedirin',
  defaultDirinURI     char(200)                                comment 'Optionnal. this is the default environment, if any. this is an URI  If set, this is automatically provided to any job, except if job defines its own DIRIN  Works.dirin may be set to NULLUID to force no DIRIN even if this apps.dirin is defined  This is ins',  --  Optionnal. this is the default environment, if any. this is an URI  If set, this is automatically provided to any job, except if job defines its own DIRIN  Works.dirin may be set to NULLUID to force no DIRIN even if this apps.dirin is defined  This is installed before basedirin to ensure this does not override  Any of the apps.basedirin files
  linux_ix86URI       char(200)                                comment 'Optionnal. this is the linux    ix86  binary, if any. this is an URI',
  linux_amd64URI      char(200)                                comment 'Optionnal. this is the linux    amd64 binary, if any. this is an URI',
  linux_x86_64URI     char(200)                                comment 'Optionnal. this is the linux    x86 64 binary, if any. this is an URI',
  linux_ia64URI       char(200)                                comment 'Optionnal. this is the linux    intel itanium binary, if any. this is an URI',
  linux_ppcURI        char(200)                                comment 'Optionnal. this is the linux    ppc   binary, if any. this is an URI',
  macos_ix86URI       char(200)                                comment 'Optionnal. this is the mac os 10 ix86  binary, if any. this is an URI',
  macos_x86_64URI     char(200)                                comment 'Optionnal. this is the mac os 10 x86_64  binary, if any. this is an URI',
  macos_ppcURI        char(200)                                comment 'Optionnal. this is the mac os 10 ppc   binary, if any. this is an URI',
  win32_ix86URI       char(200)                                comment 'Optionnal. this is the win32    ix86  binary, if any. this is an URI',
  win32_amd64URI      char(200)                                comment 'Optionnal. this is the win32    amd64 binary, if any. this is an URI',
  win32_x86_64URI     char(200)                                comment 'Optionnal. this is the win32   x86_64 binary, if any. this is an URI',
  javaURI             char(200)                                comment 'Optionnal. this is the java           binary, if any. this is an URI',
  osf1_alphaURI       char(200)                                comment 'Optionnal. this is the osf1     alpha binary, if any. this is an URI',
  osf1_sparcURI       char(200)                                comment 'Optionnal. this is the osf1     sparc binary, if any. this is an URI',
  solaris_alphaURI    char(200)                                comment 'Optionnal. this is the solaris  alpha binary, if any. this is an URI',
  solaris_sparcURI    char(200)                                comment 'Optionnal. this is the solaris  sparc binary, if any. this is an URI',
  ldlinux_ix86URI     char(200)                                comment 'Optionnal. this is the linux    ix86  library, if any. this is an URI',
  ldlinux_amd64URI    char(200)                                comment 'Optionnal. this is the linux    amd64 library, if any. this is an URI',
  ldlinux_x86_64URI   char(200)                                comment 'Optionnal. this is the linux    x86 64 library, if any. this is an URI',
  ldlinux_ia64URI     char(200)                                comment 'Optionnal. this is the linux    intel itanium library, if any. this is an URI',
  ldlinux_ppcURI      char(200)                                comment 'Optionnal. this is the linux    ppc   library, if any. this is an URI',
  ldmacos_ix86URI     char(200)                                comment 'Optionnal. this is the mac os x ix86  library, if any. this is an URI',
  ldmacos_x86_64URI   char(200)                                comment 'Optionnal. this is the mac os x ix86  library, if any. this is an URI',
  ldmacos_ppcURI      char(200)                                comment 'Optionnal. this is the mac os x ppc   library, if any. this is an URI',
  ldwin32_ix86URI     char(200)                                comment 'Optionnal. this is the win32    ix86  library, if any. this is an URI',
  ldwin32_amd64URI    char(200)                                comment 'Optionnal. this is the win32    amd64 library, if any. this is an URI',
  ldwin32_x86_64URI   char(200)                                comment 'Optionnal. this is the win32   x86_64 library, if any. this is an URI',
  ldosf1_alphaURI     char(200)                                comment 'Optionnal. this is the osf1     alpha library, if any. this is an URI',
  ldosf1_sparcURI     char(200)                                comment 'Optionnal. this is the osf1     sparc library, if any. this is an URI',
  ldsolaris_alphaURI  char(200)                                comment 'Optionnal. this is the solaris  alpha library, if any. this is an URI',
  ldsolaris_sparcURI  char(200)                                comment 'Optionnal. this is the solaris  sparc library, if any. this is an URI',
  launchscriptshuri   varchar(200)                             comment 'Optionnal, launch shell scripts  Since 8.0.0',
  launchscriptcmduri  varchar(200)                             comment 'Optionnal, launch MS command scripts  Since 8.0.0',
  unloadscriptshuri   varchar(200)                             comment 'Optionnal, unload shell scripts  Since 8.0.0',
  unloadscriptcmduri  varchar(200)                             comment 'Optionnal, unload MS command scripts  Since 8.0.0',
  envvars             varchar(200)                             comment 'Optional, env vars  Since 8.0.0',
  type                varchar(50)             default 'none'   comment 'This defines the application type  "none" : undefined; no job will run  "binary" : the worker must download the binary  "shared" : this is a shared application : the worker don"t download any bianry  "virtualbox" : this is a script for virtualbox shared a',  --  This defines the application type  "none" : undefined; no job will run  "binary" : the worker must download the binary  "shared" : this is a shared application : the worker don't download any bianry  "virtualbox" : this is a script for virtualbox shared application  "vmware" : this is a script for vmware shared application  Since 8.0.0
  neededpackages      varchar(200)                             comment 'Optional, needed packages on worker side  Since 8.0.0',
  isdeleted           char(5)                 default 'false'  comment 'True if this row has been deleted',
  
  
  primary key (uid),
  constraint unique index apps_uniq_name(name),
  index owner(owneruid)
  )
ENGINE = InnoDB,  comment = 'Applications';

show warnings;


-- 
-- since 7.0.0 deleted rows are stored in history table
-- 
create table if not exists  apps_history  like  apps;

show warnings;

-- ALTER TABLE apps_history ADD PRIMARY KEY USING HASH  (uid):
-- ALTER TABLE apps_history ADD CONSTRAINT UNIQUE INDEX (name):

-- 
-- next contains datas : binary, input files, results
-- 
create table if not exists  datas  (
  uid            char(50)   not null                   comment 'Primary key',
  ownerUID       char(50)                              comment 'May be {user, app, work} UID',
  uri            char(200)                             comment 'This is the URI of the content',
  accessRights   int(4)               default 0x700    comment 'This defines access rights "a la" linux FS  See xtremweb.common.XWAccessRights.java',
  name           char(200)                             comment 'Symbolic file name (i.e. alias name)',
  links          int(4)                                comment 'How many times it is used. can be deleted if 0',
  accessDate     datetime                              comment 'Last access date',
  insertionDate  datetime                              comment 'Creation date',
  status         char(20)                              comment 'Status. see common/XWStatus.java',
  type           char(20)                              comment 'See common/DataType.java',
  os             char(20)                              comment 'Optionnal. mainly necessary if data is an app binary',
  cpu            char(20)                              comment 'Optionnal. mainly necessary if data is an app binary',
  md5            char(50)                              comment 'Data md5',
  size           bigint                                comment 'Effective file size',
  sendToClient   char(5)              default 'false'  comment 'Optionnal. used by replication',
  replicated     char(5)              default 'false'  comment 'Optionnal. used by replication',
  isdeleted      char(5)              default 'false'  comment 'True if row has been deleted ',
  
  primary key (uid),
  index owner(owneruid)
  )
ENGINE = InnoDB,  comment = 'Datas : binary, input files, results';

show warnings;

-- 
-- since 7.0.0 deleted rows are stored in history table
-- 
create table if not exists  datas_history  like  datas;

show warnings;

-- ALTER TABLE datas_history ADD PRIMARY KEY USING HASH (uid);


-- 
-- next aims to group jobs
-- groups are not erased when the client disconnect : they are kept between client sessions
-- 
create table if not exists  groups  (
  uid           char(50)  not null                   comment 'Primary key',
  name          char(50)  not null                   comment 'Group name',
  sessionUID    char(50)                             comment 'Session UID',
  accessRights  int(4)              default 0x700    comment 'Since 5.8.0  This defines access rights "a la" linux FS  See xtremweb.common.XWAccessRights.java',
  ownerUID      char(50)  not null                   comment 'Owner (user) UID',
  isdeleted     char(5)             default 'false'  comment 'True if this row has been deleted',
  
  primary key (uid),
  index owner(owneruid),
  index sessionuid(sessionuid)
  )
ENGINE = InnoDB,  comment = 'Group jobs';

show warnings;

-- 
-- since 7.0.0 deleted rows are stored in history table
-- 
create table if not exists  groups_history  like  groups;

show warnings;

-- ALTER TABLE groups_history ADD PRIMARY KEY USING HASH (uid);


-- 
-- next contains worker definitions
-- 
create table if not exists  hosts  (
  uid                  char(50)      not null                   comment 'Primary key',
  poolworksize         int(2)                  default 0        comment 'This is the amount of simultaneous jobs',
  nbJobs               int(15)                 default 0        comment 'Completed jobs counter. updated on work completion',
  pendingJobs          int(15)                 default 0        comment 'Pending jobs counter. updated on work submission',
  runningJobs          int(15)                 default 0        comment 'Running jobs counter. updated on work request',
  errorJobs            int(15)                 default 0        comment 'Error jobs counter. updated on job error',
  timeOut              int(15)                                  comment 'How many time to wait before this host is considered as lost  Default is provided by server, but this may also be defined from host config',
  avgExecTime          int(15)                 default 0        comment 'Average execution time. updated on work completion',
  lastAlive            datetime                                 comment 'Last communication time (helps to determine whether this host is lost)',
  name                 char(100)                                comment 'This host name',
  nbconnections        int(10)                                  comment 'How many time this host has been connected',
  natedipaddr          char(50)                                 comment 'This is the IP address as provided by worker itself.  This may be  a NATed IP  Since XWHEP 5.7.3 this length is 50 to comply to IP V6',
  ipaddr               char(50)                                 comment 'This is the IP address obtained at connexion time.  This is set by server at connexion time.  This may be different from NATed IP.  Since XWHEP 5.7.3 this length is 50 to comply to IP V6',
  hwaddr               char(20)                                 comment 'MAC address',
  timezone             char(50)                                 comment 'Time zone',
  os                   char(20)                                 comment 'OS name',
  osversion            char(50)                                 comment 'OS version   Since XWHEP 6.0.0',
  javaversion          char(50)                                 comment 'Java version   Since XWHEP 6.0.0',
  javadatamodel        int(4)                                   comment 'Java data model (32 or 64 bits)  Since XWHEP 6.0.0',
  cputype              char(20)                                 comment 'CPU name',
  cpunb                int(2)                                   comment 'CPU counter',
  cpumodel             char(50)                                 comment 'CPU model',
  cpuspeed             bigint                  default 0        comment 'CPU speed',
  totalmem             bigint                  default 0        comment 'Total RAM',
  totalswap            bigint                  default 0        comment 'Total SWAP',
  totaltmp             bigint                  default 0        comment 'Total space on tmp partition',
  freetmp              bigint                  default 0        comment 'Free space on tmp partition',
  timeShift            int(15)                                  comment 'Time diff from host to server',
  avgping              int(20)                                  comment 'Average ping to server',
  nbping               int(10)                                  comment 'Ping amount to server',
  uploadbandwidth      float                                    comment 'Upload bandwidth usage (in Mb/s)',
  downloadbandwidth    float                                    comment 'Download bandwidth usage (in Mb/s)',
  ownerUID             char(50)                                 comment 'User UID',
  accessRights         int(4)                  default 0x700    comment 'This defines access rights "a la" linux FS  See xtremweb.common.XWAccessRights.java  Since 5.8.0',
  cpuLoad              int(3)                  default 50       comment 'This defines the percentage of CPU usable by the worker  Since 8.0.0',
  project              char(50)                                 comment 'Project this worker wants to limit its participation  In practice, this is a usergroup name  How it works : if a worker runs under an identity that belongs to a usergroup  This worker can execute any public or group job for the users of its group.  This w',  --  Project this worker wants to limit its participation  In practice, this is a usergroup name  How it works : if a worker runs under an identity that belongs to a usergroup  This worker can execute any public or group job for the users of its group.  This worker may want to run group jobs only. It can do so by setting project to its usergroup label
  active               char(5)                 default 'true'   comment 'This flag tells whether this host may be used   If a host has ever generated any error, this flag is automatically set to false  So that we don"t use faulty worker',
  available            char(5)                 default 'false'  comment 'This flag tells whether this host may run jobs accordingly to its local activation policy  This flag is provided by worker with alive signal',
  incomingconnections  char(5)                 default 'false'  comment 'This flag tells whether this host accepts to run jobs that listen for incoming connections  Since 8.0.0',
  acceptbin            char(5)                 default 'true'   comment 'This flag tells whether this host accept application binary   If false, this host accept to execute services written in java only',
  version              char(50)      not null                   comment 'This is this host XWHEP software version',
  traces               char(5)                 default 'false'  comment 'This flag tells whether this host is collecting traces (CPU, memory, disk usage)',
  isdeleted            char(5)                 default 'false'  comment 'True if this row has been deleted',
  pilotjob             char(5)                 default 'false'  comment 'XWHEP 5.7.3 : this is set to true if the worker is run on SG ressource (e.g. EGEE)  XWHEP 7.0.0 : this is deprecated; please use sgid instead ',
  sgid                 char(200)                                comment 'XWHEP 7.0.0 : this is the Service Grid Identifier; this is set by the DG 2 SG bridge, if any',
  jobid                char(200)                                comment 'XWHEP 7.2.0 : this is a job URI; this is for SpeQuLoS (EDGI/JRA2).   If this is set, the worker will receive this job in priority,  If available, and according to the match making - CPU, OS...  This has a higher priority than batchid',
  batchid              char(200)                                comment 'XWHEP 7.2.0 : this is a job group URI; this is for SpeQuLoS (EDGI/JRA2).  If this is set, the worker will receive a job from this group in priority,  If any, and according to the match making - CPU, OS...  This has a lower priority than jobid',
  userproxy            char(200)                                comment 'XWHEP 7.0.0 this is not used',
  sharedapps           varchar(200)                             comment 'Optional, list of coma separated applications shared by the worker  Since 8.0.0 ',
  sharedpackages       varchar(200)                             comment 'Optional, list of coma separated libraries shared by the worker  Since 8.0.0 ',
  shareddatas          varchar(200)                             comment 'Optional, list of coma separated data shared by the worker  Since 8.0.0 (not used yet)',
  
  primary key (uid),
  index owner (owneruid)
  )
ENGINE = InnoDB,  comment = 'Hosts: Worker definitions';

show warnings;

-- 
-- since 7.0.0 deleted rows are stored in history table
-- 
create table if not exists  hosts_history  like  hosts;

show warnings;

-- ALTER TABLE hosts_history ADD PRIMARY KEY USING HASH (uid);


-- 
-- next aims to group jobs
-- sessions are automatically erased (and all its jobs) when the client disconnect
-- 
create table if not exists  sessions  (
  uid           char(50)  not null                   comment 'Primary key',
  name          char(50)  not null                   comment 'Session name',
  accessRights  int(4)              default 0x700    comment 'Since 5.8.0  This defines access rights "a la" linux FS  See xtremweb.common.XWAccessRights.java',
  ownerUID      char(50)  not null                   comment 'Owner (user) UID',
  isdeleted     char(5)             default 'false'  comment 'True if this row has been deleted',
  
  primary key (uid),
  index owner (owneruid)
  )
ENGINE = InnoDB,  comment = 'Sessions: Group jobs';

show warnings;

-- 
-- since 7.0.0 deleted rows are stored in history table
-- 
create table if not exists  sessions_history  like  sessions;

show warnings;

-- ALTER TABLE sessions_history ADD PRIMARY KEY USING HASH (uid);


-- 
-- next contains task definition
-- a task is a work associated to a worker
-- tasks can not be submitted by users : they must submit jobs
-- tasks are automatically created by server
-- each work has a set of associated tasks, at least one
-- a new task is created each time a task is lost
-- 
create table if not exists  tasks  (
  uid            char(50)  not null                   comment 'Primary key',
  ownerUID       char(50)                             comment 'Since 5.8.0',
  workUID        char(50)  not null                   comment 'Since 8.0.0',
  accessRights   int(4)              default 0x700    comment 'Since 5.8.0  This defines access rights "a la" linux FS  See xtremweb.common.XWAccessRights.java',
  hostUID        char(50)                             comment 'Host UID',
  trial          int(11)                              comment 'Instanciation counter',
  status         char(20)                             comment 'Status. see common/XWStatus.java',
  InsertionDate  datetime                             comment 'When the server put this task into queue',
  StartDate      datetime                             comment 'First instanciation date',
  LastStartDate  datetime                             comment 'Last instanciation date',
  AliveCount     int(11)                              comment 'Is it necessary ?',
  LastAlive      datetime                             comment 'Is it necessary ?',
  removalDate    datetime                             comment 'When this task has been removed',
  duration       bigint              default 0        comment 'Last instanciation duration',
  isdeleted      char(5)             default 'false'  comment 'True if row has been deleted ',
  
  primary key (uid),
  index hostuid  (hostuid),
  index owner (owneruid)
  )
ENGINE = InnoDB,  comment = 'Task definition';

show warnings;

CREATE INDEX fk_tasks_works_idx ON tasks (workUID ASC); 
 
-- 
-- since 7.0.0 deleted rows are stored in history table
-- 
create table if not exists  tasks_history  like  tasks;

show warnings;

-- ALTER TABLE tasks_history ADD PRIMARY KEY USING HASH (uid);


-- 
-- next used to store traces from workers (to trace CPU, RAM, Disk etc. activities)
-- this is not used any more
-- 
create table if not exists  traces  (
  uid           char(50)   not null                   comment 'Primary key',
  hostUID       char(50)   not null  default ''       comment 'Host UID',
  login         char(25)   not null  default ''       comment '',
  arrivalDate   datetime   not null                   ,
  startDate     datetime   not null                   ,
  endDate       datetime   not null                   ,
  ownerUID      char(50)                              comment 'Since 5.8.0',
  accessRights  int(4)               default 0x700    comment 'Since 5.8.0  This defines access rights "a la" linux FS  See xtremweb.common.XWAccessRights.java',
  data          char(200)  not null  default ''       comment 'Data URI',
  isdeleted     char(5)              default 'false'  comment 'True if this row has been deleted',
  
  primary key (uid),
  index hostuid  (hostuid),
  index owner (owneruid)
  )
ENGINE = InnoDB,  comment = 'Traces from workers (to trace CPU, RAM, Disk etc. activities';

show warnings;

-- 
-- since 7.0.0 deleted rows are stored in history table
-- 
create table if not exists  traces_history  like  traces;

show warnings;

-- ALTER TABLE traces_history ADD PRIMARY KEY USING HASH (uid);


-- 
-- next contains user definitions
-- 
create table if not exists  users  (
  uid           char(50)   not null                   comment 'Primary key',
  usergroupUID  char(50)                              comment 'Optionnal. user group UID',
  nbJobs        int(15)              default 0        comment 'Completed jobs counter. updated on work completion',
  pendingJobs   int(15)              default 0        comment 'Pending jobs counter. updated on work submission',
  runningJobs   int(15)              default 0        comment 'Running jobs counter. updated on work request',
  errorJobs     int(15)              default 0        comment 'Error jobs counter. updated on job error',
  usedCpuTime   bigint               default 0        comment 'Average execution time. updated on work completion',
  certificate   text                                  comment 'This is the X.509 proxy file content',
  ownerUID      char(50)                              comment 'Since 5.8.0',
  accessRights  int(4)               default 0x700    comment 'Since 5.8.0  This defines access rights "a la" linux FS  See xtremweb.common.XWAccessRights.java',
  login         char(250)  not null                   comment 'User login. if your change length, don t forget to change UserInterface.USERLOGINLENGTH',
  password      char(50)   not null  default ''       comment 'User passworkd',
  email         char(50)   not null  default ''       comment 'User email',
  fname         char(25)                              comment 'Optionnal, user first name',
  lname         char(25)                              comment 'Optionnal, user last name',
  country       char(25)                              comment 'User country',
  rights        char(20)             default 'none'   comment 'User rights. see common/UserRights.java',
  challenging   char(5)              default 'false'  comment 'True if this user connect using private/public keys pair',
  isdeleted     char(5)              default 'false'  comment 'True if this row has been deleted',
  
  primary key (uid),
  constraint unique index users_uniq_login(login),
  index groupuid (usergroupuid),
  index owner (owneruid)
  )
ENGINE = InnoDB,  comment = 'User definitions';

show warnings;

-- 
-- since 7.0.0 deleted rows are stored in history table
-- 
create table if not exists  users_history  like  users;

show warnings;

-- ALTER TABLE users_history ADD PRIMARY KEY USING HASH (uid);
-- ALTER TABLE users_history ADD CONSTRAINT UNIQUE INDEX (login):


-- 
-- next contains group of users
-- 
create table if not exists  usergroups  (
  uid           char(50)   not null                   comment 'Primary key',
  label         char(50)                              comment 'User group label',
  ownerUID      char(50)                              comment 'Since 5.8.0',
  accessRights  int(4)               default 0x700    comment 'Since 5.8.0  This defines access rights "a la" linux FS  See xtremweb.common.XWAccessRights.java',
  webpage       char(200)                             comment 'Application web page',
  project       char(5)              default 'true'   comment 'True if this can be a "project"  This is always true, except for worker and administrator user groups',
  isdeleted     char(5)              default 'false'  comment 'True if this row has been deleted',
  
  primary key (uid),
  constraint unique index usergroups_uniq_label(label),
  index owner(owneruid)
  )
ENGINE = InnoDB,  comment = 'Usergroups: Group of users';

show warnings;

-- 
-- since 7.0.0 deleted rows are stored in history table
-- 
create table if not exists  usergroups_history  like  usergroups;

show warnings;

-- ALTER TABLE usergroups_history ADD PRIMARY KEY USING HASH (uid);


-- 
-- next contains work definitions as submitted by users
-- 
create table if not exists  works  (
  uid                char(50)      not null                   comment 'Primary key',
  userproxy          char(200)                                comment 'This is the X.509 user proxy URI to identify the owner of this work. this is not a certificate',
  expectedhostUID    char(50)                                 comment 'Optionnal. expected host UID',
  accessRights       int(4)                  default 0x700    comment 'This defines access rights "a la" linux FS  See xtremweb.common.XWAccessRights.java',
  sgid               char(200)                                comment 'XWHEP 7.2.0 : this is the Service Grid Identifier; this is set by the DG 2 SG bridge, if any',
  sessionUID         char(50)                                 comment 'Optionnal. session UID',
  groupUID           char(50)                                 comment 'Optionnal. group UID (we call it "groupUID" since "group" is a MySql reserved word)  This is not an usergroup but a group !',
  label              char(150)                                comment 'Optionnal. user label',
  appUID             char(50)                                 comment 'Application UID',
  ownerUID           char(50)                                 comment 'User UID',
  status             char(20)                                 comment 'Status. see common/XWStatus.java',
  wallclocktime      int(10)                                  comment 'Wallclocktime : how many seconds a job can be computed  The job is stopped as the wall clocktime is reached  If wallclocktime < 0, the job is not stopped  Since 8.2.0',
  maxRetry           int(3)                                   comment 'How many time should we try to compute',
  retry              int(3)                                   comment 'How many time have we tried to compute  Since 8.0.0',
  diskSpace          int(10)                 default 0        comment 'Optionnal. disk space needed  This is in Mb',
  minMemory          int(10)                 default 0        comment 'Optionnal. minimum memory needed',
  minCPUSpeed        int(10)                 default 0        comment 'Optionnal. minimum CPU speed needed',
  returnCode         int(3)                                   comment 'Application return code',
  server             char(200)                                comment 'For replication',
  cmdLine            char(255)                                comment 'This work command line to provide to application',
  listenport         char(200)                                comment 'The job may be a server that needs to listen to some ports  This is a comma separated integer list  Privileged ports (lower than 1024) are ignored  Default : null (no port)  Since 8.0.0',
  smartsocketaddr    text                                     comment 'The SmartSockets addresses to connect to the listened ports  This is set if listenport != null  This is a comma separated SmartSockets addresses list  Default : null  Since 8.0.0',
  smartsocketclient  text                                     comment 'This is the column index of a semicolon list containing tuple of   SmartSockets address and local port.  This helps a job running on XWHEP worker side to connect to a server  Like application running on XWHEP client side.  E.g. "Saddr1, port1; Saddr2, por',  --  This is the column index of a semicolon list containing tuple of   SmartSockets address and local port.  This helps a job running on XWHEP worker side to connect to a server  Like application running on XWHEP client side.  E.g. "Saddr1, port1; Saddr2, port2"   Default : null  Since 8.0.0
  stdinURI           char(200)                                comment 'Data URI. this is the STDIN. If not set, apps.stdin is used by default  NULLUID may be used to force no STDIN even if apps.stdin is defined  See common/UID.java#NULLUID',
  dirinURI           char(200)                                comment 'Data URI. this is the DIRIN. If not set, apps.dirin is used by default  NULLURI may be used to force no DIRIN even if apps.dirin is defined  See common/UID.java#NULLUID  This is installed before apps.basedirin to ensure this does not override  Any of the ',  --  Data URI. this is the DIRIN. If not set, apps.dirin is used by default  NULLURI may be used to force no DIRIN even if apps.dirin is defined  See common/UID.java#NULLUID  This is installed before apps.basedirin to ensure this does not override  Any of the apps.basedirin files
  resultURI          char(200)                                comment 'Data URI',
  arrivalDate        datetime                                 comment 'When the server received this work',
  completedDate      datetime                                 comment 'When this work has been completed',
  resultDate         datetime                                 comment 'When this work result has been available',
  readydate          datetime                                 comment 'When this work has been downloaded by the worker  Since 8.0.0',
  datareadydate      datetime                                 comment 'When this work data have been downloaded by the worker  Since 8.0.0',
  compstartdate      datetime                                 comment 'When this work has been started on worker  Since 8.0.0',
  compenddate        datetime                                 comment 'When this work work has been ended on worker  Since 8.0.0',
  error_msg          char(255)                                comment 'Error message',
  sendToClient       char(5)                 default 'false'  comment 'Used for replication',
  local              char(5)                 default 'true'   comment 'Used for replication',
  active             char(5)                 default 'true'   comment 'Used for replication',
  replicated         char(5)                 default 'false'  comment 'Optionnal. used for replication',
  isService          char(5)                 default 'false'  comment 'Is it a service (see apps.isService)',
  isdeleted          char(5)                 default 'false'  comment 'True if row has been deleted ',
  envvars            varchar(200)                             comment 'Opional,  Since 8.0.0',
  
  primary key (uid),
  index works_status_active (status, active),
  index app (appuid),
  index session_uid (sessionuid),
  index group_uid (groupuid),
  index owner (owneruid)
  )
ENGINE = InnoDB,  comment = 'Work definitions as submitted by users';

show warnings;

CREATE INDEX fk_works_hosts_idx ON works (expectedhostUID); 
 
-- 
-- since 7.0.0 deleted rows are stored in history table
-- 
create table if not exists  works_history  like  works;

show warnings;

-- ALTER TABLE works_history ADD PRIMARY KEY USING HASH (uid);
-- ALTER TABLE works_history ADD INDEX (appuid);


-- 
-- Insert a priviliged user to be able to manage your platform
-- 
-- 

-- insert into usergroups (uid,label,project) values("@ADMINGROUPUID@","admin", "false");
-- insert into usergroups (uid,label,project) values("@PUBLICWORKERSGROUPUID@","publicworkers", "false");
-- accessrights = 1792 = 0x700
-- insert into users (uid,login,password,fname,lname,email,rights,owneruid,accessrights) values ("@ADMINUID@","@DBADMIN@","@DBADMINPASSWORD@", "@DBADMINFNAME@", "@DBADMINLNAME@", "@DBADMINEMAIL@","SUPER_USER","@ADMINUID@","1792");
-- insert into users (uid,login,password,fname,lname,email,rights,owneruid,accessrights) values ("@WORKERUID@","@DBWORKER@","@DBWORKERPASSWORD@", "@DBADMINFNAME@", "@DBADMINLNAME@", "@DBADMINEMAIL@","WORKER_USER","@ADMINUID@","1792");

-- 
-- if your dispatcher complains about handshake, try this
-- set password for xtremweb@localhost = old_password('xwpassword');



-- 
-- Finally, allow access to your database
-- There may be error messages if you don't have enough privileges
-- If so, contact you DB amdinistrator
-- 

-- GRANT ALL PRIVILEGES ON * TO @DBUSER@@localhost IDENTIFIED BY '@DBUSERPASSWORD@' WITH GRANT OPTION;
-- GRANT ALL PRIVILEGES ON * TO @DBUSER@@localhost.localdomain IDENTIFIED BY '@DBUSERPASSWORD@' WITH GRANT OPTION;
-- GRANT ALL PRIVILEGES ON * TO @DBUSER@@'@XWHOST@' IDENTIFIED BY '@DBUSERPASSWORD@' WITH GRANT OPTION;
-- set password for @DBUSER@@localhost = old_password('@DBUSERPASSWORD@');


-- 
-- End Of File
-- 
