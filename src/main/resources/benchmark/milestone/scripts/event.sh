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
#
# This draws graph for a given event
# Params : -h to get this help
#          -i <inputDir>  (datas)
#          -o <outputDir> (figures)
#          -e <event>
#          -w <wildcard>
#
#   Event examples :
#        downloaded    : a single event
#        "got result"  : two events
#        got@result    : a single event ('@' replaces space)
#
#




ROOTDIR=`dirname $0`/..
DATADIR=""
FIGDIR=""
main_wildcard="*"

MARGE=10

file=$1
event=$2
column=5


#if [ "$file" == "" ]; then
#		echo "avg from which file ?"
#		exit
#fi

#if [ "$event" == "" ]; then
#		echo "avg of what event ?"
#		exit
#fi




############################################################
# Help
############################################################
help()
{
		head -n 12 $0 | tail -11
		exit
}



###########################################
# This generates a new DAT file from CSV files
# to use in gnuplot script to draw boxe graph
# $1 is the event
###########################################
csvBoxes()
{
		WHAT=$1
		TITLE=`echo $WHAT | sed "s/@/ /g"`
		STEP=0
		WIDTH=3

		DATAFILE="$WHAT.dat"
		echo "cvsBoxes $DATAFILE"

		rm -f $DATAFILE
		touch $DATAFILE

		index=0
		for file in `ls $DATADIR/exp*$WHAT.dat`; do
				words=`tail $file | grep -vE "^#" | tail -1 | wc -w`
				data=`tail $file | grep -vE "^#" | tail -1 | cut -d ' ' -f $words`
				[ "$data" == "" ] && data="null"
				ii=`expr $index \* $MARGE`
				ii=$(( $ii + $STEP ))
				echo "$ii $data $WIDTH  # `basename $file`" >> $DATAFILE
				index=$(( $index + 1 ))
		done

		EXPS="exp00 exp01 exp02 exp03 exp04 exp12 exp13 exp14 exp22 exp23 exp24 exp32 exp33 exp34 exp42 exp43 exp44 exp52 exp53 exp54 exp62 exp63 exp64 exp72 exp73 exp74 exp82 exp83 exp84"
		XTICS=""
		index=0
#echo "m=$MARGE"
		for exp in $EXPS; do 
				ii=`expr $index \* $MARGE`
#		echo "ii=$ii"
				index=$(( $index + 1 ))
				XTICS="$XTICS \"$exp\" $ii,"
		done

		XTICS=`echo $XTICS | sed "s/,$//g"`
#echo $XTICS

		XMAX=`expr $MARGE \* 30`

		GPFILE=$DATADIR/$WHAT.gp
		rm -f $GPFILE

		cat > $GPFILE <<EOF
set title '$TITLE'
set xlabel "  " 0,0
set ylabel 'Time (ms)' 0,0
set encoding iso_8859_1
set key outside
#set key left Left reverse 80,235
#set size 1.2,1.2
set key spacing 1.5
set key width 1.5
set logscale y 20

set xtics rotate 90
set xtics ($XTICS)

set terminal postscript	eps
set output '$FIGDIR/$WHAT.ps'
plot '$DATADIR/$DATAFILE' with boxes title ''

set terminal png
set output '$FIGDIR/$WHAT.png
plot '$DATADIR/$DATAFILE' with boxes title ''

EOF
}


