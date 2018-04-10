#!/bin/sh

# Acknowledgment : XtremWeb-HEP is based on XtremWeb 1.8.0 by inria : http://www.xtremweb.net/
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
#
#




if [ "$1" = "" ] ; then
	echo "not enough arg"
	exit 1
fi

filename=`echo $1 | cut -d '.' -f 1`
datfile=$filename".dat"
gpfile=$filename".gp"
ofpngfile=$filename"_of.png"
pspngfile=$filename"_ps.png"
cpumempngfile=$filename"_cpumem.png"
memswappngfile=$filename"_memswap.png"


cat $1 | gawk 'BEGIN{FS=";"}{ printf ("%i\t%i\t%i\t%i\t%i\t%i\t%i\t%i\t%i\t%i\t%i\t%i\t%i\n", $1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13)}' > $datfile

echo $gpfile

rm -f $gpfile


cat > $gpfile << EOF
set key outside
set title "lsof | grep java | wc -l\nOneworker, one client and one dispatcher on a single host"
set xlabel "Time (step = ~1 sec)"
set ylabel "opened files"
set logscale y

set terminal png
set output '$ofpngfile'

plot "$datfile" using 2 with linespoints title "mysql", "$datfile" using 3 with linespoints title "pipe", "$datfile" using 4 with linespoints title "total"

set ylabel "Amount of processors"

set terminal png
set output '$pspngfile'

plot "$datfile" using 5 with linespoints title "Total procs", "$datfile" using 6 with linespoints title "XW procs", "$datfile" using 7 with linespoints title "Child procs"

set ylabel "%CPU and %MEM usage"

set terminal png
set output '$cpumempngfile'

plot "$datfile" using 8 with linespoints title "XW CPU", "$datfile" using 9 with linespoints title "XW MEM", "$datfile" using 10 with linespoints title "Child CPU", "$datfile" using 11 with linespoints title "Child MEM"

set ylabel "Free Mem & Swap"

set terminal png
set output '$memswappngfile'

plot "$datfile" using 12 with linespoints title "Free MEM", "$datfile" using 13 with linespoints title "Free Swap"

EOF

gnuplot $gpfile


kuickshow $ofpngfile $pspngfile $cpumempngfile $memswappngfile
