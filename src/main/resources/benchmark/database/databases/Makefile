# Copyrights     : CNRS
# Author         : Oleg Lodygensky
# Acknowledgment : XtremWeb-HEP is based on XtremWeb 1.8.0 by inria : http://www.xtremweb.net/
# Web            : http://www.xtremweb-hep.org
#
#      This file is part of XtremWeb-HEP.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#     http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

SHELL=/bin/sh

DBNAME=xwservpub_20130624
DBUSER=root
DBPASSWD=
DBHOST=localhost
MYSQL=@mysql -B -h $(DBHOST) -u $(DBUSER) --password=$(DBPASSWD)
ECHOMYSQL=mysql -B -h $(DBHOST) -u $(DBUSER) --password=XXXXX

ROOTDIR=..
DATADIR=$(ROOTDIR)/databases
CSVDIR=$(ROOTDIR)/csv
FIGDIR=$(ROOTDIR)/fig
SCRIPTDIR=$(ROOTDIR)/scripts
GPDIR=$(ROOTDIR)/gnuplot

RAWS = $(wildcard *.sql)
CSVS = $(RAWS:.sql=.csv)
GPS  = $(RAWS:.sql=.gp)
PS0 = $(GPS:.gp=.ps)
PS = $(subst $(DATADIR), $(FIGDIR), $(PS0))

GNUPLOTSCRIPT=$(SCRIPDIR)/gnuplot.gp

.PRECIOUS: $(DATS) $(CSVS)

all: $(CSVS) $(GPS)
	@echo Databases ready

clean: $(RAWS)
	rm -f *.csv
	#@echo $(ECHOMYSQL) -e "drop database if exists $(shell basename $< .sql)"
	#$(MYSQL) -e "drop database if exists $(shell basename $< .sql)"
	cd $(SCRIPTDIR) && make -e clean
	cd $(GPDIR) && make -e clean 
	cd $(FIGDIR) && make -e clean

%.csv : %.sql
	#@echo $(ECHOMYSQL) -e "drop database if exists $*"
	#$(MYSQL) -e "drop database if exists $*"
	#@echo $(ECHOMYSQL) -e "create database $*"
	#$(MYSQL) -e "create database $*"
	#@echo $(ECHOMYSQL) $* < $< 
	#$(MYSQL) $* < $< 
	cd $(SCRIPTDIR) && make -e DBNAME=$* DBUSER="$(DBUSER)" DBPASSWD="$(DBPASSWD)" DBHOST="$(DBHOST)"
	cd $(GPDIR) && make -e DBNAME=$*
	cd $(FIGDIR) && make -e

%.gp : %.sql
	cd $(GPDIR) && make -e DBNAME=$* DBUSER="$(DBUSER)" DBPASSWD="$(DBPASSWD)" DBHOST="$(DBHOST)"
