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
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- Table "Taxes"
-- #  Each Tax is persistent :  Details which may change, such as rate, are in
--    the "TaxDetails" table
-- ---------------------------------------------------------------------------
create table if not exists  Taxes  (
  TaxId           int unsigned  not null  auto_increment,
  TaxName         varchar(45)   not null,
  TaxDescription  varchar(254)  null,
  primary key (TaxId),
  unique index unique_TaxName (TaxName)
  )
engine  = InnoDB
comment = 'Each Tax is persistent even if rates change';

show warnings;

-- ---------------------------------------------------------------------------
-- Table "TaxDetails"
-- #  Each Tax Details Row :
--    # refers to a Tax,
--    # contains a validity start date,
--    # contains details which may change, such as rate, ...
-- ---------------------------------------------------------------------------
create table if not exists  TaxDetails  (
  TaxDetailsId        int unsigned  not null  auto_increment,
  TaxId               int unsigned  not null,
  TaxDetailStartDate  date          not null,
  TaxLevel            tinyint       not null,
  TaxRate             decimal(3,3)  not null,
  TaxComment          varchar(254)  null,
  primary key (TaxDetailsId),
  index fk_TaxDetails_Taxes_idx (TaxId),
  unique index unique_TaxDetails_Tax_StartDate (TaxId, TaxDetailStartDate),
  constraint fk_TaxDetails_Taxes
    foreign key (TaxId)
    references Taxes (TaxId)
    on delete cascade
    on update restrict
  )
engine  = InnoDB
comment = 'Each Tax Details Row has a validity start date';

show warnings;

-- ---------------------------------------------------------------------------
-- Table "Metrics"
-- #  Each Metric permits billing and is subject to taxes
-- ---------------------------------------------------------------------------
create table if not exists  Metrics  (
  MetricId           int unsigned  not null  auto_increment,
  MetricName         varchar(45)   not null,
  MetricDimension    varchar(45)   not null                  comment  'For example second, hour, day, MB, MB/s',
  MetricTable        varchar(45)   not null,
  MetricColumn       varchar(45)   not null,
  MetricDescription  varchar(254)  null,
  primary key (MetricId),
  unique index unique_MetricName (MetricName),
  unique index unique_MetricTableAndColumn (MetricTable, MetricColumn)
  )
engine  = InnoDB
comment = 'Each Metric permits billing and is subject to taxes';

show warnings;

-- ---------------------------------------------------------------------------
-- Table "MetricTaxes"
-- #  Each Metric Tax refers to a Metric and to a Tax
-- ---------------------------------------------------------------------------
create table if not exists  MetricTaxes  (
  MetricId            int unsigned  not null,
  TaxId               int unsigned  not null,
  MetricTaxesComment  varchar(254)  null,
  primary key (MetricId, TaxId),
  index fk_MetricTaxes_Metrics_idx (MetricId),
  index fk_MetricTaxes_Taxes_idx (TaxId),
  constraint fk_MetricTaxes_Metrics
    foreign key (MetricId)
    references Metrics (MetricId)
    on delete cascade
    on update restrict,
  constraint fk_MetricTaxes_Taxes
    foreign key (TaxId)
    references Taxes (TaxId)
    on delete cascade
    on update restrict
  )
engine  = InnoDB
comment = 'Relationship table between Metrics and Taxes';

show warnings;

-- ---------------------------------------------------------------------------
-- Table "TariffPlans"
-- #  Each Tariff Plan :
--    # is persistent :  Details which may change, such as rate, fare or
--      threshold, are in the "TariffDetails" table.
--    # permits the offering to Customers, the billing of Customers,
--      or the verification of billing by the Providers
-- ---------------------------------------------------------------------------
create table if not exists  TariffPlans  (
  TariffPlanId                    int unsigned  not null  auto_increment,
  TariffPlanName                  varchar(45)   not null,
  TariffPlanMaxSimultaneousTasks  int           null                      comment  'NOT relevant',
  TariffPlanDescription           varchar(254)  null,
  primary key (TariffPlanId),
  unique index unique_TariffPlanName (TariffPlanName)
  )
engine  = InnoDB
comment = 'Each Tariff Plan is persistent even if rates change';

show warnings;

