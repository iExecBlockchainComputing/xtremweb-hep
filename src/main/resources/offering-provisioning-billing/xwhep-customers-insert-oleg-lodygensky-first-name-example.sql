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
--  XtremWeb-HEP Tables for Offering, Provisioning and Billing :
--  SQL script inserting example rows in tables 'Customers', 'CustomerDetails'
--  and 'CustomerContracts' for 'Oleg LODYGENSKY fisrt name'.
-- 
-- ===========================================================================
insert  into  Customers
             (CustomerId, CustomerAccount, CustomerBalance, CustomerDescription)
      values (         1,          'oleg',               0, 'Oleg LODYGENSKY fisrt name');

insert  into  CustomerDetails
             (CustomerId, CustomerDetailsStartDate, CustomerName,                 CustomerBillingAddress)
      values (         1,             '2000-01-01', 'Oleg LODYGENSKY fisrt name', 'LAL,  Bat 200,  91898 ORSAY,  France');

insert  into  CustomerContracts
             (CustomerId, TariffPlanId, CustomerContractStartDate, CustomerContractEndDate, CustomerContractDescription)
      values (         1,            6,              '2000-01-01',            '2012-12-31', 'Extensive - Oleg LODYGENSKY fisrt name'),
             (         1,            2,              '2013-01-01',            '2013-12-31', 'Trivial   - Oleg LODYGENSKY fisrt name'),
             (         1,            5,              '2014-01-01',            '2014-12-31', 'Regular   - Oleg LODYGENSKY fisrt name');
