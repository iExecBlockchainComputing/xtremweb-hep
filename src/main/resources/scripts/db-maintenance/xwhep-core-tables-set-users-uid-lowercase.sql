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
--  SQL script permitting to set 'users.uid' and '*.ownerUID' to lowercase
--
-- ===========================================================================
update users               set uid      = lower(uid)       where cast(uid      as binary) regexp '[A-Z]';
update usergroups          set ownerUID = lower(ownerUID)  where cast(ownerUID as binary) regexp '[A-Z]';
update hosts               set ownerUID = lower(ownerUID)  where cast(ownerUID as binary) regexp '[A-Z]';
update traces              set ownerUID = lower(ownerUID)  where cast(ownerUID as binary) regexp '[A-Z]';
update datas               set ownerUID = lower(ownerUID)  where cast(ownerUID as binary) regexp '[A-Z]';
update apps                set ownerUID = lower(ownerUID)  where cast(ownerUID as binary) regexp '[A-Z]';
update sessions            set ownerUID = lower(ownerUID)  where cast(ownerUID as binary) regexp '[A-Z]';
update groups              set ownerUID = lower(ownerUID)  where cast(ownerUID as binary) regexp '[A-Z]';
update works               set ownerUID = lower(ownerUID)  where cast(ownerUID as binary) regexp '[A-Z]';
update tasks               set ownerUID = lower(ownerUID)  where cast(ownerUID as binary) regexp '[A-Z]';

update users_history       set uid      = lower(uid)       where cast(uid      as binary) regexp '[A-Z]';
update usergroups_history  set ownerUID = lower(ownerUID)  where cast(ownerUID as binary) regexp '[A-Z]';
update hosts_history       set ownerUID = lower(ownerUID)  where cast(ownerUID as binary) regexp '[A-Z]';
update traces_history      set ownerUID = lower(ownerUID)  where cast(ownerUID as binary) regexp '[A-Z]';
update datas_history       set ownerUID = lower(ownerUID)  where cast(ownerUID as binary) regexp '[A-Z]';
update apps_history        set ownerUID = lower(ownerUID)  where cast(ownerUID as binary) regexp '[A-Z]';
update sessions_history    set ownerUID = lower(ownerUID)  where cast(ownerUID as binary) regexp '[A-Z]';
update groups_history      set ownerUID = lower(ownerUID)  where cast(ownerUID as binary) regexp '[A-Z]';
update works_history       set ownerUID = lower(ownerUID)  where cast(ownerUID as binary) regexp '[A-Z]';
update tasks_history       set ownerUID = lower(ownerUID)  where cast(ownerUID as binary) regexp '[A-Z]';
