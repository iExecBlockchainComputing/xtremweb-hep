-- ===========================================================================
--
--  Copyright 2013  E. URBAH
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
--  SQL script permitting to set NOT NULL the foreign keys referencing
--  the new tables
--
-- ===========================================================================

select 'Foreign key "users.userRightId"' as '';
alter table users  modify column userRightId tinyint unsigned not null default 255;
show warnings;

select 'Foreign key "datas.statusId"' as '';
alter table datas  modify column statusId    tinyint unsigned not null default 255;
show warnings;

select 'Foreign key "apps.appTypeId"' as '';
alter table apps   modify column appTypeId   tinyint unsigned not null default 255;
show warnings;

select 'Foreign key "works.statusId"' as '';
alter table works  modify column statusId    tinyint unsigned not null default 255;
show warnings;

select 'Foreign key "tasks.statusId"' as '';
alter table tasks  modify column statusId    tinyint unsigned not null default 255;
show warnings;
