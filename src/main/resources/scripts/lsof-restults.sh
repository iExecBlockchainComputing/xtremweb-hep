#!/bin/sh
#
# Copyrights     : CNRS
# Author         : Oleg Lodygensky
# Acknowledgment : XtremWeb-HEP is based on XtremWeb 1.8.0 by inria : http://www.xtremweb.net/
# Web            : http://www.xtremweb-hep.org
# 
#      This file is part of XtremWeb-HEP.
#
# Copyright [2018] [CNRS]
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
#
#



if [ "$1" = "" ] ; then
	echo "not enough arg"
	exit 1
fi

filename=`echo $1 | cut -d '.' -f 1`
datfile=$filename".dat"
gpfile=$filename".gp"
pngfile=$filename".png"

cat $1 | gawk 'BEGIN{FS=";"}{ printf ("%i\t%i\t%i\n", $2,$3,$4)}' > $datfile

echo $gpfile

rm -f $gpfile


cat > $gpfile << EOF
set key outside
set title "lsof | grep java | wc -l\nOneworker, one client and one dispatcher on a single host"
set xlabel "Time (step = ~1 sec)"
set ylabel "opened files"

set terminal png
set output '$pngfile'

plot "$datfile" using 1 title "mysql", "$datfile" using 2 title "pipe", "$datfile" using 3 title "total"

EOF

gnuplot $gpfile


kuickshow $pngfile
