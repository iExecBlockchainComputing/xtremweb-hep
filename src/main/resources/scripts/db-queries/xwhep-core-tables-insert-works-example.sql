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
--  Example stand-alone SQL script inserting in total 6 works for 2 users.
-- 
-- Usergroups :
--      @usergroup_first  is the first usergroup by UID having standard users
--      @usergroup_last   is the last  usergroup by UID having standard users
--
-- Users :
--      @user_first       is a user belonging to group @usergroup_first
--      @user_last        is a user belonging to group @usergroup_last
-- 
--  Applications :
--      @app_shared       is a shared application with no executable
--      @app_java         has OS- and CPU-independant executables
--      @app_binary       has OS- and CPU-specific    executables
-- 
-- ===========================================================================
set @accessRights = 1877;

-- ---------------------------------------------------------------------------
-- Usergroups :
--      @usergroup_first  is the first usergroup by UID having standard users
--      @usergroup_last   is the last  usergroup by UID having standard users
-- ---------------------------------------------------------------------------
set @usergroup_first = ( select distinct
                                     users.usergroupUID
                         from        users
                         inner join  userRights  on  userRights.userRightId = users.userRightId
                         where       (users.usergroupUID       is not null) and
                                     (userRights.userRightName = 'STANDARD_USER')
                         order by    users.usergroupUID asc
                         limit 1 );

set @usergroup_last  = ( select distinct
                                     users.usergroupUID
                         from        users
                         inner join  userRights  on  userRights.userRightId = users.userRightId
                         where       (users.usergroupUID       is not null) and
                                     (userRights.userRightName = 'STANDARD_USER')
                         order by    users.usergroupUID desc
                         limit 1 );

-- ---------------------------------------------------------------------------
-- Users :
--      @user_first       is a user belonging to group @usergroup_first
--      @user_last        is a user belonging to group @usergroup_last
-- ---------------------------------------------------------------------------
set @user_first      = ( select      users.uid
                         from        users
                         inner join  userRights  on  userRights.userRightId = users.userRightId
                         where       (userRights.userRightName = 'STANDARD_USER') and
                                     (users.usergroupUID       = @usergroup_first)
                         limit 1 );

set @user_last       = ( select      users.uid
                         from        users
                         inner join  userRights  on  userRights.userRightId = users.userRightId
                         where       (userRights.userRightName = 'STANDARD_USER') and
                                     (users.usergroupUID        = @usergroup_last)
                         limit 1 );

-- ---------------------------------------------------------------------------
--  Applications :
--      @app_shared       is a shared application with no executable
--      @app_java         has OS- and CPU-independant executables
--      @app_binary       has OS- and CPU-specific    executables
-- ---------------------------------------------------------------------------
set @app_shared      = ( select      apps.uid
                         from        apps
                         inner join  appTypes  on  apps.appTypeId = appTypes.appTypeId
                         where       appTypes.appTypeName <> 'DEPLOYABLE'
                         limit 1 );

set @app_java        = ( select      apps.uid
                         from        apps
                         inner join  executables  on  executables.appUID = apps.uid
                         inner join  oses         on  oses.osId          = executables.osId
                         where       oses.osName = 'JAVA'
                         limit 1 );

set @app_binary      = ( select      apps.uid
                         from        apps
                         inner join  executables  on  executables.appUID = apps.uid
                         inner join  hosts        on  hosts.osId         = executables.osId
                         where       hosts.project <> ''
                         group by    apps.uid
                         order by    count(hosts.uid)  desc,
                                     length(apps.name) asc
                         limit 1 );

-- ---------------------------------------------------------------------------
--  Purge table 'works'
--  Insert 6 rows
--  Display them
-- ---------------------------------------------------------------------------
delete  from  works   where  uid like '%-public';

insert  into  works ( uid,                   appUID,      status,    ownerUID,    accessRights,  arrivaldate )
             values ( 'shared-first-public', @app_shared, 'PENDING', @user_first, @accessRights, now() - 1 ),
                    ( 'shared-last--public', @app_shared, 'PENDING', @user_last,  @accessRights, now() - 2 ),
                    ( 'java-first-public',   @app_java,   'PENDING', @user_first, @accessRights, now() - 3 ),
                    ( 'java-last--public',   @app_java,   'PENDING', @user_last,  @accessRights, now() - 4 ),
                    ( 'binary-first-public', @app_binary, 'PENDING', @user_first, @accessRights, now() - 5 ),
                    ( 'binary-last--public', @app_binary, 'PENDING', @user_last,  @accessRights, now() - 6 );

select    uid, application, owner, usergroup, arrivalDate
from      view_works
where     uid like '%-public'
order by  arrivalDate;
