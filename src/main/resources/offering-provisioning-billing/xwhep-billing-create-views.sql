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
--  XtremWeb-HEP Billing :  SQL script creating following views :
-- 
--  View_Billing_Details_Base               (uses view_works_for_billing)
--  View_Billing_Details_Helper             (uses View_Billing_Details_Base)
--  View_Billing_Details_With_Long_Titles   (uses View_Billing_Details_Helper)
--  View_Billing_Details                    (uses View_Billing_Details_With_Long_Titles)
--  
--  View_Billing_By_Hour_Base               (uses View_Billing_Details_With_Long_Titles)
--  View_Billing_By_Hour_Helper             (uses View_Billing_By_Hour_Base)
--  View_Billing_By_Hour_With_Long_Titles   (uses View_Billing_By_Hour_Helper)
--  View_Billing_By_Hour                    (uses View_Billing_By_Hour_With_Long_Titles)
--  
--  View_Billing_By_Day_Base                (uses View_Billing_Details_With_Long_Titles)
--  View_Billing_By_Day_Helper              (uses View_Billing_By_Day_Base)
--  View_Billing_By_Day_With_Long_Titles    (uses View_Billing_By_Day_Helper)
--  View_Billing_By_Day                     (uses View_Billing_By_Day_With_Long_Titles)
--  
--  View_Billing_By_Month_Base              (uses View_Billing_Details_With_Long_Titles)
--  View_Billing_By_Month_Helper            (uses View_Billing_By_Month_Base)
--  View_Billing_By_Month_With_Long_Titles  (uses View_Billing_By_Month_Helper)
--  View_Billing_By_Month                   (uses View_Billing_By_Month_With_Long_Titles)
-- 
-- ===========================================================================
drop view if exists  View_Billing_By_Month;
drop view if exists  View_Billing_By_Month_With_Long_Titles;
drop view if exists  View_Billing_By_Month_Helper;
drop view if exists  View_Billing_By_Month_Base;

drop view if exists  View_Billing_By_Day;
drop view if exists  View_Billing_By_Day_With_Long_Titles;
drop view if exists  View_Billing_By_Day_Helper;
drop view if exists  View_Billing_By_Day_Base;

drop view if exists  View_Billing_By_Hour;
drop view if exists  View_Billing_By_Hour_With_Long_Titles;
drop view if exists  View_Billing_By_Hour_Helper;
drop view if exists  View_Billing_By_Hour_Base;

drop view if exists  View_Billing_Details;
drop view if exists  View_Billing_Details_With_Long_Titles;
drop view if exists  View_Billing_Details_Helper;
drop view if exists  View_Billing_Details_Base;


-- ---------------------------------------------------------------------------
create  view  View_Billing_Details_Base  as
-- ---------------------------------------------------------------------------
select      view_works_for_billing.uid            as workUID,
            view_works_for_billing.completedDate,
            view_works_for_billing.compDuration,
            Customers.CustomerAccount,
            TariffPlans.TariffPlanName,
            TariffDetails.TariffPackageFare,
            TariffDetails.TariffUsageIndivisible,
            TariffDetails.TariffUsageRate,
            TaxDetails.TaxRate,
            if(view_works_for_billing.compDuration <= TariffDetails.TariffPackageThreshold, 0,
               (view_works_for_billing.compDuration - TariffDetails.TariffPackageThreshold))
               as DurationAbovePackage
from        view_works_for_billing
inner join  Customers          on  view_works_for_billing.owner   = Customers.CustomerAccount
inner join  CustomerContracts  on  Customers.CustomerId           = CustomerContracts.CustomerId
inner join  TariffPlans        on  CustomerContracts.TariffPlanId = TariffPlans.TariffPlanId
inner join  TariffDetails      on  TariffPlans.TariffPlanId       = TariffDetails.TariffPlanId
inner join  Metrics            on  TariffDetails.MetricId         = Metrics.MetricId
inner join  MetricTaxes        on  Metrics.MetricId               = MetricTaxes.MetricId
inner join  Taxes              on  MetricTaxes.TaxId              = Taxes.TaxId
inner join  TaxDetails         on  Taxes.TaxId                    = TaxDetails.TaxId
where       ( view_works_for_billing.statusName     = 'COMPLETED' ) and
            ( date(view_works_for_billing.completedDate) >= CustomerContracts.CustomerContractStartDate ) and
            ( date(view_works_for_billing.completedDate) <= CustomerContracts.CustomerContractEndDate   ) and
            ( TariffDetails.TariffDetailStartDate   =
              ( select max(TariffDetails2.TariffDetailStartDate)
                from   TariffDetails as TariffDetails2
                where  TariffDetails2.TariffDetailStartDate <= view_works_for_billing.completedDate ) ) and
            ( TaxDetails.TaxDetailStartDate =
              ( select max(TaxDetails2.TaxDetailStartDate)
                from   TaxDetails as TaxDetails2
                where  TaxDetails2.TaxDetailStartDate <= view_works_for_billing.completedDate ) )
