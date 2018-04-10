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
--  XtremWeb-HEP Core Tables :  SQL Query on 'information_schema.columns'
--  generating an SQL script for the copy of the table contents from the
--  schema specified in @SOURCE_SCHEMA into the current schema.
--
--  Due to foreign keys, the order of tables for copy is really important.
--
-- ===========================================================================

select if(@SOURCE_SCHEMA is null, 'select "@SOURCE_SCHEMA is NOT defined"; exit;', '')
       as '' union
select concat('-- Copy contents of schema "', @SOURCE_SCHEMA, '" into current schema');


-- ---------------------------------------------------------------------------
-- Temporary table for insert statements from live to history tables
-- ---------------------------------------------------------------------------
drop temporary table if exists temp_insert_into_history_tables;

create temporary table temp_insert_into_history_tables (primary key (table_name)) as
select table_name,
       concat('insert into ', table_name, '_history (',
              group_concat(column_name separator ', '),
              ') select ',
              group_concat(column_name separator ', '),
                ' from ', table_schema, '.', table_name) as insert_statement
from   information_schema.columns
where  (table_schema = @SOURCE_SCHEMA) and
       (table_name not like '%_history')
group  by table_name;

-- ---------------------------------------------------------------------------
-- Temporary table for insert statements into all tables
-- ---------------------------------------------------------------------------
drop temporary table if exists temp_insert_into_all_tables;

create temporary table temp_insert_into_all_tables (primary key (table_name)) as
select table_name,
       concat('insert into ', table_name, '(',
              group_concat(column_name separator ', '),
              ') select ',
              group_concat(column_name separator ', '),
                ' from ', table_schema, '.', table_name) as insert_statement
from   information_schema.columns
where  table_schema = @SOURCE_SCHEMA
group  by table_name;


-- ---------------------------------------------------------------------------
select '-- Live tables' as ' ';
-- ---------------------------------------------------------------------------


-- ---------------------------------------------------------------------------
-- Table 'versions'
-- ---------------------------------------------------------------------------
select concat(insert_statement, ';') as ' '
from   temp_insert_into_all_tables
where  table_name = 'versions';


-- ---------------------------------------------------------------------------
-- Users      with no or bad owner
-- ---------------------------------------------------------------------------
select insert_statement as ' '
from   temp_insert_into_history_tables
where  table_name = 'users'
union
select concat('where (', @SOURCE_SCHEMA, '.users.ownerUID is null) or')
union
select concat('      (', @SOURCE_SCHEMA, '.users.ownerUID not in ',
                      '(select users2.uid from ', @SOURCE_SCHEMA, '.users as users2));');

-- ---------------------------------------------------------------------------
-- Tables 'users' and 'usergroups' have reciprocal foreign keys, which have to
-- be disabled for initial copy
-- ---------------------------------------------------------------------------
select 'SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;' as '';

-- ---------------------------------------------------------------------------
-- Users      with good owner
-- ---------------------------------------------------------------------------
select insert_statement as ' '
from   temp_insert_into_all_tables
where  table_name = 'users'
union
select concat('where (', @SOURCE_SCHEMA, '.users.ownerUID is not null) and')
union
select concat('      (', @SOURCE_SCHEMA, '.users.ownerUID in ',
                      '(select users2.uid from ', @SOURCE_SCHEMA, '.users as users2));');

-- ---------------------------------------------------------------------------
-- Re-enable foreign key checks
-- ---------------------------------------------------------------------------
select 'SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;' as '';


-- ---------------------------------------------------------------------------
-- Usergroups with no or bad owner
-- ---------------------------------------------------------------------------
select insert_statement as ' '
from   temp_insert_into_history_tables
where  table_name = 'usergroups'
union
select concat('where (', @SOURCE_SCHEMA, '.usergroups.ownerUID is null) or')
union
select concat('      (', @SOURCE_SCHEMA, '.usergroups.ownerUID not in ',
                      '(select users.uid from users));');

