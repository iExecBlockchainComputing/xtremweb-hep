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

set title "XWHEP 7.5.0 Vs 7.4.0\nClient timestamps, submitting 10K jobs\n\n200 TCP handlers\nSequential flow : time = fct(event number)"
set xlabel "Event number"
set ylabel 'Time (ms)'
set encoding iso_8859_1
set key outside
#set key left Left reverse 80,235
#set size 1.2,1.2
set key spacing 1.5
set key width 1.5
#set logscale y
#set yrange [0:300]
set xtics rotate

set terminal png
set output 'xwhep_750_740.png'

plot 'datas/xwhep750_TCP200_DB60_0W.dat'  using 4 title '7.5.0 Reference'  lt 2 lw 2 with lines,\
     'datas/xwhep750_TCP200_DB60_1457W.dat'  using 4 title '7.5.0 DB60'  lt 2 lw 2 with lines,\
     'datas/xwhep750_TCP200_DB120_1466W.dat' using 4 title '7.5.0 DB120' lt 1 lw 2 with lines,\
     'datas/xwhep740_TCP200_DB60_2323W_insertdelayed_commoptimized_tblindexes_hpools_dbcache2.dat'  using 4 title '7.4.0 DB60' lt 3 lw 2 with lines