order by    view_works_for_billing.completedDate;

show warnings;

-- ---------------------------------------------------------------------------
create  view  View_Billing_Details_Helper  as
-- ---------------------------------------------------------------------------
select      workUID,
            completedDate,
            compDuration,
            CustomerAccount,
            TariffPlanName,
            TariffPackageFare,
            TariffUsageIndivisible,
            TariffUsageRate,
            TaxRate,
            DurationAbovePackage,
            ( ceiling(DurationAbovePackage / TariffUsageIndivisible) *
              TariffUsageRate )  as  BillingForUsageAbovePackage
from        View_Billing_Details_Base;

show warnings;

-- ---------------------------------------------------------------------------
create  view  View_Billing_Details_With_Long_Titles  as
-- ---------------------------------------------------------------------------
select      workUID,
            completedDate,
            compDuration,
            CustomerAccount,
            TariffPlanName,
            TariffPackageFare,
            TariffUsageIndivisible,
            TariffUsageRate,
            TaxRate,
            DurationAbovePackage,
            round(BillingForUsageAbovePackage, 5)  as  BillingForUsageAbovePackage,
            round(TariffPackageFare +
                  BillingForUsageAbovePackage, 5)  as  BillingWithoutTax
from        View_Billing_Details_Helper;

show warnings;

-- ---------------------------------------------------------------------------
create  view  View_Billing_Details  as
-- ---------------------------------------------------------------------------
select      workUID,
            completedDate,
            compDuration                     as  Time,
            CustomerAccount                  as  Cust,
            TariffPlanName                   as  TariffPlan,
            TariffPackageFare                as  Fare,
            TariffUsageIndivisible           as  Indivis,
            TariffUsageRate                  as  UsageRate,
            TaxRate,
            DurationAbovePackage             as  TimeAbovePack,
            if(DurationAbovePackage = 0,
               '      0',
               BillingForUsageAbovePackage)  as  BillAbovePack,
            BillingWithoutTax                as  BillWithoutTax
from        View_Billing_Details_With_Long_Titles;

show warnings;


-- ---------------------------------------------------------------------------
create  view  View_Billing_By_Hour_Base  as
-- ---------------------------------------------------------------------------
select    left(completedDate, 13)           as  BillingHour,
          CustomerAccount,
          TariffPlanName,
          TariffPackageFare,
          TariffUsageIndivisible,
          TariffUsageRate,
          TaxRate,
          count(workUID)                    as  nbWorks,
          sum(durationAbovePackage > 0)     as  nbWorksAbovePackage,
          sum(compDuration)                 as  SumDuration,
          sum(durationAbovePackage)         as  SumDurationAbovePackage,
          sum(TariffPackageFare)            as  SumPackageFares,
          sum(BillingForUsageAbovePackage)  as  SumBillingForUsageAbovePackage
from      View_Billing_Details_With_Long_Titles
group by  BillingHour, CustomerAccount, TariffPlanName, TariffPackageFare,
          TariffUsageIndivisible, TariffUsageRate, TaxRate
order by  BillingHour, CustomerAccount;

show warnings;

-- ---------------------------------------------------------------------------
create  view  View_Billing_By_Hour_Helper  as
-- ---------------------------------------------------------------------------
select    BillingHour,
          CustomerAccount,
          TariffPlanName,
          TariffPackageFare,
          TariffUsageIndivisible,
          TariffUsageRate,
          TaxRate,
          nbWorks,
          nbWorksAbovePackage,
          SumDuration,
          SumDurationAbovePackage,
          SumPackageFares,
          SumBillingForUsageAbovePackage,
          ( SumPackageFares +
            SumBillingForUsageAbovePackage )  as  SumBillingWithoutTax
from      View_Billing_By_Hour_Base;

show warnings;