-- ---------------------------------------------------------------------------
-- Table "TariffDetails"
-- #  Each Tariff Details Row :
--    # refers to a Tariff Plan and to a Metric,
--    # contains a validity start date,
--    # contains details which may change, such as threshold, fare or rate.
-- ---------------------------------------------------------------------------
create table if not exists  TariffDetails  (
  TariffDetailId          int unsigned    not null  auto_increment,
  TariffPlanId            int unsigned    not null,
  MetricId                int unsigned    not null,
  TariffDetailStartDate   date            not null                  comment  'A tariff is valid from its start date until the day preceding the next start date for the same tariff name',
  TariffPackageThreshold  float unsigned  null                      comment  'Package threshold above which resource consumption is billed to the customer or by the provider with the usage rate',
  TariffPackageFare       float unsigned  null                      comment  'Fixed fare billed for any resource consumption lower than or equal to the package threshold',
  TariffUsageIndivisible  float unsigned  null                      comment  'Indivisible amount of resources billed by usage rate for any resource consumption above the package threshold  (for example 60 for billing by indivisible minute)',
  TariffUsageRate         float unsigned  null                      comment  'Usage rate by indivisible amount of resources serving to bill the customer or billed by the provider for any resource consumption above the package threshold',
  TariffComment           varchar(254)    null,
  primary key (TariffDetailId),
  unique index unique_TariffDetails_TariffPlan_Metric_StartDate (TariffPlanId, MetricId, TariffDetailStartDate),
  index fk_TariffDetails_TariffPlans_idx (TariffPlanId),
  index fk_TariffDetails_Metrics_idx (MetricId),
  constraint fk_TariffDetails_TariffPlans
    foreign key (TariffPlanId)
    references TariffPlans (TariffPlanId)
    on delete cascade
    on update restrict,
  constraint fk_TariffDetails_Metrics
    foreign key (MetricId)
    references Metrics (MetricId)
    on delete cascade
    on update restrict
  )
engine  = InnoDB
comment = 'Each Tariff Details Row uses a Tariff Plan and a Metric';

show warnings;

-- ---------------------------------------------------------------------------
-- Table "Customers"
-- #  Each Customer is persistent :  Strings which may change, such as name
--    or address, are in the "CustomerDetails" table
-- ---------------------------------------------------------------------------
create table if not exists  Customers  (
  CustomerId           int unsigned  not null  auto_increment,
  CustomerAccount      varchar(254)  not null  unique          comment  'Should match users.login',
  CustomerBalance      int           not null  default 0,
  CustomerDescription  varchar(254)  null,
  primary key (CustomerId)
  )
engine  = InnoDB
comment = 'Each Customer is persistent even if name and address change';

show warnings;

-- ---------------------------------------------------------------------------
-- Table "CustomerDetails"
-- #  Each Customer Details Row :
--    # refers to a Customer,
--    # contains a validity start date,
--    # contains strings which may change, such as name, address, ...
-- ---------------------------------------------------------------------------
create table if not exists  CustomerDetails  (
  CustomerDetailsId         int unsigned  not null  auto_increment,
  CustomerId                int unsigned  not null,
  CustomerDetailsStartDate  date          not null,
  CustomerName              varchar(45)   not null,
  CustomerBillingAddress    varchar(254)  not null,
  CustomerVATnumber         varchar(45)   null,
  CustomerComment           varchar(254)  null,
  primary key (CustomerDetailsId),
  index fk_CustomerDetails_Customers_idx (CustomerId),
  unique index unique_CustomerDetails_StartDate_Name (CustomerDetailsStartDate, CustomerName),
  constraint fk_CustomerDetails_Customers
    foreign key (CustomerId)
    references Customers (CustomerId)
    on delete cascade
    on update restrict
  )
engine  = InnoDB
comment = 'Each Customer Details Row has a validity start date';

show warnings;

-- ---------------------------------------------------------------------------
-- Table "CustomerContracts"
-- #  Each Customer Contract :
--    # refers to a Customer and to a Customer Tariff Plan,
--    # contains a start date and an end date.
-- ---------------------------------------------------------------------------
create table if not exists  CustomerContracts  (
  CustomerContractId           int unsigned  not null  auto_increment,
  CustomerId                   int unsigned  not null,
  TariffPlanId                 int unsigned  not null,
  CustomerContractStartDate    date          not null                  comment  'A contract is valid from its start date until its end date',
  CustomerContractEndDate      date          null,
  CustomerContractDescription  varchar(254)  null,
  primary key (CustomerContractId),
  index fk_CustomerContracts_Customers_idx (CustomerId),
  index fk_CustomerContracts_TariffPlans_idx (TariffPlanId),
  unique index unique_CustomerContracts_Customer_Tariff_StartDate (CustomerId, TariffPlanId, CustomerContractStartDate),
  constraint fk_CustomerContracts_Customers
    foreign key (CustomerId)
    references Customers (CustomerId)
    on delete cascade
    on update restrict,
  constraint fk_CustomerContracts_TariffPlans
    foreign key (TariffPlanId)
    references TariffPlans (TariffPlanId)
    on delete cascade
    on update restrict
  )
