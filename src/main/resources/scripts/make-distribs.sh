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



#
# File   : make-distribs.sh
# Author : O. Lodygensky
# Date   : Oct 5th, 2010
#
# This helps to prepare different distributions
# This needs distribution name in command line argument
# e.g. : make-distribs.sh public private
#
# You must prepare distribution configurations in ../conf/ directory
# Configuration files must end with "-$DISTRIBS[0..n]"
#
# Example:
#
#  Prepare the following configuration files:
#    ../conf/xtremweb.worker.conf-public
#    ../conf/xtremweb.worker.conf-private
#    ../conf/xwconfigure.values-public   (if applicable)
#    ../conf/xwconfigure.values-private  (if applicable)
#
# Then call this script like : ./make-distribs.sh public private
#

progdir=`dirname $0`
currentdir=`pwd`
cd $progdir/..
ROOTDIR=`pwd`
cd $currentdir


if [ $# -eq 0 ] ; then
    echo "Usage : $0 distrib0 distrib1 ... distribN"
    exit 1
fi

DISTRIBS="$*"

BINDIR=$ROOTDIR/bin
CONFDIR=$ROOTDIR/conf
DOCDIR=$ROOTDIR/doc
INSTALLERSDIR=$ROOTDIR/installers
KEYSTOREDIRNAME=keystore
KEYSTOREDIR=$ROOTDIR/$KEYSTOREDIRNAME
LIBDIR=$ROOTDIR/lib
XWSERVERCONFNAME=xtremweb.server.conf
XWSERVERCONF=$CONFDIR/$XWSERVERCONFNAME
XWWORKERCONFNAME=xtremweb.worker.conf
XWWORKERCONF=$CONFDIR/$XWWORKERCONFNAME
XWCLIENTCONFNAME=xtremweb.client.conf
XWCLIENTCONF=$CONFDIR/$XWCLIENTCONFNAME
XWCONFVALUESNAME=xwconfigure.values


for distrib in $DISTRIBS ; do

    DDIR=$ROOTDIR/dist/$distrib
    DCONFDIR=$DDIR/conf

    mkdir -p $DCONFDIR > /dev/null 2>&1
    if [ $? -ne 0 ] ; then
		echo "WARN : can't mkdir -p $DCONFDIR"
		continue
    fi
    echo "****************************************"
    echo "* Preparing distribution $distrib to"
    echo "* $DDIR"
    echo "****************************************"

    cp -f $ROOTDIR/AUTHORS $ROOTDIR/COPYING $ROOTDIR/ChangeLog $ROOTDIR/INSTALL $ROOTDIR/License* $DDIR
    cp -Rf $BINDIR $DDIR > /dev/null 2>&1
    if [ $? -ne 0 ] ; then
		echo "WARN : can't copy $BINDIR to $DDIR"
		echo "WARN : cancelling $distrib"
		continue
    fi

    rm -f $DCONFDIR/*

    cp -Rf $CONFDIR $DDIR > /dev/null 2>&1
    if [ $? -ne 0 ] ; then
		echo "WARN : can't copy $CONFDIR to $DDIR"
		echo "WARN : cancelling $distrib"
		continue
    fi

    DWORKERCONF=$CONFDIR/$XWWORKERCONFNAME-$distrib
    WORKERCONF=$DCONFDIR/$XWWORKERCONFNAME

    if [ -f $DWORKERCONF ] ; then
	    cp -f $DWORKERCONF $WORKERCONF > /dev/null 2>&1
    	if [ $? -ne 0 ] ; then
			echo "WARN : can't copy $DWORKERCONF to $WORKERCONF"
			echo "WARN : cancelling $distrib"
			continue
    	fi
	else
		echo "INFO : no $DWORKERCONF; using default"
	fi

    DSERVERCONF=$CONFDIR/$XWSERVERCONFNAME-$distrib
    SERVERCONF=$DCONFDIR/$XWSERVERCONFNAME
    if [ -f $DSERVERCONF ] ; then
    	cp -f $DSERVERCONF $SERVERCONF > /dev/null 2>&1
    	if [ $? -ne 0 ] ; then
			echo "WARN : can't copy $DSERVERCONF to $SERVERCONF"
			echo "WARN : cancelling $distrib"
			continue
	    fi
	else
		echo "INFO : no $DSERVERCONF; using default"
	fi

    DCLIENTCONF=$CONFDIR/$XWCLIENTCONFNAME-$distrib
    CLIENTCONF=$DCONFDIR/$XWCLIENTCONFNAME
    if [ -f $DCLIENTCONF ] ; then
    	cp -f $DCLIENTCONF $CLIENTCONF > /dev/null 2>&1
    	if [ $? -ne 0 ] ; then
			echo "WARN : can't copy $DCLIENTCONF to $CLIENTCONF"
			echo "WARN : cancelling $distrib"
			continue
	    fi
	else
		echo "INFO : no $DCLIENTCONF; using default"
	fi

    DXWCONFVALUES=$CONFDIR/$XWCONFVALUESNAME-$distrib
    if [ -f $DXWCONFVALUES ] ; then
		cp -f $DXWCONFVALUES $DCONFDIR/$XWCONFVALUESNAME > /dev/null 2>&1
		if [ $? -ne 0 ] ; then
	    	echo "WARN : can't copy $DXWCONFVALUES to $DCONFDIR/$XWCONFVALUESNAME"
			echo "WARN : cancelling $distrib"
		    continue
		fi
    else
		echo "INFO : no $DXWCONFVALUES; using default"
    fi

    cp -Rf $DOCDIR $DDIR > /dev/null 2>&1
    if [ $? -ne 0 ] ; then
		echo "WARN : can't copy $DOCDIR to $DDIR"
		continue
    fi
    cp -Rf $INSTALLERSDIR $DDIR > /dev/null 2>&1
    if [ $? -ne 0 ] ; then
		echo "WARN : can't copy $INSTALLERDIR to $DDIR"
		echo "WARN : cancelling $distrib"
		continue
    fi
    cp -Rf $KEYSTOREDIR  $DDIR > /dev/null 2>&1
    if [ $? -ne 0 ] ; then
		echo "WARN : can't copy $KEYSTOREDIR to $DDIR"
		echo "WARN : cancelling $distrib"
		continue
    fi
    cp -Rf $LIBDIR  $DDIR > /dev/null 2>&1
    if [ $? -ne 0 ] ; then
		echo "WARN : can't copy $LIBDIR to $DDIR"
		echo "WARN : cancelling $distrib"
		continue
    fi

    cd $DDIR > /dev/null 2>&1
    if [ $? -ne 0 ] ; then
		echo "WARN : can't changedir to $DDIR"
		echo "WARN : cancelling $distrib"
		continue
    fi

    ./bin/xwconfigure --yes
    RC=$?
    if [ $RC -ne 0 ] ; then
		echo "WARN : xwconfigure error : $RC"
		echo "WARN : cancelling $distrib"
		continue
    fi

    cp -f $DCONFDIR/$XWCONFVALUESNAME $CONFDIR > /dev/null 2>&1
    if [ $? -ne 0 ] ; then
		echo "WARN : can't copy $DCONFDIR/$XWCONFVALUESNAME to $CONFDIR"
		echo "WARN : cancelling $distrib"
		continue
    fi

    cp -f $DDIR/$KEYSTOREDIRNAME/* $KEYSTOREDIR > /dev/null 2>&1
    if [ $? -ne 0 ] ; then
		echo "WARN : can't copy $DDIR/$KEYSTOREDIRNAME/* to $KEYSTOREDIR"
		echo "WARN : cancelling $distrib"
		continue
    fi

    for i in `ls $DCONFDIR/*` ; do
		iname=`basename $i`
		cat $i | sed "s/current/$CLUSTERNAME/g" > $DCONFDIR/tmp
		[ $? -ne 0 ] &&	echo "WARN : can't sed $i"
		mv $DCONFDIR/tmp $i 
		[ $? -ne 0 ] &&	echo "WARN : can't mv $i"
    done

done


cd $currentdir

#
# EOF
#