-- ---------------------------------------------------------------------------
-- Usergroups with good owner
-- ---------------------------------------------------------------------------
select insert_statement as ' '
from   temp_insert_into_all_tables
where  table_name = 'usergroups'
union
select concat('where (', @SOURCE_SCHEMA, '.usergroups.ownerUID is not null) and')
union
select concat('      (', @SOURCE_SCHEMA, '.usergroups.ownerUID in ',
                      '(select users.uid from users));');


-- ---------------------------------------------------------------------------
-- Hosts      with no or bad owner
-- ---------------------------------------------------------------------------
select insert_statement as ' '
from   temp_insert_into_history_tables
where  table_name = 'hosts'
union
select concat('where (', @SOURCE_SCHEMA, '.hosts.ownerUID is null) or')
union
select concat('      (', @SOURCE_SCHEMA, '.hosts.ownerUID not in ',
                      '(select users.uid from users));');

-- ---------------------------------------------------------------------------
-- Hosts      with good owner
-- ---------------------------------------------------------------------------
select insert_statement as ' '
from   temp_insert_into_all_tables
where  table_name = 'hosts'
union
select concat('where (', @SOURCE_SCHEMA, '.hosts.ownerUID is not null) and')
union
select concat('      (', @SOURCE_SCHEMA, '.hosts.ownerUID in ',
                      '(select users.uid from users));');


-- ---------------------------------------------------------------------------
-- Traces     with no or bad owner
--              or no or bad host
-- ---------------------------------------------------------------------------
select insert_statement as ' '
from   temp_insert_into_history_tables
where  table_name = 'traces'
union
select concat('where (', @SOURCE_SCHEMA, '.traces.ownerUID is null) or')
union
select concat('      (', @SOURCE_SCHEMA, '.traces.ownerUID not in ',
                      '(select users.uid from users)) or')
union
select concat('      (', @SOURCE_SCHEMA, '.traces.hostUID  is null) or')
union
select concat('      (', @SOURCE_SCHEMA, '.traces.hostUID  not in ',
                      '(select hosts.uid from hosts));');

-- ---------------------------------------------------------------------------
-- Traces     with good owner and host
-- ---------------------------------------------------------------------------
select insert_statement as ' '
from   temp_insert_into_all_tables
where  table_name = 'traces'
union
select concat('where (', @SOURCE_SCHEMA, '.traces.ownerUID is not null) and')
union
select concat('      (', @SOURCE_SCHEMA, '.traces.ownerUID in ',
                      '(select users.uid from users)) and')
union
select concat('      (', @SOURCE_SCHEMA, '.traces.hostUID  is not null) and')
union
select concat('      (', @SOURCE_SCHEMA, '.traces.hostUID  in ',
                      '(select hosts.uid from hosts));');


-- ---------------------------------------------------------------------------
-- Datas      with no or bad owner
-- ---------------------------------------------------------------------------
select insert_statement as ' '
from   temp_insert_into_history_tables
where  table_name = 'datas'
union
select concat('where (', @SOURCE_SCHEMA, '.datas.ownerUID is null) or')
union
select concat('      (', @SOURCE_SCHEMA, '.datas.ownerUID not in ',
                      '(select users.uid from users));');

-- ---------------------------------------------------------------------------
-- Datas      with good owner
-- ---------------------------------------------------------------------------
select insert_statement as ' '
from   temp_insert_into_all_tables
where  table_name = 'datas'
union
select concat('where (', @SOURCE_SCHEMA, '.datas.ownerUID is not null) and')
union
select concat('      (', @SOURCE_SCHEMA, '.datas.ownerUID in ',
                      '(select users.uid from users));');


-- ---------------------------------------------------------------------------
-- Apps       with no or bad owner
-- ---------------------------------------------------------------------------
select insert_statement as ' '
from   temp_insert_into_history_tables
where  table_name = 'apps'
union
select concat('where (', @SOURCE_SCHEMA, '.apps.ownerUID is null) or')
union
select concat('      (', @SOURCE_SCHEMA, '.apps.ownerUID not in ',
                      '(select users.uid from users));');