engine  = InnoDB
comment = 'A Customer Contract uses a Tariff Plan and start/end dates';

show warnings;

-- ---------------------------------------------------------------------------
-- Table "Providers"
-- #  Each Provider is persistent :  Strings which may change, such as name
--    or address, are in the "ProviderDetails" table
-- ---------------------------------------------------------------------------
create table if not exists  Providers  (
  ProviderId           int unsigned  not null  auto_increment,
  ProviderAccount      varchar(254)  null,
  ProviderBalance      int           not null  default 0,
  ProviderDescription  varchar(254)  null,
  primary key (ProviderId)
  )
engine  = InnoDB
comment = 'Each Provider is persistent even if name and address change';

show warnings;

-- ---------------------------------------------------------------------------
-- Table "ProviderDetails"
-- #  Each Provider Details Row :
--    # refers to a Provider,
--    # contains a validity start date,
--    # contains strings which may change, such as name, address, ...
-- ---------------------------------------------------------------------------
create table if not exists  ProviderDetails  (
  ProviderDetailsId         int unsigned  not null  auto_increment,
  ProviderId                int unsigned  not null,
  ProviderDetailsStartDate  date          not null,
  ProviderName              varchar(45)   not null,
  ProviderOrderingAddress   varchar(254)  not null,
  ProviderVATnumber         varchar(45)   null,
  ProviderComment           varchar(254)  null,
  primary key (ProviderDetailsId),
  unique index unique_ProviderDetails_StartDate_Name (ProviderDetailsStartDate, ProviderName),
  index fk_ProviderDetails_Providers_idx (ProviderId),
  constraint fk_ProviderDetails_Providers
    foreign key (ProviderId)
    references Providers (ProviderId)
    on delete cascade
    on update restrict
  )
engine  = InnoDB
comment = 'Each Provider Details Row has a validity start date';

show warnings;

-- ---------------------------------------------------------------------------
-- Table "ProviderContracts"
-- #  Each Provider Contract :
--    # refers to a Provider and to a Provider Tariff Plan,
--    # contains a start date and an end date.
-- ---------------------------------------------------------------------------
create table if not exists  ProviderContracts  (
  ProviderContractId           int unsigned  not null  auto_increment,
  ProviderId                   int unsigned  not null,
  TariffPlanId                 int unsigned  not null,
  ProviderContractStartDate    date          not null                  comment  'A contract is valid from its start date until its end date',
  ProviderContractEndDate      date          null,
  ProviderContractDescription  varchar(254)  null,
  primary key (ProviderContractId),
  unique index unique_ProviderContracts_Provider_Tariff_StartDate (ProviderId, TariffPlanId, ProviderContractStartDate),
  index fk_ProviderContracts_TariffPlans_idx (TariffPlanId),
  index fk_ProviderContracts_Providers_idx (ProviderId),
  constraint fk_ProviderContracts_Providers
    foreign key (ProviderId)
    references Providers (ProviderId)
    on delete cascade
    on update restrict,
  constraint fk_ProviderContracts_TariffPlans
    foreign key (TariffPlanId)
    references TariffPlans (TariffPlanId)
    on delete cascade
    on update restrict
  )
engine  = InnoDB
comment = 'A Provider Contract uses a Tariff Plan and start/end dates';

show warnings;


-- ===========================================================================
-- 
-- Initial data for tables containing constant data
-- 
-- ===========================================================================

start transaction;

