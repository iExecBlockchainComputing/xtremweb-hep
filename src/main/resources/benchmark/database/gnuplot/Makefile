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
GPDIR=$(ROOTDIR)/gnuplot

RAWS = $(wildcard *.gp.in)
GPS  = $(RAWS:.gp.in=.gp)

GNUPLOTSCRIPT=$(SCRIPDIR)/gnuplot.gp

.PRECIOUS: $(DATS) $(CSVS)

all: $(GPS)
	@echo $(CSVS) $(SQLS)
	@echo $(DBNAME) : done

%.gp : %.gp.in
	sed "s/TITLE/$(DBNAME)/" $< > $@
	mv $@ gptmp_file
	cat gptmp_file | sed "s/FILEIN/\.\.\/csv\/$(DBNAME)/g" > $@
	mv $@ gptmp_file
	cat gptmp_file | sed "s/FILEOUT/\.\.\/fig\/$(DBNAME)/g" > $@
	rm -f gptmp_file
	mv $@ ../fig/$(DBNAME)_$@
	echo $(DBNAME) $< $@ $*
	@if [ "$*" != "summary" -a ! -s $(CSVDIR)/$(DBNAME)_$*.csv ]; then rm -f ../fig/$(DBNAME)_$@ ; echo canceling gnuplot ../fig//$(DBNAME)_$@ ; fi

clean :
	rm -f ../fig/*gp