-- ---------------------------------------------------------------------------
create  view  View_Billing_By_Hour_With_Long_Titles  as
-- ---------------------------------------------------------------------------
select    BillingHour,
          timestampdiff(hour, concat(BillingHour,     ':00:00'),
                              concat(left(now(), 13), ':00:00'))  as  HourDifferenceFromNow,
          CustomerAccount,
          TariffPlanName,
          TariffPackageFare,
          TariffUsageIndivisible,
          TariffUsageRate,
          TaxRate,
          nbWorks,
          nbWorksAbovePackage,
          SumDuration,
          SumDurationAbovePackage,
          round(SumPackageFares, 2)                               as  SumPackageFares,
          round(SumBillingForUsageAbovePackage, 2)                as  SumBillingForUsageAbovePackage,
          round(SumBillingWithoutTax, 2)                          as  SumBillingWithoutTax,
          round(SumBillingWithoutTax * TaxRate, 2)                as  SumBillingTaxes
from      View_Billing_By_Hour_Helper;

show warnings;

-- ---------------------------------------------------------------------------
create  view  View_Billing_By_Hour  as
-- ---------------------------------------------------------------------------
select    BillingHour                     as  Hour,
          HourDifferenceFromNow           as  HourDiff,
          CustomerAccount                 as  Cust,
          TariffPlanName                  as  TariffPlan,
          TariffPackageFare               as  Fare,
          TariffUsageIndivisible          as  Indivis,
          TariffUsageRate                 as  UsageRate,
          TaxRate,
          nbWorks,
          nbWorksAbovePackage             as  nbWorksAbovePack,
          SumDuration                     as  SumTime,
          SumDurationAbovePackage         as  SumTimeAbovePack,
          SumPackageFares                 as  SumFares,
          SumBillingForUsageAbovePackage  as  SumUsage,
          SumBillingWithoutTax            as  SumBilling,
          SumBillingTaxes                 as  SumTaxes
from      View_Billing_By_Hour_With_Long_Titles;

show warnings;


-- ---------------------------------------------------------------------------
create  view  View_Billing_By_Day_Base  as
-- ---------------------------------------------------------------------------
select    date(completedDate)               as  BillingDay,
          CustomerAccount,
          TariffPlanName,
          TariffPackageFare,
          TariffUsageIndivisible,
          TariffUsageRate,
          TaxRate,
          count(workUID)                    as  nbWorks,
          sum(durationAbovePackage > 0)     as  nbWorksAbovePackage,
          sum(compDuration)                 as  SumDuration,
          sum(durationAbovePackage)         as  SumDurationAbovePackage,
          sum(TariffPackageFare)            as  SumPackageFares,
          sum(BillingForUsageAbovePackage)  as  SumBillingForUsageAbovePackage
from      View_Billing_Details_With_Long_Titles
group by  BillingDay, CustomerAccount, TariffPlanName, TariffPackageFare,
          TariffUsageIndivisible, TariffUsageRate, TaxRate
order by  BillingDay, CustomerAccount;

show warnings;

-- ---------------------------------------------------------------------------
create  view  View_Billing_By_Day_Helper  as
-- ---------------------------------------------------------------------------
select    BillingDay,
          CustomerAccount,
          TariffPlanName,
          TariffPackageFare,
          TariffUsageIndivisible,
          TariffUsageRate,
          TaxRate,
          nbWorks,
          nbWorksAbovePackage,
          SumDuration,
          SumDurationAbovePackage,
          SumPackageFares,
          SumBillingForUsageAbovePackage,
          ( SumPackageFares +
            SumBillingForUsageAbovePackage )  as  SumBillingWithoutTax
from      View_Billing_By_Day_Base;

show warnings;

-- ---------------------------------------------------------------------------
create  view  View_Billing_By_Day_With_Long_Titles  as
-- ---------------------------------------------------------------------------
select    BillingDay,
          timestampdiff(day, BillingDay, date(now()))  as  DayDifferenceFromNow,
          CustomerAccount,
          TariffPlanName,
          TariffPackageFare,
          TariffUsageIndivisible,
          TariffUsageRate,
          TaxRate,
          nbWorks,
          nbWorksAbovePackage,
          SumDuration,
          SumDurationAbovePackage,
          round(SumPackageFares, 2)                    as  SumPackageFares,
          round(SumBillingForUsageAbovePackage, 2)     as  SumBillingForUsageAbovePackage,
          round(SumBillingWithoutTax, 2)               as  SumBillingWithoutTax,
          round(SumBillingWithoutTax * TaxRate, 2)     as  SumBillingTaxes
from      View_Billing_By_Day_Helper;

show warnings;