-- ---------------------------------------------------------------------------
-- Data for table "Metrics"
-- ---------------------------------------------------------------------------
insert into Metrics (MetricId, MetricName, MetricDimension, MetricTable, MetricColumn, MetricDescription) values ( 1, 'datas.size',               'byte',       'datas',       'size',               'For Offering and Billing');
insert into Metrics (MetricId, MetricName, MetricDimension, MetricTable, MetricColumn, MetricDescription) values ( 2, 'executables.dataUID',      'datas.size', 'executables', 'dataUID',            'For Offering and Billing');
insert into Metrics (MetricId, MetricName, MetricDimension, MetricTable, MetricColumn, MetricDescription) values ( 3, 'apps.minMemory',           'megabyte',   'apps',        'minMemory',          'For Offering');
insert into Metrics (MetricId, MetricName, MetricDimension, MetricTable, MetricColumn, MetricDescription) values ( 4, 'apps.minCPUSpeed',         'megaherz',   'apps',        'minCPUSpeed',        'For Offering');
insert into Metrics (MetricId, MetricName, MetricDimension, MetricTable, MetricColumn, MetricDescription) values ( 5, 'apps.minFreeMassStorage',  'megabyte',   'apps',        'minFreeMassStorage', 'For Offering');
insert into Metrics (MetricId, MetricName, MetricDimension, MetricTable, MetricColumn, MetricDescription) values ( 6, 'apps.defaultStdinURI',     'datas.size', 'apps',        'defaultStdinURI',    'For Offering');
insert into Metrics (MetricId, MetricName, MetricDimension, MetricTable, MetricColumn, MetricDescription) values ( 7, 'apps.baseDirinURI',        'datas.size', 'apps',        'baseDirinURI',       'For Offering');
insert into Metrics (MetricId, MetricName, MetricDimension, MetricTable, MetricColumn, MetricDescription) values ( 8, 'apps.defaultDirinURI',     'datas.size', 'apps',        'defaultDirinURI',    'For Offering');
insert into Metrics (MetricId, MetricName, MetricDimension, MetricTable, MetricColumn, MetricDescription) values ( 9, 'works.minMemory',          'megabyte',   'works',       'minMemory',          'For Offering and Billing');
insert into Metrics (MetricId, MetricName, MetricDimension, MetricTable, MetricColumn, MetricDescription) values (10, 'works.minCPUSpeed',        'megaherz',   'works',       'minCPUSpeed',        'For Offering and Billing');
insert into Metrics (MetricId, MetricName, MetricDimension, MetricTable, MetricColumn, MetricDescription) values (11, 'works.minFreeMassStorage', 'megabyte',   'works',       'minFreeMassStorage', 'For Offering and Billing');
insert into Metrics (MetricId, MetricName, MetricDimension, MetricTable, MetricColumn, MetricDescription) values (12, 'works.maxWallClockTime',   'second',     'works',       'maxWallClockTime',   'For Offering and Billing');
insert into Metrics (MetricId, MetricName, MetricDimension, MetricTable, MetricColumn, MetricDescription) values (13, 'works.appUID',             'datas.size', 'works',       'appUID',             'For Billing');
insert into Metrics (MetricId, MetricName, MetricDimension, MetricTable, MetricColumn, MetricDescription) values (14, 'works.stdinURI',           'datas.size', 'works',       'stdinURI',           'For Billing');
insert into Metrics (MetricId, MetricName, MetricDimension, MetricTable, MetricColumn, MetricDescription) values (15, 'works.dirinURI',           'datas.size', 'works',       'dirinURI',           'For Billing');
insert into Metrics (MetricId, MetricName, MetricDimension, MetricTable, MetricColumn, MetricDescription) values (16, 'works.resultURI',          'datas.size', 'works',       'resultURI',          'For Billing');
insert into Metrics (MetricId, MetricName, MetricDimension, MetricTable, MetricColumn, MetricDescription) values (17, 'works.compstartdate',      'second',     'works',       'compstartdate',      'For Billing');
insert into Metrics (MetricId, MetricName, MetricDimension, MetricTable, MetricColumn, MetricDescription) values (18, 'works.compenddate',        'second',     'works',       'compenddate',        'For Billing');

