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


drop table if exists  envs;
drop table if exists  envs_history;



ALTER TABLE  works DROP   COLUMN envId;
ALTER TABLE  works DROP   COLUMN maxFreeMassStorage;
ALTER TABLE  works DROP   COLUMN maxFileSize;
ALTER TABLE  works DROP   COLUMN maxMemory;
ALTER TABLE  works DROP   COLUMN maxCpuSpeed;

ALTER TABLE  works ADD    COLUMN wallclocktime int(10)                   comment 'Wallclocktime : how many seconds a job can be computed.  The job is stopped as the wall clocktime is reached.  If < 0, the job is not stopped.';
ALTER TABLE  works ADD    COLUMN diskSpace bigint             default 0  comment 'Optionnal. disk space needed  This is in Mb';

ALTER TABLE  works DROP   COLUMN uploadbandwidth;
ALTER TABLE  works DROP   COLUMN downloadbandwidth;

ALTER TABLE  datas DROP   COLUMN shasum;
ALTER TABLE  datas_history DROP   COLUMN shasum;

--
-- remove status FAILED
--
update works set statusId='5', status='ERROR' where statusId='14';
DELETE FROM statuses where statusId='14';



SET FOREIGN_KEY_CHECKS=0;

drop table userRights;

create table if not exists  userRights  (
  userRightId           tinyint unsigned  not null  primary key,
  userRightName         varchar(254)      not null  unique,
  mtime                 timestamp,
  userRightDescription  varchar(254)
  )
engine  = InnoDB,
comment = 'userRights = Constants for "users"."rights"';

show warnings;

INSERT INTO userRights VALUES
    (0,'NONE','2018-04-11 13:55:25',NULL),
    (1,'INSERTJOB','2018-04-11 13:55:25',NULL),
    (2,'GETJOB','2018-04-11 13:55:25',NULL),
    (3,'INSERTDATA','2018-04-11 13:55:25',NULL),
    (4,'GETDATA','2018-04-11 13:55:25',NULL),
    (5,'GETGROUP','2018-04-11 13:55:25',NULL),
    (6,'GETSESSION','2018-04-11 13:55:25',NULL),
    (7,'GETHOST','2018-04-11 13:55:25',NULL),
    (8,'GETAPP','2018-04-11 13:55:25',NULL),
    (9,'GETUSER','2018-04-11 13:55:25',NULL),
    (10,'UPDATEWORK','2018-04-11 13:55:25','worker can update work for the owner'),(
    11,'WORKER_USER','2018-04-11 13:55:25','worker cannot do everything'),
    (12,'VWORKER_USER','2018-04-11 13:55:25','vworker can take advantage of stickybit'),
    (13,'BROADCAST','2018-04-11 13:55:25','submit one job to all workers'),
    (14,'LISTJOB','2018-04-11 13:55:25',NULL),
    (15,'DELETEJOB','2018-04-11 13:55:25',NULL),
    (16,'LISTDATA','2018-04-11 13:55:25',NULL),
    (17,'DELETEDATA','2018-04-11 13:55:25',NULL),
    (18,'LISTGROUP','2018-04-11 13:55:25',NULL),
    (19,'INSERTGROUP','2018-04-11 13:55:25',NULL),
    (20,'DELETEGROUP','2018-04-11 13:55:25',NULL),
    (21,'LISTSESSION','2018-04-11 13:55:25',NULL),
    (22,'INSERSESSION','2018-04-11 13:55:25',NULL),
    (23,'DELETESESSION','2018-04-11 13:55:25',NULL),
    (24,'LISTHOST','2018-04-11 13:55:25',NULL),
    (25,'LISTUSER','2018-04-11 13:55:25',NULL),
    (26,'LISTUSERGROUP','2018-04-11 13:55:25',NULL),
    (27,'GETUSERGROUP','2018-04-11 13:55:25',NULL),
    (28,'INSERTAPP','2018-04-11 13:55:25',NULL),
    (29,'DELETEAPP','2018-04-11 13:55:25',NULL),
    (30,'LISTAPP','2018-04-11 13:55:25',NULL),
    (31,'STANDARD_USER','2018-04-11 13:55:25','non privileged user'),
    (32,'INSERTUSER','2018-04-11 13:55:25',NULL),
    (33,'DELETEUSER','2018-04-11 13:55:25',NULL),
    (34,'ADVANCED_USER','2018-04-11 13:55:25','privileged user (e.g. user group manager)'),
    (35,'MANDATED_USER','2018-04-11 13:55:25','can work in name of another user'),
    (36,'INSERTHOST','2018-04-11 13:55:25',NULL),
    (37,'DELETEHOST','2018-04-11 13:55:25',NULL),
    (38,'INSERTUSERGROUP','2018-04-11 13:55:25',NULL),
    (39,'DELETEUSERGROUP','2018-04-11 13:55:25',NULL),
    (40,'SUPER_USER','2018-04-11 13:55:25','can do all');


UPDATE users SET userRightId=(select userRightId from userRights where userRightName=users.rights);

SET FOREIGN_KEY_CHECKS=1;

--
-- End Of File
--
