-- ===========================================================================
-- 
--  Copyright 2014  E. URBAH
--                  at LAL, Univ Paris-Sud, IN2P3/CNRS, Orsay, France
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
--  'TariffPackageFare'       (in �) comes from the spreadsheet of Oleg.
--
--  'TariffUsageIndivisible'  is defined here as 1 mn = 60 s
--
--  'TariffUsageRate'         (in �/indivisible) is calculated from
--                            Amazon 0.683 �/h for 8 cores, plus 25% margin.
-- 
-- ===========================================================================
insert  into  TariffDetails
             (TariffPlanId, MetricId, TariffDetailStartDate, TariffPackageThreshold, TariffPackageFare, TariffUsageIndivisible, TariffUsageRate, TariffComment)
      values (           2,       12,          '2000-01-01',                   1800,              0.01,                    300,          0.0089, 'Amazon.  Max 0.5 hour  =  1800 s / task.  Rate above threshold by indivisible 5 mn = 300s.  Unlimited number of simultaneous tasks.'),
             (           5,       12,          '2000-01-01',                  14400,              0.07,                    300,          0.0089, 'Amazon.  Max 4   hours = 14400 s / task.  Rate above threshold by indivisible 5 mn = 300s.  Unlimited number of simultaneous tasks.'),
             (           6,       12,          '2000-01-01',                  28800,              0.09,                    300,          0.0089, 'Amazon.  Max 8   hours = 28800 s / task.  Rate above threshold by indivisible 5 mn = 300s.  Unlimited number of simultaneous tasks.');