-- ---------------------------------------------------------------------------
-- Apps       with good owner
-- ---------------------------------------------------------------------------
select insert_statement as ' '
from   temp_insert_into_all_tables
where  table_name = 'apps'
union
select concat('where (', @SOURCE_SCHEMA, '.apps.ownerUID is not null) and')
union
select concat('      (', @SOURCE_SCHEMA, '.apps.ownerUID in ',
                      '(select users.uid from users));');


-- ---------------------------------------------------------------------------
-- Sessions   with no or bad owner
-- ---------------------------------------------------------------------------
select insert_statement as ' '
from   temp_insert_into_history_tables
where  table_name = 'sessions'
union
select concat('where (', @SOURCE_SCHEMA, '.sessions.ownerUID is null) or')
union
select concat('      (', @SOURCE_SCHEMA, '.sessions.ownerUID not in ',
                      '(select users.uid from users));');

-- ---------------------------------------------------------------------------
-- Sessions   with good owner
-- ---------------------------------------------------------------------------
select insert_statement as ' '
from   temp_insert_into_all_tables
where  table_name = 'sessions'
union
select concat('where (', @SOURCE_SCHEMA, '.sessions.ownerUID is not null) and')
union
select concat('      (', @SOURCE_SCHEMA, '.sessions.ownerUID in ',
                      '(select users.uid from users));');


-- ---------------------------------------------------------------------------
-- Groups     with no or bad owner
--                    or bad session
-- ---------------------------------------------------------------------------
select insert_statement as ' '
from   temp_insert_into_history_tables
where  table_name = 'groups'
union
select concat('where (', @SOURCE_SCHEMA, '.groups.ownerUID   is null) or')
union
select concat('      (', @SOURCE_SCHEMA, '.groups.ownerUID   not in ',
                      '(select users.uid    from users   )) or')
union
select concat('    ( (', @SOURCE_SCHEMA, '.groups.sessionUID is not null) and')
union
select concat('      (', @SOURCE_SCHEMA, '.groups.sessionUID not in ',
                      '(select sessions.uid from sessions)) );');

-- ---------------------------------------------------------------------------
-- Groups     with good owner and session
-- ---------------------------------------------------------------------------
select insert_statement as ' '
from   temp_insert_into_all_tables
where  table_name = 'groups'
union
select concat('where (', @SOURCE_SCHEMA, '.groups.ownerUID   is not null) and')
union
select concat('      (', @SOURCE_SCHEMA, '.groups.ownerUID   in ',
                      '(select users.uid    from users   )) and')
union
select concat('    ( (', @SOURCE_SCHEMA, '.groups.sessionUID is null) or')
union
select concat('      (', @SOURCE_SCHEMA, '.groups.sessionUID in ',
                      '(select sessions.uid from sessions)) );');


-- ---------------------------------------------------------------------------
-- Works      with no or bad owner
--              or no or bad application
--                    or bad expectedhost
--                    or bad session
--                    or bad group
-- ---------------------------------------------------------------------------
select insert_statement as ' '
from   temp_insert_into_history_tables
where  table_name = 'works'
union
select concat('where (', @SOURCE_SCHEMA, '.works.ownerUID        is null) or')
union
select concat('      (', @SOURCE_SCHEMA, '.works.ownerUID        not in ',
                      '(select users.uid    from users   )) or')
union
select concat('      (', @SOURCE_SCHEMA, '.works.appUID          is null) or')
union
select concat('      (', @SOURCE_SCHEMA, '.works.appUID          not in ',
                      '(select apps.uid     from apps    )) or')
union
select concat('    ( (', @SOURCE_SCHEMA, '.works.expectedhostUID is not null) and')
union
select concat('      (', @SOURCE_SCHEMA, '.works.expectedhostUID not in ',
                      '(select hosts.uid    from hosts   )) ) or')
union
select concat('    ( (', @SOURCE_SCHEMA, '.works.sessionUID      is not null) and')
union
select concat('      (', @SOURCE_SCHEMA, '.works.sessionUID      not in ',
                      '(select sessions.uid from sessions)) ) or')
