#!/bin/sh 
#=============================================================================
# Acknowledgment : XtremWeb-HEP is based on XtremWeb 1.8.0 by INRIA : http://www.xtremweb.net/
# Web            : http://www.xtremweb-hep.org
# 
#      This file is part of XtremWeb-HEP.
#
# Copyright [2018] [CNRS] Oleg Lodygensky
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#=============================================================================

#
# next variables must be set
#
VERSION=9.1.1
# next variable must have a value among rpm, dpkg or macosx
#WHAT=
# next variable must be like someone@somewhere
RHOST=
# next variable must be like /home/someone/xwhep/dist/
RDISTDIR=
# space separated package names, if any
PACKAGES="PUBLIC PRIVATE"

[ "$WHAT" = "" ] && { echo "Please set variables" ; exit 1; }

for p in $PACKAGES ; do 
	RDIR=${RDISTDIR}/$p/installers
	echo "./rbuild-package.sh ${RHOST} ${RDIR} ${VERSION}  ${WHAT} "
	./rbuild-package.sh ${RHOST} ${RDIR} ${VERSION}  ${WHAT} 
done

