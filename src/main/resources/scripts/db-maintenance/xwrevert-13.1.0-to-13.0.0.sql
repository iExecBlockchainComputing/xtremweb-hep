-- ===========================================================================
--  Copyrights     : CNRS
--  Authors        : Oleg Lodygensky
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
-- ===========================================================================

--

SET FOREIGN_KEY_CHECKS=0;


drop table if exists  marketorders;
drop table if exists  marketorders_history;

ALTER TABLE  works DROP   COLUMN marketOrderIdx;
ALTER TABLE  works DROP   COLUMN requester;
ALTER TABLE  works DROP   COLUMN dataset;
ALTER TABLE  works DROP   COLUMN workerPool;
ALTER TABLE  works DROP   COLUMN emitcost;
ALTER TABLE  works DROP   COLUMN callback;
ALTER TABLE  works DROP   COLUMN beneficiary;
ALTER TABLE  works DROP   COLUMN marketorderUID;
ALTER TABLE  works DROP   COLUMN h2r;
ALTER TABLE  works DROP   COLUMN h2rps;
ALTER TABLE  works DROP   COLUMN workOrderId;


ALTER TABLE  works CHANGE COLUMN replications  replications int(3) default 0        comment 'Optionnal. Amount of expected replications. No replication, if <= 0';
ALTER TABLE  works CHANGE COLUMN sizer         sizer        int(3) default 0        comment 'Optionnal. This is the size of the replica set';
ALTER TABLE  works CHANGE COLUMN totalr        totalr       int(3) default 0        comment 'Optionnal. Current amount of replicas';

ALTER TABLE  hosts DROP   COLUMN ethwalletaddr;
ALTER TABLE  hosts DROP   COLUMN marketorderUID;
ALTER TABLE  hosts DROP   COLUMN hascontributed;
ALTER TABLE  hosts DROP   COLUMN workerpooladdr;

ALTER TABLE  apps  DROP   COLUMN price;

ALTER TABLE  tasks DROP   COLUMN price;

delete from statuses where statusId='15';
delete from statuses where statusId='16';
update works set statusId='5', status='ERROR' where statusId='15' or statusId='16';


drop table userRights;
create table if not exists  userRights  (
  userRightId           tinyint unsigned  not null  primary key,
  userRightName         varchar(254)      not null  unique,
  mtime                 timestamp,
  userRightDescription  varchar(254)
  )
engine  = InnoDB,
comment = 'userRights = Constants for "users"."rights"';

insert into userRights (userRightId, userRightName, userRightDescription) values ( 0, 'NONE',            null);
insert into userRights (userRightId, userRightName, userRightDescription) values ( 1, 'INSERTJOB',       null);
insert into userRights (userRightId, userRightName, userRightDescription) values ( 2, 'GETJOB',          null);
insert into userRights (userRightId, userRightName, userRightDescription) values ( 3, 'INSERTDATA',      null);
insert into userRights (userRightId, userRightName, userRightDescription) values ( 4, 'GETDATA',         null);
insert into userRights (userRightId, userRightName, userRightDescription) values ( 5, 'GETGROUP',        null);
insert into userRights (userRightId, userRightName, userRightDescription) values ( 6, 'GETSESSION',      null);
insert into userRights (userRightId, userRightName, userRightDescription) values ( 7, 'GETHOST',         null);
insert into userRights (userRightId, userRightName, userRightDescription) values ( 8, 'GETAPP',          null);
insert into userRights (userRightId, userRightName, userRightDescription) values ( 9, 'GETUSER',         null);
insert into userRights (userRightId, userRightName, userRightDescription) values (10, 'GETENVELOPE',     null);
insert into userRights (userRightId, userRightName, userRightDescription) values (11, 'UPDATEWORK',      'worker can update work for the owner');
insert into userRights (userRightId, userRightName, userRightDescription) values (12, 'WORKER_USER',     'worker cannot do everything');
insert into userRights (userRightId, userRightName, userRightDescription) values (13, 'VWORKER_USER',    'vworker can take advantage of stickybit');
insert into userRights (userRightId, userRightName, userRightDescription) values (14, 'BROADCAST',       'submit one job to all workers');
insert into userRights (userRightId, userRightName, userRightDescription) values (15, 'LISTJOB',         null);
insert into userRights (userRightId, userRightName, userRightDescription) values (16, 'DELETEJOB',       null);
insert into userRights (userRightId, userRightName, userRightDescription) values (17, 'LISTDATA',        null);
insert into userRights (userRightId, userRightName, userRightDescription) values (18, 'DELETEDATA',      null);
insert into userRights (userRightId, userRightName, userRightDescription) values (19, 'LISTGROUP',       null);
insert into userRights (userRightId, userRightName, userRightDescription) values (20, 'INSERTGROUP',     null);
insert into userRights (userRightId, userRightName, userRightDescription) values (21, 'DELETEGROUP',     null);
insert into userRights (userRightId, userRightName, userRightDescription) values (22, 'LISTSESSION',     null);
insert into userRights (userRightId, userRightName, userRightDescription) values (23, 'INSERTSESSION',    null);
insert into userRights (userRightId, userRightName, userRightDescription) values (24, 'DELETESESSION',   null);
insert into userRights (userRightId, userRightName, userRightDescription) values (25, 'LISTHOST',        null);
insert into userRights (userRightId, userRightName, userRightDescription) values (26, 'LISTUSER',        null);
insert into userRights (userRightId, userRightName, userRightDescription) values (27, 'LISTUSERGROUP',   null);
insert into userRights (userRightId, userRightName, userRightDescription) values (28, 'GETUSERGROUP',    null);
insert into userRights (userRightId, userRightName, userRightDescription) values (29, 'INSERTAPP',       null);
insert into userRights (userRightId, userRightName, userRightDescription) values (30, 'DELETEAPP',       null);
insert into userRights (userRightId, userRightName, userRightDescription) values (31, 'LISTAPP',         null);
insert into userRights (userRightId, userRightName, userRightDescription) values (32, 'LISTENVELOPE',   'non privileged user');
insert into userRights (userRightId, userRightName, userRightDescription) values (33, 'STANDARD_USER',   'non privileged user');
insert into userRights (userRightId, userRightName, userRightDescription) values (34, 'INSERTUSER',      null);
insert into userRights (userRightId, userRightName, userRightDescription) values (35, 'DELETEUSER',      null);
insert into userRights (userRightId, userRightName, userRightDescription) values (36, 'ADVANCED_USER',   'privileged user (e.g. user group manager)');
insert into userRights (userRightId, userRightName, userRightDescription) values (37, 'MANDATED_USER',   'can work in name of another user');
insert into userRights (userRightId, userRightName, userRightDescription) values (38, 'INSERTHOST',      null);
insert into userRights (userRightId, userRightName, userRightDescription) values (39, 'INSERTENVELOPE',  null);
insert into userRights (userRightId, userRightName, userRightDescription) values (40, 'DELETEHOST',      null);
insert into userRights (userRightId, userRightName, userRightDescription) values (41, 'INSERTUSERGROUP', null);
insert into userRights (userRightId, userRightName, userRightDescription) values (42, 'DELETEUSERGROUP', null);
insert into userRights (userRightId, userRightName, userRightDescription) values (43, 'SUPER_USER',      'can do all');


UPDATE users SET userRightId=(select userRightId from userRights where userRightName=users.rights);

SET FOREIGN_KEY_CHECKS=1;


UPDATE users SET userRightId=(select userRightId from userRights where userRightName=users.rights);

SET FOREIGN_KEY_CHECKS=1;

--
-- End Of File
--