-- ---------------------------------------------------------------------------
create  view  View_Billing_By_Day  as
-- ---------------------------------------------------------------------------
select    BillingDay                      as  Day,
          DayDifferenceFromNow            as  DayDiff,
          CustomerAccount                 as  Cust,
          TariffPlanName                  as  TariffPlan,
          TariffPackageFare               as  Fare,
          TariffUsageIndivisible          as  Indivis,
          TariffUsageRate                 as  UsageRate,
          TaxRate,
          nbWorks,
          nbWorksAbovePackage             as  nbWorksAbovePack,
          SumDuration                     as  SumTime,
          SumDurationAbovePackage         as  SumTimeAbovePack,
          SumPackageFares                 as  SumFares,
          SumBillingForUsageAbovePackage  as  SumUsage,
          SumBillingWithoutTax            as  SumBilling,
          SumBillingTaxes                 as  SumTaxes
from      View_Billing_By_Day_With_Long_Titles;

show warnings;


-- ---------------------------------------------------------------------------
create  view  View_Billing_By_Month_Base  as
-- ---------------------------------------------------------------------------
select    left(completedDate, 7)            as  BillingMonth,
          CustomerAccount,
          TariffPlanName,
          TariffPackageFare,
          TariffUsageIndivisible,
          TariffUsageRate,
          TaxRate,
          count(workUID)                    as  nbWorks,
          sum(durationAbovePackage > 0)     as  nbWorksAbovePackage,
          sum(compDuration)                 as  SumDuration,
          sum(durationAbovePackage)         as  SumDurationAbovePackage,
          sum(TariffPackageFare)            as  SumPackageFares,
          sum(BillingForUsageAbovePackage)  as  SumBillingForUsageAbovePackage
from      View_Billing_Details_With_Long_Titles
group by  BillingMonth, CustomerAccount, TariffPlanName, TariffPackageFare,
          TariffUsageIndivisible, TariffUsageRate, TaxRate
order by  BillingMonth, CustomerAccount;

show warnings;

-- ---------------------------------------------------------------------------
create  view  View_Billing_By_Month_Helper  as
-- ---------------------------------------------------------------------------
select    BillingMonth,
          CustomerAccount,
          TariffPlanName,
          TariffPackageFare,
          TariffUsageIndivisible,
          TariffUsageRate,
          TaxRate,
          nbWorks,
          nbWorksAbovePackage,
          SumDuration,
          SumDurationAbovePackage,
          SumPackageFares,
          SumBillingForUsageAbovePackage,
          ( SumPackageFares +
            SumBillingForUsageAbovePackage )  as  SumBillingWithoutTax
from      View_Billing_By_Month_Base;

show warnings;

-- ---------------------------------------------------------------------------
create  view  View_Billing_By_Month_With_Long_Titles  as
-- ---------------------------------------------------------------------------
select    BillingMonth,
          timestampdiff(month, concat(BillingMonth,   '-01'),
                               concat(left(now(), 7), '-01'))  as  MonthDifferenceFromNow,
          CustomerAccount,
          TariffPlanName,
          TariffPackageFare,
          TariffUsageIndivisible,
          TariffUsageRate,
          TaxRate,
          nbWorks,
          nbWorksAbovePackage,
          SumDuration,
          SumDurationAbovePackage,
          round(SumPackageFares, 2)                            as  SumPackageFares,
          round(SumBillingForUsageAbovePackage, 2)             as  SumBillingForUsageAbovePackage,
          round(SumBillingWithoutTax, 2)                       as  SumBillingWithoutTax,
          round(SumBillingWithoutTax * TaxRate, 2)             as  SumBillingTaxes
from      View_Billing_By_Month_Helper;

show warnings;

-- ---------------------------------------------------------------------------
create  view  View_Billing_By_Month  as
-- ---------------------------------------------------------------------------
select    BillingMonth                    as  Month,
          MonthDifferenceFromNow          as  MonthDiff,
          CustomerAccount                 as  Cust,
          TariffPlanName                  as  TariffPlan,
          TariffPackageFare               as  Fare,
          TariffUsageIndivisible          as  Indivis,
          TariffUsageRate                 as  UsageRate,
          TaxRate,
          nbWorks,
          nbWorksAbovePackage             as  nbWorksAbovePack,
          SumDuration                     as  SumTime,
          SumDurationAbovePackage         as  SumTimeAbovePack,
          SumPackageFares                 as  SumFares,
          SumBillingForUsageAbovePackage  as  SumUsage,
          SumBillingWithoutTax            as  SumBilling,
          SumBillingTaxes                 as  SumTaxes
from      View_Billing_By_Month_With_Long_Titles;

show warnings;
