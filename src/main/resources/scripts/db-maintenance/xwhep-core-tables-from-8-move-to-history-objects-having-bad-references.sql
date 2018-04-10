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
--  XtremWeb-HEP Core Tables :  Move to history objects having bad references
--
-- ===========================================================================
delimiter //


drop procedure if exists move_to_history_objects_having_bad_references //


create procedure move_to_history_objects_having_bad_references()

begin
  
  declare end_cursor int;
  declare v_uid      char(50);
  declare v_total    int unsigned;
  
  -- -------------------------------------------------------------------------
  -- Users      with no or bad owner
  -- -------------------------------------------------------------------------
  declare users_cursor cursor for
    select   users1.uid
    from     users as users1
    where    (users1.ownerUID is null) or
             (users1.ownerUID not in (select users2.uid from users as users2));
  
  -- -------------------------------------------------------------------------
  -- Usergroups with no or bad owner
  -- -------------------------------------------------------------------------
  declare usergroups_cursor cursor for
    select   usergroups.uid
    from     usergroups
    where    (usergroups.ownerUID is null) or
             (usergroups.ownerUID not in (select users.uid from users));
  
  -- -------------------------------------------------------------------------
  -- Hosts      with no or bad owner
  --                    or bad project
  -- -------------------------------------------------------------------------
  declare hosts_cursor cursor for
    select   hosts.uid
    from     hosts
    where    (hosts.ownerUID is null) or
             (hosts.ownerUID not in (select users.uid from users)) or
           ( (hosts.project  <> '') and
             (hosts.project  not in (select usergroups.label from usergroups)) );
  
  -- -------------------------------------------------------------------------
  -- Traces     with no or bad owner
  --              or no or bad host
  -- -------------------------------------------------------------------------
  declare traces_cursor cursor for
    select   traces.uid
    from     traces
    where    (traces.ownerUID is null) or
             (traces.ownerUID not in (select users.uid from users)) or
             (traces.hostUID  is null) or
             (traces.hostUID  not in (select hosts.uid from hosts));
  
  -- -------------------------------------------------------------------------
  -- Datas      with no or bad owner
  -- -------------------------------------------------------------------------
  declare datas_cursor cursor for
    select   datas.uid
    from     datas
    where    (datas.ownerUID is null) or
             (datas.ownerUID not in (select users.uid from users));
  
  -- -------------------------------------------------------------------------
  -- Apps       with no or bad owner
  -- -------------------------------------------------------------------------
  declare apps_cursor cursor for
    select   apps.uid
    from     apps
    where    (apps.ownerUID is null) or
             (apps.ownerUID not in (select users.uid from users));
  
  -- -------------------------------------------------------------------------
  -- Sessions   with no or bad owner
  -- -------------------------------------------------------------------------
  declare sessions_cursor cursor for
    select   sessions.uid
    from     sessions
    where    (sessions.ownerUID is null) or
             (sessions.ownerUID not in (select users.uid from users));
  
  -- -------------------------------------------------------------------------
  -- Groups     with no or bad owner
  --                    or bad session
  -- -------------------------------------------------------------------------
  declare groups_cursor cursor for
    select   groups.uid
    from     groups
    where    (groups.ownerUID   is null) or
             (groups.ownerUID   not in (select users.uid    from users   )) or
             (groups.sessionUID not in (select sessions.uid from sessions));
  
  -- -------------------------------------------------------------------------
  -- Works      with no or bad owner
  --              or no or bad application
  --                    or bad expectedhost
  --                    or bad session
  --                    or bad group
  -- -------------------------------------------------------------------------
  declare works_cursor cursor for
    select   works.uid
    from     works
    where    (works.ownerUID        is null) or
             (works.ownerUID        not in (select users.uid    from users   )) or
             (works.appUID          is null) or
             (works.appUID          not in (select apps.uid     from apps    )) or
             (works.expectedhostUID not in (select hosts.uid    from hosts   )) or
             (works.sessionUID      not in (select sessions.uid from sessions)) or
             (works.groupUID        not in (select groups.uid   from groups  ));
  
  -- -------------------------------------------------------------------------
  -- Tasks      with no or bad owner
  --              or no or bad work
  --                    or bad host
  -- -------------------------------------------------------------------------
  declare tasks_cursor cursor for
    select   tasks.uid
    from     tasks
    where    (tasks.ownerUID is null) or
             (tasks.ownerUID not in (select users.uid from users)) or
             (tasks.workUID  is null) or
             (tasks.workUID  not in (select works.uid from works)) or
             (tasks.hostUID  not in (select hosts.uid from hosts));
  
  -- -------------------------------------------------------------------------
  -- Handler for 'End of rows'
  -- -------------------------------------------------------------------------
  declare continue handler for not found
          set end_cursor = true;
  
  
  -- -------------------------------------------------------------------------
  -- Users with no or bad owner
  -- -------------------------------------------------------------------------
  set v_total    = 0;
  set end_cursor = false;
  
  open users_cursor;
  
  read_users: loop
    
    fetch users_cursor into v_uid;
    
    if end_cursor then
      leave read_users;
    end if;
    
    set v_total = v_total + 1;
    
    start transaction;
    insert into users_history select * from users where uid = v_uid;
    delete                             from users where uid = v_uid;
    commit;
    
  end loop;
  
  close users_cursor;
  
  select concat('Users      moved to history :  ',
                cast(v_total as char)) as '';
  
  
  -- -------------------------------------------------------------------------
  -- Usergroups with no or bad owner
  -- -------------------------------------------------------------------------
  set v_total    = 0;
  set end_cursor = false;
  
  open usergroups_cursor;
  
  read_usergroups: loop
    
    fetch usergroups_cursor into v_uid;
    
    if end_cursor then
      leave read_usergroups;
    end if;
    
    set v_total = v_total + 1;
    
    start transaction;
    insert into usergroups_history select * from usergroups where uid = v_uid;
    delete                                  from usergroups where uid = v_uid;
    commit;
    
  end loop;
  
  close usergroups_cursor;
  
  select concat('Usergroups moved to history :  ', 
                cast(v_total as char)) as '';
  
  
  -- -------------------------------------------------------------------------
  -- Hosts      with no or bad owner
  -- -------------------------------------------------------------------------
  set v_total    = 0;
  set end_cursor = false;
  
  open hosts_cursor;
  
  read_hosts: loop
    
    fetch hosts_cursor into v_uid;
    
    if end_cursor then
      leave read_hosts;
    end if;
    
    set v_total = v_total + 1;
    
    start transaction;
    insert into hosts_history select * from hosts where uid = v_uid;
    delete                             from hosts where uid = v_uid;
    commit;
    
  end loop;
  
  close hosts_cursor;
  
  select concat('Hosts      moved to history :  ', 
                cast(v_total as char)) as '';
  
  
  -- -------------------------------------------------------------------------
  -- Traces     with no or bad owner
  --              or no or bad host
  -- -------------------------------------------------------------------------
  set v_total    = 0;
  set end_cursor = false;
  
  open traces_cursor;
  
  read_traces: loop
    
    fetch traces_cursor into v_uid;
    
    if end_cursor then
      leave read_traces;
    end if;
    
    set v_total = v_total + 1;
    
    start transaction;
    insert into traces_history select * from traces where uid = v_uid;
    delete                              from traces where uid = v_uid;
    commit;
    
  end loop;
  
  close traces_cursor;
  
  select concat('Traces     moved to history :  ', 
                cast(v_total as char)) as '';
  
  
  -- -------------------------------------------------------------------------
  -- Datas      with no or bad owner
  -- -------------------------------------------------------------------------
  set v_total    = 0;
  set end_cursor = false;
  
  open datas_cursor;
  
  read_datas: loop
    
    fetch datas_cursor into v_uid;
    
    if end_cursor then
      leave read_datas;
    end if;
    
    set v_total = v_total + 1;
    
    start transaction;
    insert into datas_history select * from datas where uid = v_uid;
    delete                             from datas where uid = v_uid;
    commit;
    
  end loop;
  
  close datas_cursor;
  
  select concat('Datas      moved to history :  ', 
                cast(v_total as char)) as '';
  
  
  -- -------------------------------------------------------------------------
  -- Apps       with no or bad owner
  -- -------------------------------------------------------------------------
  set v_total    = 0;
  set end_cursor = false;
  
  open apps_cursor;
  
  read_apps: loop
    
    fetch apps_cursor into v_uid;
    
    if end_cursor then
      leave read_apps;
    end if;
    
    set v_total = v_total + 1;
    
    start transaction;
    insert into apps_history select * from apps where uid = v_uid;
    delete                            from apps where uid = v_uid;
    commit;
    
  end loop;
  
  close apps_cursor;
  
  select concat('Apps       moved to history :  ', 
                cast(v_total as char)) as '';
  
  
  -- -------------------------------------------------------------------------
  -- Sessions   with no or bad owner
  --                    or bad session
  -- -------------------------------------------------------------------------
  set v_total    = 0;
  set end_cursor = false;
  
  open sessions_cursor;
  
  read_sessions: loop
    
    fetch sessions_cursor into v_uid;
    
    if end_cursor then
      leave read_sessions;
    end if;
    
    set v_total = v_total + 1;
    
    start transaction;
    insert into sessions_history select * from sessions where uid = v_uid;
    delete                                from sessions where uid = v_uid;
    commit;
    
  end loop;
  
  close sessions_cursor;
  
  select concat('Sessions   moved to history :  ', 
                cast(v_total as char)) as '';
  
  
  -- -------------------------------------------------------------------------
  -- Groups     with no or bad owner
  -- -------------------------------------------------------------------------
  set v_total    = 0;
  set end_cursor = false;
  
  open groups_cursor;
  
  read_groups: loop
    
    fetch groups_cursor into v_uid;
    
    if end_cursor then
      leave read_groups;
    end if;
    
    set v_total = v_total + 1;
    
    start transaction;
    insert into groups_history select * from groups where uid = v_uid;
    delete                              from groups where uid = v_uid;
    commit;
    
  end loop;
  
  close groups_cursor;
  
  select concat('Groups     moved to history :  ', 
                cast(v_total as char)) as '';
  
  
  -- -------------------------------------------------------------------------
  -- Works      with no or bad owner
  --              or no or bad application
  --                    or bad expectedhost
  --                    or bad session
  --                    or bad group
  -- -------------------------------------------------------------------------
  set v_total    = 0;
  set end_cursor = false;
  
  open works_cursor;
  
  read_works: loop
    
    fetch works_cursor into v_uid;
    
    if end_cursor then
      leave read_works;
    end if;
    
    set v_total = v_total + 1;
    
    start transaction;
    insert into works_history select * from works where uid = v_uid;
    delete                             from works where uid = v_uid;
    commit;
    
  end loop;
  
  close works_cursor;
  
  select concat('Works      moved to history :  ', 
                cast(v_total as char)) as '';
  
  
  -- -------------------------------------------------------------------------
  -- Tasks      with no or bad owner
  --              or no or bad work
  --                    or bad host
  -- -------------------------------------------------------------------------
  set v_total    = 0;
  set end_cursor = false;
  
  open tasks_cursor;
  
  read_tasks: loop
    
    fetch tasks_cursor into v_uid;
    
    if end_cursor then
      leave read_tasks;
    end if;
    
    set v_total = v_total + 1;
    
    start transaction;
    insert into tasks_history select * from tasks where uid = v_uid;
    delete                             from tasks where uid = v_uid;
    commit;
    
  end loop;
  
  close tasks_cursor;
  
  select concat('Tasks      moved to history :  ', 
                cast(v_total as char)) as '';
  
end; //


call move_to_history_objects_having_bad_references() //


delimiter ;