############################################################
# This generates gnuplot scripts from CSV files to draw line graphs
# $1 is the file wildcard
# $2 is the event to search from files; spaces have been replaced by '@'
############################################################
gpLine()
{
	ae_nbfiles=0;
	ae_avg=0
	ae_totalfiles=0
	ae_event0=$2
	ae_event=`echo $2 | sed "s/@/ /g"`
	ae_filewildcard=$1

#	echo $1 $2

	plots="plot "

	for ae_file in `ls *$ae_filewildcard*.csv`; do
#			echo "    $ae_file"
			grep "$ae_event" $ae_file > /dev/null 2>&1
			[ $? -ne 0 ] && continue
#			grep "$ae_event" $ae_file | sed "s/^/$ae_file/g"
#			nbt=`grep "$ae_event" $ae_file | cut -f 5 | wc -l`
#			echo "$ae_file \"$ae_event\" $nbt"
			times=`grep "$ae_event" $ae_file | cut -f 5`
			basename=`echo $ae_file | cut -d '.' -f 1`
			filename=`echo $basename"_"$ae_event0`
			datafile=`echo $filename".dat"`
			gpthis=`echo $filename".gp"`
			rm -f $datafile
			touch $datafile
			rm -f $gpthis
			touch $gpthis
			total=0
			for time in $times; do
					total=$(( $time + $total ))
					echo $time $total >> $datafile
			done
#			time=`grep "$ae_event" $ae_file | cut -f 5`
#			echo $ae_file $ae_event $time

			plots=`echo $plots" '$DATADIR/$datafile'  using 1 title '$basename' with steps, '$DATADIR/$datafile'  using 2 title '$basename cumulated' with steps,"`

			cat > $gpthis <<EOF
set title "$ae_event"
set xlabel "Calls" 0,0
set ylabel 'Time (ms)' 0,0
set encoding iso_8859_1
set key outside
#set key left Left reverse 80,235
#set size 1.2,1.2
set key spacing 1.5
set key width 1.5
#set logscale y

set terminal postscript	eps
set output '$FIGDIR/$filename.ps'

plot '$DATADIR/$datafile'  using 1 title '$basename' with steps, '$DATADIR/$datafile'  using 2 title '$basename cumulated' with steps

set terminal png
set output '$FIGDIR/$filename.png'

plot '$DATADIR/$datafile'  using 1 title '$basename' with steps, '$DATADIR/$datafile'  using 2 title '$basename cumulated' with steps

EOF

	done

	plots=`echo $plots | sed "s/,$//g"`

	gpfile=`echo $ae_event0".gp"`
	rm -f $gpfile
	touch $gpfile
	cat > $gpfile <<EOF
set title "$ae_event"
set xlabel "Calls" 0,0
set ylabel 'Time (ms)' 0,0
set encoding iso_8859_1
set key outside
#set key left Left reverse 80,235
#set size 1.2,1.2
set key spacing 1.5
set key width 1.5
#set logscale y
set terminal postscript	eps
set output '$FIGDIR/$ae_event0.ps'

$plots

set terminal png
set output '$FIGDIR/$ae_event0.png'

$plots

EOF
}



############################################################
# Main
############################################################



while [ $# -gt 0 ]; do
		case $1 in
				"-i" )
						shift
						DATADIR=$1
						;;
				"-o" )
						shift
						FIGDIR=$1
						;;
				"-e" )
						shift
						main_events=$1
						;;
				"-w" )
						shift
						main_wildcard=$1
						;;
				"-h" )
						help
						;;
		esac
		shift
done

[ "$DATADIR" = "" ] && help
[ "$FIGDIR"  = "" ] && help



#
# events list is space separated; hence we replace spaces by '@' on events themselves
#

#events=`cat mounts_xw_dispatcher*.csv | sed "s/ /@/g" | grep -v '#' | grep -v DeltaTime | cut -f 2 | sort -u | sed "s/$/\"/g" | sed "s/^/\"/"`


a_wildcard=$main_wildcard
[ "$a_wildcard" == "*" ] && a_wildcard=client
echo $a_wildcard | grep client > /dev/null 2>&1
TEST=$?
if [ $TEST == 0 ]; then
		[ "$main_events" == "" ] && main_events=`cat *$a_wildcard.csv | sed "s/ /@/g" | grep -vE "^#" | grep -v DeltaTime | cut -f 2 | sort -u`
		for event in $main_events; do
				echo "W=$a_wildcard E=$event"
				gpLine $a_wildcard $event
				csvBoxes $event
		done
fi

a_wildcard=$main_wildcard
[ "$a_wildcard" == "*" ] && a_wildcard=dispatcher
echo $a_wildcard | grep dispatcher > /dev/null 2>&1
TEST=$?
if [ $TEST == 0 ]; then
		[ "$main_events" == "" ] && main_events=`cat *$a_wildcard.csv | sed "s/ /@/g" | grep -vE '^#' | grep -v DeltaTime | grep -vi UID | cut -f 2 | sort -u`
		for event in $main_events; do
				echo "W=$a_wildcard E=$event"
				gpLine $a_wildcard $event
				csvBoxes $event
		done
fi

a_wildcard=$main_wildcard
[ "$a_wildcard" == "*" ] && a_wildcard=worker
echo $a_wildcard | grep worker > /dev/null 2>&1
TEST=$?
if [ $TEST == 0 ]; then
		[ "$main_events" == "" ] && main_events=`cat *$a_wildcard.csv | sed "s/ /@/g" | grep -vE '^#' | grep -v DeltaTime | cut -f 2 | sort -u`
		for event in $main_events; do
				echo "W=$a_wildcard E=$event"
				gpLine $a_wildcard $event
				csvBoxes $event
		done
fi

