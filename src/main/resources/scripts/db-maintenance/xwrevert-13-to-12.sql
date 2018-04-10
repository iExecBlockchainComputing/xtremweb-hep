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

ALTER TABLE  datas DROP   COLUMN shasum;
ALTER TABLE  datas_history DROP   COLUMN shasum;

--
-- remove status FAILED
--
update table works set statusId='5', status='ERROR' where statusId='14';
DELETE FROM statuses where statusId='14';


--
-- End Of File
--
