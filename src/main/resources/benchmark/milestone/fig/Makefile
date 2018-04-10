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

CSVOPTS = -v of=csv
DATOPTS = -v of=gnuplot

ROOTDIR=..
DATADIR=$(ROOTDIR)/datas
FIGDIR=$(ROOTDIR)/fig
SCRIPTDIR=$(ROOTDIR)/scripts

AWK = gawk
AWKSCRIPT = $(SCRIPTDIR)/parse.awk
AWKOPTS = -f $(AWKSCRIPT)

GNUPLOT = gnuplot

PS = mounts.ps
JPEG = mounts.jpeg

GPS = $(wildcard $(DATADIR)/*.gp)
PS0 = $(GPS:.gp=.ps)
PS = $(subst $(DATADIR), $(FIGDIR), $(PS0))

all: $(PS)

PS: $(GPS)
	@echo PS done

clean :
	rm -f *client*.png
	rm -f *client*.jpeg
	rm -f *client*.ps
	rm -f *dispatcher*.png
	rm -f *dispatcher*.jpeg
	rm -f *dispatcher*.ps
	rm -f *worker*.png
	rm -f *worker*.jpeg
	rm -f *worker*.ps
	rm -f $(GPS) dispatchers.gp clients.gp workers.gp

VERSUS0 : $(DATADIR)/mounts_pipeudp0.dat $(DATADIR)mounts_xw_client0.dat
	gnuplot $(SCRIPTDIR)/pipeudpVSxw.gp

$(FIGDIR)/%ps : $(DATADIR)/%gp
	$(GNUPLOT) $<