union
select concat('    ( (', @SOURCE_SCHEMA, '.works.groupUID        is not null) and')
union
select concat('      (', @SOURCE_SCHEMA, '.works.groupUID        not in ',
                      '(select groups.uid   from groups  )) );');

-- ---------------------------------------------------------------------------
-- Works      with good owner, application, expectedhost, session and group
-- ---------------------------------------------------------------------------
select insert_statement as ' '
from   temp_insert_into_all_tables
where  table_name = 'works'
union
select concat('where (', @SOURCE_SCHEMA, '.works.ownerUID        is not null) and')
union
select concat('      (', @SOURCE_SCHEMA, '.works.ownerUID        in ',
                      '(select users.uid    from users   )) and')
union
select concat('      (', @SOURCE_SCHEMA, '.works.appUID          is not null) and')
union
select concat('      (', @SOURCE_SCHEMA, '.works.appUID          in ',
                      '(select apps.uid     from apps    )) and')
union
select concat('    ( (', @SOURCE_SCHEMA, '.works.expectedhostUID is null) or')
union
select concat('      (', @SOURCE_SCHEMA, '.works.expectedhostUID in ',
                      '(select hosts.uid    from hosts   )) ) and')
union
select concat('    ( (', @SOURCE_SCHEMA, '.works.sessionUID      is null) or')
union
select concat('      (', @SOURCE_SCHEMA, '.works.sessionUID      in ',
                      '(select sessions.uid from sessions)) ) and')
union
select concat('    ( (', @SOURCE_SCHEMA, '.works.groupUID        is null) or')
union
select concat('      (', @SOURCE_SCHEMA, '.works.groupUID        in ',
                      '(select groups.uid   from groups  )) );');


-- ---------------------------------------------------------------------------
-- Tasks      with no or bad owner
--              or no or bad work
--                    or bad host
-- ---------------------------------------------------------------------------
select insert_statement as ' '
from   temp_insert_into_history_tables
where  table_name = 'tasks'
union
select concat('where (', @SOURCE_SCHEMA, '.tasks.ownerUID is null) or')
union
select concat('      (', @SOURCE_SCHEMA, '.tasks.ownerUID not in ',
                      '(select users.uid from users)) or')
union
select concat('      (', @SOURCE_SCHEMA, '.tasks.workUID  is null) or')
union
select concat('      (', @SOURCE_SCHEMA, '.tasks.workUID  not in ',
                      '(select works.uid from works)) or')
union
select concat('    ( (', @SOURCE_SCHEMA, '.tasks.hostUID  is not null) and')
union
select concat('      (', @SOURCE_SCHEMA, '.tasks.hostUID  not in ',
                      '(select hosts.uid from hosts)) );');

-- ---------------------------------------------------------------------------
-- Tasks      with good owner, work and host
-- ---------------------------------------------------------------------------
select insert_statement as ' '
from   temp_insert_into_all_tables
where  table_name = 'tasks'
union
select concat('where (', @SOURCE_SCHEMA, '.tasks.ownerUID is not null) and')
union
select concat('      (', @SOURCE_SCHEMA, '.tasks.ownerUID in ',
                      '(select users.uid from users)) and')
union
select concat('      (', @SOURCE_SCHEMA, '.tasks.workUID  is not null) and')
union
select concat('      (', @SOURCE_SCHEMA, '.tasks.workUID  in ',
                      '(select works.uid from works)) and')
union
select concat('    ( (', @SOURCE_SCHEMA, '.tasks.hostUID  is null) or')
union
select concat('      (', @SOURCE_SCHEMA, '.tasks.hostUID  in ',
                      '(select hosts.uid from hosts)) );');


-- ---------------------------------------------------------------------------
select '-- History tables' as ' ';
-- ---------------------------------------------------------------------------
select concat(insert_statement, ';') as ' '
from   temp_insert_into_all_tables
where  table_name like '%_history';


-- ---------------------------------------------------------------------------
-- Drop temporary tables
-- ---------------------------------------------------------------------------
drop temporary table if exists temp_insert_into_history_tables;

drop temporary table if exists temp_insert_into_all_tables;
