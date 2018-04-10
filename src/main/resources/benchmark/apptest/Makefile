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


OSNAME=$(shell uname -s)
CPUNAME=$(shell uname -m)


APPLICATION=apptest
SOURCE=$(APPLICATION).c

MACOS=macosx
LINUX=linux
WIN32=win32

INTEL=ix86
PPC=ppc
AMD64=amd64


OSTYPE=unknown
CPUTYPE=$(CPUNAME)


GCC=gcc
GCCFLAGS=


ifeq ("$(OSNAME)", "Darwin")
  OSTYPE=$(MACOS)
  GCCFLAGS= -force_cpusubtype_ALL -arch i386 -arch ppc
endif
ifeq ("$(OSNAME)", "Linux")
  OSTYPE=$(LINUX)
endif
ifeq ("$(CPUNAME)", "i686")
  CPUTYPE=$(INTEL)
endif
ifeq ("$(CPUNAME)", "i386")
  CPUTYPE=$(INTEL)
endif

ALL=$(APPLICATION)-$(OSTYPE)-$(CPUTYPE)
MACOSXPPC=$(APPLICATION)-$(OSTYPE)-$(PPC)
MACOSXINTEL=$(APPLICATION)-$(OSTYPE)-$(INTEL)

all: $(ALL)

clean:
	@echo $(OSNAME)
	rm -f $(ALL)
ifeq ("$(OSNAME)", "Darwin")
	rm -f $(MACOSXINTEL) $(MACOSXPPC)
endif

$(ALL): $(SOURCE)
	gcc $(GCCFLAGS) $< -o $@
ifeq ("$(OSNAME)", "Darwin")
ifeq ("$(CPUNAME)", "PPC")
	cp $(ALL) $(MACOSXINTEL)
else
	cp $(ALL) $(MACOSXPPC)
endif
endif
