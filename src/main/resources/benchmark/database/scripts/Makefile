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

ROOTDIR=..
FIGDIR=$(ROOTDIR)/fig
CSVDIR=$(ROOTDIR)/csv
SCRIPTDIR=$(ROOTDIR)/scripts

MYSQL=@mysql -B -h ${DBHOST} -u $(DBUSER) --password=$(DBPASSWD)
ECHOMYSQL=mysql -B -h $(DBHOST) -u $(DBUSER) --password=XXXXXX

RAWS = $(wildcard *.sql)
CSVS = $(RAWS:.sql=.csv)

GNUPLOTSCRIPT=$(SCRIPDIR)/gnuplot.gp

.PRECIOUS: $(DATS) $(CSVS)

all: $(CSVS)
	@echo $(CSVS) $(SQLS)
	@echo $(DBNAME) : done

%csv : %sql
	@echo "$(ECHOMYSQL) < $< > $(CSVDIR)/$(DBNAME)_$@"
	$(MYSQL) $(DBNAME)  < $< > $(CSVDIR)/$(DBNAME)_$@
	@if [ ! -s $(CSVDIR)/$(DBNAME)_$@ ]; then rm -f $(CSVDIR)/$(DBNAME)_$@ ; echo canceling script $(CSVDIR)/$(DBNAME)_$@ ; fi

clean :
	rm -f *~ *dat *csv  $(CSVDIR)/*