-- ---------------------------------------------------------------------------
-- Data for table "TariffPlans"
-- ---------------------------------------------------------------------------
insert into TariffPlans (TariffPlanId, TariffPlanName, TariffPlanMaxSimultaneousTasks, TariffPlanDescription) values (1, 'Minimal',   999999999, 'Max 15 mn    =    900 s / task');
insert into TariffPlans (TariffPlanId, TariffPlanName, TariffPlanMaxSimultaneousTasks, TariffPlanDescription) values (2, 'Trivial',   999999999, 'Max 30 mn    =   1800 s / task');
insert into TariffPlans (TariffPlanId, TariffPlanName, TariffPlanMaxSimultaneousTasks, TariffPlanDescription) values (3, 'Low',       999999999, 'Max  1 hour  =   3600 s / task');
insert into TariffPlans (TariffPlanId, TariffPlanName, TariffPlanMaxSimultaneousTasks, TariffPlanDescription) values (4, 'Medium',    999999999, 'Max  2 hours =   7200 s / task');
insert into TariffPlans (TariffPlanId, TariffPlanName, TariffPlanMaxSimultaneousTasks, TariffPlanDescription) values (5, 'Regular',   999999999, 'Max  4 hours =  14400 s / task');
insert into TariffPlans (TariffPlanId, TariffPlanName, TariffPlanMaxSimultaneousTasks, TariffPlanDescription) values (6, 'Extensive', 999999999, 'Max  8 hours =  28800 s / task');
insert into TariffPlans (TariffPlanId, TariffPlanName, TariffPlanMaxSimultaneousTasks, TariffPlanDescription) values (7, 'Big',       999999999, 'Max 16 hours =  57600 s / task');
insert into TariffPlans (TariffPlanId, TariffPlanName, TariffPlanMaxSimultaneousTasks, TariffPlanDescription) values (8, 'Huge',      999999999, 'Max 32 hours = 115200 s / task');

-- ---------------------------------------------------------------------------
-- Data for table "Taxes"
-- ---------------------------------------------------------------------------
insert into Taxes (TaxId, TaxName, TaxDescription) values (1, 'VAT very low', null);
insert into Taxes (TaxId, TaxName, TaxDescription) values (2, 'VAT low',      null);
insert into Taxes (TaxId, TaxName, TaxDescription) values (3, 'VAT medium',   null);
insert into Taxes (TaxId, TaxName, TaxDescription) values (4, 'VAT normal',   null);
insert into Taxes (TaxId, TaxName, TaxDescription) values (5, 'VAT high',     null);

-- ---------------------------------------------------------------------------
-- Data for table "TaxDetails"
-- See http://fr.wikipedia.org/wiki/Taxe_sur_la_valeur_ajout%C3%A9e_en_France#Historique_des_taux
-- ---------------------------------------------------------------------------
insert into TaxDetails (TaxDetailsId, TaxId, TaxDetailStartDate, TaxLevel, TaxRate, TaxComment) values (1,  1, '2000-01-01', 9, 0.021, null);
insert into TaxDetails (TaxDetailsId, TaxId, TaxDetailStartDate, TaxLevel, TaxRate, TaxComment) values (2,  2, '2000-01-01', 9, 0.055, null);
insert into TaxDetails (TaxDetailsId, TaxId, TaxDetailStartDate, TaxLevel, TaxRate, TaxComment) values (3,  4, '2000-01-01', 9, 0.196, null);
insert into TaxDetails (TaxDetailsId, TaxId, TaxDetailStartDate, TaxLevel, TaxRate, TaxComment) values (4,  1, '2012-01-01', 9, 0.021, null);
insert into TaxDetails (TaxDetailsId, TaxId, TaxDetailStartDate, TaxLevel, TaxRate, TaxComment) values (5,  2, '2012-01-01', 9, 0.055, null);
insert into TaxDetails (TaxDetailsId, TaxId, TaxDetailStartDate, TaxLevel, TaxRate, TaxComment) values (6,  3, '2012-01-01', 9, 0.07,  null);
insert into TaxDetails (TaxDetailsId, TaxId, TaxDetailStartDate, TaxLevel, TaxRate, TaxComment) values (7,  4, '2012-01-01', 9, 0.196, null);
insert into TaxDetails (TaxDetailsId, TaxId, TaxDetailStartDate, TaxLevel, TaxRate, TaxComment) values (8,  1, '2014-01-01', 9, 0.021, null);
insert into TaxDetails (TaxDetailsId, TaxId, TaxDetailStartDate, TaxLevel, TaxRate, TaxComment) values (9,  2, '2014-01-01', 9, 0.05,  null);
insert into TaxDetails (TaxDetailsId, TaxId, TaxDetailStartDate, TaxLevel, TaxRate, TaxComment) values (10, 3, '2014-01-01', 9, 0.10,  null);
insert into TaxDetails (TaxDetailsId, TaxId, TaxDetailStartDate, TaxLevel, TaxRate, TaxComment) values (11, 4, '2014-01-01', 9, 0.20,  null);

-- ---------------------------------------------------------------------------
-- Data for table "MetricTaxes"
-- ---------------------------------------------------------------------------
insert into MetricTaxes (MetricId, TaxId, MetricTaxesComment) values (12, 4, 'works.maxWallClockTime :  VAT normal');

commit;
