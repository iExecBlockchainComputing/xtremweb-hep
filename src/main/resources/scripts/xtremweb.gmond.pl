#!/usr/bin/perl

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



#  Installation under ubuntu16: sudo apt-get install libdbi-perl libclass-dbi-mysql-perl
#
# Depending on mysql version, you may have the following error
# Couldn't execute statement: Expression #1 of SELECT list is not in GROUP BY clause
# and contains nonaggregated column 'xtremweb.hosts.uid' which is not functionally
# dependent on columns in GROUP BY clause;
# this is incompatible with sql_mode=only_full_group_by
#
# The next command solves this issue (to be run in mysql):
#       SET GLOBAL sql_mode = '';
# or
#       set global sql_mode="STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION";
#
# File    : gmond.pl
# Author  : Oleg Lodygens (lodygens at lal.in2p3.fr)
# Date    : Octobre 20th, 2005
# Purpose : this retreives the XWHEP DesktopGrid cluster informations
#
# chkconfig: 345 99 99
#

use strict;
use English;
use IO::Handle;
use IO::Socket;
use File::Basename;
use File::stat;
use DirHandle;
use integer;
use DBI;

#
# trap CTRL+C
#
use sigtrap 'handler' => \&ctrlcHandler, 'INT';

#
# this controls program execution
#
my $continuer = 1;

# Synchronize the Perl IOs
autoflush STDIN 1;
autoflush STDERR 1;
autoflush STDOUT 1;

#
# default values
#
my $DBUSER = "@DBADMINLOGIN@";
my $dbpassword = "@DBADMINPASSWORD@";
my $DBHOST = "@DBHOST@";
my $database = "@DBNAME@";

my $clusterName = "XtremWeb-HEP";
chomp($clusterName=`uname -n`);

my $tcpPort = 8694;
my $startTime = time;
my $currentTime = time;
my $scriptName = `basename $0`;
my $scriptDir = `dirname $0`;
my $dbHandler;

chomp ($scriptName);
chomp ($scriptDir);
$scriptDir = `pwd` if ($scriptDir = ".");
chomp ($scriptDir);

#
# debug levels
#
my $DEBUG = 0;
my $INFO  = 1;
my $WARN  = 2;
my $ERROR = 3;
my $debug = $INFO;



#
# this is called whenever user hits CTRL+C
#
sub ctrlcHandler {
    $dbHandler->disconnect;
    print "Caught SIGINT\n";
    exit 1;
}


#
# This prints out usage
#
sub usage {
    print "Usage : $scriptName -u dbuser -p dbpassword [-db database] [-r dir] [-h] [-d level] \n";
    print "\t -h                to get this help\n";
    print "\t -d    level       to set debug level\n";
    print "\t -u    dbuser      where dbuser     is the DB user name\n";
    print "\t -pass dbpassword  where dbpassword is the DB user password\n";
    print "\t -db   database    where database   is the DB name\n";
    print "\t -p    port        where port is the port to listen to (default = $tcpPort)\n";
    exit;
}


#
# This prints out debugs
#
sub debug {
    print "DEBUG : @_\n" if ($debug <= $DEBUG);
}


#
# This prints out infos
#
sub info {
    print "INFO : @_\n" if ($debug <= $INFO);
}


#
# This prints warnings
#
sub warn {
    print "WARN : @_\n" if ($debug <= $WARN);
}


#
# This prints out errors
#
sub error {
    print "ERROR : @_\n" if ($debug <= $ERROR);
}



# ============================================== #
# *                    Main                      #
# ============================================== #


#
# parse command line args
#
my $arg;

while ($arg = shift) {
    if (($arg eq '-h') || ($arg eq '--help')) {
        usage;
    }
    if ($arg eq '-c') {
        $clusterName = shift;
    }
    if ($arg eq '-d') {
        $debug = shift;
    }
    if ($arg eq '-pass') {
        $dbpassword = shift;
    }
    if ($arg eq '-p') {
        $tcpPort = shift;
    }
    if ($arg eq '-u') {
        $DBUSER = shift;
    }
    if ($arg eq '-db') {
        $database = shift;
    }
}

#
# create socket to listen to
#
my $inputSocket = new IO::Socket::INET (#LocalHost => 'localhost',
                                        LocalPort => $tcpPort,
                                        Proto => 'tcp',
                                        Listen => 1,
                                        Reuse => 1);
die "Could not create socket: $!\n" unless $inputSocket;



#
# Don't leave zombies processes
#
$SIG{CHLD} = 'IGNORE';

#
# Loop for ever
#
while(1) {
    #
    # wait incoming connections
    #
#    &debug("Waiting incoming connexions");

    while (my $inSock = $inputSocket->accept()) {

#
# create a child process
#
	unless(fork) {
	    &debug("New connexion");

	    $currentTime = time;

	    #
	    # Print Ganglia header
	    #
	    print $inSock "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"yes\"?>\n";
	    print $inSock "<!DOCTYPE GANGLIA_XML [\n";
	    print $inSock "<!ELEMENT GANGLIA_XML (GRID)*>\n";
	    print $inSock "<!ATTLIST GANGLIA_XML VERSION CDATA #REQUIRED>\n";
	    print $inSock "<!ATTLIST GANGLIA_XML SOURCE CDATA #REQUIRED>\n";
	    print $inSock "<!ELEMENT GRID (CLUSTER | GRID | HOSTS | METRICS)*>\n";
	    print $inSock "<!ATTLIST GRID NAME CDATA #REQUIRED>\n";
	    print $inSock "<!ATTLIST GRID AUTHORITY CDATA #REQUIRED>\n";
	    print $inSock "<!ATTLIST GRID LOCALTIME CDATA #IMPLIED>\n";
	    print $inSock "<!ELEMENT CLUSTER (HOST | HOSTS | METRICS)*>\n";
	    print $inSock "<!ATTLIST CLUSTER NAME CDATA #REQUIRED>\n";
	    print $inSock "<!ATTLIST CLUSTER OWNER CDATA #IMPLIED>\n";
	    print $inSock "<!ATTLIST CLUSTER LATLONG CDATA #IMPLIED>\n";
	    print $inSock "<!ATTLIST CLUSTER URL CDATA #IMPLIED>\n";
	    print $inSock "<!ATTLIST CLUSTER LOCALTIME CDATA #REQUIRED>\n";
	    print $inSock "<!ELEMENT HOST (METRIC)*>\n";
	    print $inSock "<!ATTLIST HOST NAME CDATA #REQUIRED>\n";
	    print $inSock "<!ATTLIST HOST IP CDATA #REQUIRED>\n";
	    print $inSock "<!ATTLIST HOST LOCATION CDATA #IMPLIED>\n";
	    print $inSock "<!ATTLIST HOST REPORTED CDATA #REQUIRED>\n";
	    print $inSock "<!ATTLIST HOST TN CDATA #IMPLIED>\n";
	    print $inSock "<!ATTLIST HOST TMAX CDATA #IMPLIED>\n";
	    print $inSock "<!ATTLIST HOST DMAX CDATA #IMPLIED>\n";
	    print $inSock "<!ATTLIST HOST GMOND_STARTED CDATA #IMPLIED>\n";
	    print $inSock "<!ELEMENT METRIC EMPTY>\n";
	    print $inSock "<!ATTLIST METRIC NAME CDATA #REQUIRED>\n";
	    print $inSock "<!ATTLIST METRIC VAL CDATA #REQUIRED>\n";
	    print $inSock "<!ATTLIST METRIC TYPE (string | int8 | uint8 | int16 | uint16 | int32 | uint32 | float | double | timestamp) #REQUIRED>\n";
	    print $inSock "<!ATTLIST METRIC UNITS CDATA #IMPLIED>\n";
	    print $inSock "<!ATTLIST METRIC TN CDATA #IMPLIED>\n";
	    print $inSock "<!ATTLIST METRIC TMAX CDATA #IMPLIED>\n";
	    print $inSock "<!ATTLIST METRIC DMAX CDATA #IMPLIED>\n";
	    print $inSock "<!ATTLIST METRIC SLOPE (zero | positive | negative | both | unspecified) #IMPLIED>\n";
	    print $inSock "<!ATTLIST METRIC SOURCE (gmond | gmetric) #REQUIRED>\n";
	    print $inSock "<!ELEMENT HOSTS EMPTY>\n";
	    print $inSock "<!ATTLIST HOSTS UP CDATA #REQUIRED>\n";
	    print $inSock "<!ATTLIST HOSTS DOWN CDATA #REQUIRED>\n";
	    print $inSock "<!ATTLIST HOSTS SOURCE (gmond | gmetric | gmetad) #REQUIRED>\n";
	    print $inSock "<!ELEMENT METRICS EMPTY>\n";
	    print $inSock "<!ATTLIST METRICS NAME CDATA #REQUIRED>\n";
	    print $inSock "<!ATTLIST METRICS SUM CDATA #REQUIRED>\n";
	    print $inSock "<!ATTLIST METRICS NUM CDATA #REQUIRED>\n";
	    print $inSock "<!ATTLIST METRICS TYPE (string | int8 | uint8 | int16 | uint16 | int32 | uint32 | float | double | timestamp) #REQUIRED>\n";
	    print $inSock "<!ATTLIST METRICS UNITS CDATA #IMPLIED>\n";
	    print $inSock "<!ATTLIST METRICS SLOPE (zero | positive | negative | both | unspecified) #IMPLIED>\n";
	    print $inSock "<!ATTLIST METRICS SOURCE (gmond | gmetric) #REQUIRED>\n";
	    print $inSock "]>\n";
	    print $inSock "<GANGLIA_XML VERSION=\"2.5.6\" SOURCE=\"gmond\">\n";
	    print $inSock "<CLUSTER NAME=\"".$clusterName."\" LOCALTIME=\"".$currentTime."\" OWNER=\"unspecified\" LATLONG=\"unspecified\" URL=\"unspecified\">\n";

#
# connect to mysql
#
	    $dbHandler = DBI->connect("DBI:mysql:$database:$DBHOST", $DBUSER, $dbpassword)
		or die "Couldn't connect to database: " . DBI->errstr;

    # This query is mandatory to support the next query performed in the script (see explanation at the beginning of the file)
      my $setUpDb = $dbHandler->prepare("SET GLOBAL sql_mode=?")
    or die "Couldn't prepare statement: " . $dbHandler->errstr;
      $setUpDb->execute("STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION")
    or die "Couldn't execute statement: " . $setUpDb->errstr;

      $dbHandler->disconnect;
#
# SELECT statement
# This retreives max(lastalive) group by name, ipaddr
#

	    #
	    # retreive workers group by name, ipaddr
	    #
	    #&debug("ReqMaxAlive");
#	    my $reqWorker = $dbHandler->prepare("select uid, os as osrelease, cpunb, cpuspeed, cputype as machinetype, totalswap as swaptotal, totalmem as memtotal,name, ipaddr, unix_timestamp(now())-unix_timestamp(lastalive) as delai,active,available,pilotjob,nbjobs from hosts where isdeleted='false' and (unix_timestamp(now())-unix_timestamp(lastalive) < 1000)")

      $dbHandler = DBI->connect("DBI:mysql:$database:$DBHOST", $DBUSER, $dbpassword)
    or die "Couldn't connect to database: " . DBI->errstr;

#	    my $reqWorker = $dbHandler->prepare("select uid, os as osrelease, cpunb, cpuspeed, cputype as machinetype, totalswap as swaptotal, totalmem as memtotal,name, totaltmp,freetmp,ipaddr, unix_timestamp(now())-unix_timestamp(lastalive) as delai,active,available,pilotjob,nbjobs,pendingjobs,runningjobs,errorjobs from hosts where isdeleted='false' and (unix_timestamp(now())-unix_timestamp(lastalive) < 1000)")
#		or die "Couldn't prepare statement: " . $dbHandler->errstr;
	    my $reqWorker = $dbHandler->prepare("select uid,lastalive, os as osrelease, cpunb, max(poolworksize) as poolworksize, cpuspeed, cputype as machinetype, totalswap as swaptotal, totalmem as memtotal,name, totaltmp,freetmp,ipaddr,natedipaddr ,unix_timestamp(now())-unix_timestamp(lastalive) as delai,active,available,pilotjob,sum(nbjobs) as nbjobs,sum(pendingjobs) as pendingjobs,sum(runningjobs) as runningjobs ,sum(errorjobs) as errorjobs from hosts where isdeleted='false' and (unix_timestamp(now())-unix_timestamp(lastalive) < 4000) group by name order by lastalive")
		or die "Couldn't prepare statement: " . $dbHandler->errstr;
	    $reqWorker->execute()
		or die "Couldn't execute statement: " . $reqWorker->errstr;
	    #&debug("ReqMaxAlive executed");

	    while (my $worker = $reqWorker->fetchrow_hashref) {

		#&debug("Alive = ".$alive->{name});

		my $tn = 0;

		if($worker->{delai} > 1000) {
		    &debug("Worker.delai > 1000");
		    next;
		}

		&debug("Worker = ".$worker->{name});

		print $inSock "<HOST NAME=\"".$worker->{name}."\"";
		print $inSock " IP=\"".$worker->{ipaddr}."\"";
		print $inSock " REPORTED=\"".$currentTime."\"";
		print $inSock " TN=\"".$tn."\"";
		print $inSock " TMAX=\"900\"";
		print $inSock " DMAX=\"0\"";
		print $inSock " LOCATION=\"unspecified\"";
		print $inSock " GMOND_STARTED=\"" .$startTime."\">\n";

		print $inSock "<METRIC NAME=\"cpu_num\" VAL=\"".$worker->{cpunb}."\"";
		print $inSock " TYPE=\"uint16\"";
		print $inSock " UNITS=\"cores\"";
		print $inSock " TN=\"0\"";
		print $inSock " TMAX=\"1200\"";
		print $inSock " DMAX=\"0\"";
		print $inSock " SLOPE=\"zero\"";
		print $inSock " SOURCE=\"gmond\" />\n";

		print $inSock "<METRIC NAME=\"poolworksize\" VAL=\"".$worker->{poolworksize}."\"";
		print $inSock " TYPE=\"uint16\"";
		print $inSock " UNITS=\"cores\"";
		print $inSock " TN=\"0\"";
		print $inSock " TMAX=\"1200\"";
		print $inSock " DMAX=\"0\"";
		print $inSock " SLOPE=\"zero\"";
		print $inSock " SOURCE=\"gmond\" />\n";

		print $inSock "<METRIC NAME=\"cpu_speed\" VAL=\"".$worker->{cpuspeed}."\"";
		print $inSock " TYPE=\"uint16\"";
		print $inSock " UNITS=\"MHz\"";
		print $inSock " TN=\"0\"";
		print $inSock " TMAX=\"1200\"";
		print $inSock " DMAX=\"0\"";
		print $inSock " SLOPE=\"zero\"";
		print $inSock " SOURCE=\"gmond\" />\n";

		print $inSock "<METRIC NAME=\"os_release\" VAL=\"".$worker->{osrelease}."\"";
		print $inSock " TYPE=\"string\"";
		print $inSock " UNITS=\"\"";
		print $inSock " TN=\"0\"";
		print $inSock " TMAX=\"1200\"";
		print $inSock " DMAX=\"0\"";
		print $inSock " SLOPE=\"zero\"";
		print $inSock " SOURCE=\"gmond\" />\n";

		print $inSock "<METRIC NAME=\"machine_type\" VAL=\"".$worker->{machinetype}."\"";
		print $inSock " TYPE=\"string\"";
		print $inSock " UNITS=\"\"";
		print $inSock " TN=\"0\"";
		print $inSock " TMAX=\"1200\"";
		print $inSock " DMAX=\"0\"";
		print $inSock " SLOPE=\"zero\"";
		print $inSock " SOURCE=\"gmond\" />\n";

		print $inSock "<METRIC NAME=\"disk_total\" VAL=\"".$worker->{totaltmp}."\"";
		print $inSock " TYPE=\"uint32\"";
		print $inSock " UNITS=\"BYTES\"";
		print $inSock " TN=\"0\"";
		print $inSock " TMAX=\"1200\"";
		print $inSock " DMAX=\"0\"";
		print $inSock " SLOPE=\"zero\"";
		print $inSock " SOURCE=\"gmond\" />\n";

		print $inSock "<METRIC NAME=\"disk_free\" VAL=\"".$worker->{freetmp}."\"";
		print $inSock " TYPE=\"uint32\"";
		print $inSock " UNITS=\"BYTES\"";
		print $inSock " TN=\"0\"";
		print $inSock " TMAX=\"1200\"";
		print $inSock " DMAX=\"0\"";
		print $inSock " SLOPE=\"zero\"";
		print $inSock " SOURCE=\"gmond\" />\n";

		print $inSock "<METRIC NAME=\"mem_total\" VAL=\"".$worker->{memtotal}."\"";
		print $inSock " TYPE=\"uint32\"";
		print $inSock " UNITS=\"BYTES\"";
		print $inSock " TN=\"0\"";
		print $inSock " TMAX=\"1200\"";
		print $inSock " DMAX=\"0\"";
		print $inSock " SLOPE=\"zero\"";
		print $inSock " SOURCE=\"gmond\" />\n";

		print $inSock "<METRIC NAME=\"swap_total\" VAL=\"".$worker->{swaptotal}."\"";
		print $inSock " TYPE=\"uint32\"";
		print $inSock " UNITS=\"BYTES\"";
		print $inSock " TN=\"0\"";
		print $inSock " TMAX=\"1200\"";
		print $inSock " DMAX=\"0\"";
		print $inSock " SLOPE=\"zero\"";
		print $inSock " SOURCE=\"gmond\" />\n";

		print $inSock "<METRIC NAME=\"ALIVEDELAI\" VAL=\"".$worker->{delai}."\"";
		print $inSock " TYPE=\"uint32\"";
		print $inSock " UNITS=\"seconds\"";
		print $inSock " TN=\"".$tn."\"";
		print $inSock " TMAX=\"900\"";
		print $inSock " DMAX=\"0\"";
		print $inSock " SLOPE=\"both\"";
		print $inSock " SOURCE=\"gmetric\" />\n";

		my $aliveBoolean = 1;
		$aliveBoolean = 0 if $worker->{delai} > 900;
		print $inSock "<METRIC NAME=\"ALIVE\" VAL=\"".$aliveBoolean."\"";
		print $inSock " TYPE=\"uint8\"";
		print $inSock " UNITS=\"boolean\"";
		print $inSock " TN=\"".$tn."\"";
		print $inSock " TMAX=\"900\"";
		print $inSock " DMAX=\"0\"";
		print $inSock " SLOPE=\"both\"";
		print $inSock " SOURCE=\"gmetric\" />\n";

		my $myBoolean = 0;
		$myBoolean = 1 if ($worker->{pilotjob} eq "true");
		print $inSock "<METRIC NAME=\"PILOTJOB\" VAL=\"".$myBoolean."\"";
		print $inSock " TYPE=\"uint8\"";
		print $inSock " UNITS=\"boolean\"";
		print $inSock " TN=\"".$tn."\"";
		print $inSock " TMAX=\"900\"";
		print $inSock " DMAX=\"0\"";
		print $inSock " SLOPE=\"both\"";
		print $inSock " SOURCE=\"gmetric\" />\n";

		$myBoolean = 0;
		$myBoolean = 1 if ($worker->{active} eq "true");
		$myBoolean = 0 if ($aliveBoolean == 0);
		print $inSock "<METRIC NAME=\"ACTIVE\" VAL=\"".$myBoolean."\"";
		print $inSock " TYPE=\"uint8\"";
		print $inSock " UNITS=\"boolean\"";
		print $inSock " TN=\"".$tn."\"";
		print $inSock " TMAX=\"900\"";
		print $inSock " DMAX=\"0\"";
		print $inSock " SLOPE=\"both\"";
		print $inSock " SOURCE=\"gmetric\" />\n";

		$myBoolean = 0;
		$myBoolean = 1 if ($worker->{available} eq "true");
		$myBoolean = 0 if ($aliveBoolean == 0);
		print $inSock "<METRIC NAME=\"AVAILABLE\" VAL=\"".$myBoolean."\"";
		print $inSock " TYPE=\"uint8\"";
		print $inSock " UNITS=\"boolean\"";
		print $inSock " TN=\"".$tn."\"";
		print $inSock " TMAX=\"900\"";
		print $inSock " DMAX=\"0\"";
		print $inSock " SLOPE=\"both\"";
		print $inSock " SOURCE=\"gmetric\" />\n";

		print $inSock "<METRIC NAME=\"COMPLETEDS\" VAL=\"".$worker->{nbjobs}."\"";
		print $inSock " TYPE=\"uint32\"";
		print $inSock " UNITS=\"Jobs\"";
		print $inSock " TN=\"".$tn."\"";
		print $inSock " TMAX=\"900\"";
		print $inSock " DMAX=\"0\"";
		print $inSock " SLOPE=\"both\"";
		print $inSock " SOURCE=\"gmetric\" />\n";

		print $inSock "<METRIC NAME=\"RUNNINGS\" VAL=\"".$worker->{runningjobs}."\"";
		print $inSock " TYPE=\"uint32\"";
		print $inSock " UNITS=\"Jobs\"";
		print $inSock " TN=\"".$tn."\"";
		print $inSock " TMAX=\"900\"";
		print $inSock " DMAX=\"0\"";
		print $inSock " SLOPE=\"both\"";
		print $inSock " SOURCE=\"gmetric\" />\n";

		print $inSock "<METRIC NAME=\"ERRORS\" VAL=\"".$worker->{errorjobs}."\"";
		print $inSock " TYPE=\"uint32\"";
		print $inSock " UNITS=\"Jobs\"";
		print $inSock " TN=\"".$tn."\"";
		print $inSock " TMAX=\"900\"";
		print $inSock " DMAX=\"0\"";
		print $inSock " SLOPE=\"both\"";
		print $inSock " SOURCE=\"gmetric\" />\n";

		print $inSock "</HOST>\n";
	    }
	    #
	    # Print Ganglia trailer
	    #
	    print $inSock "</CLUSTER>\n</GANGLIA_XML>\n";

	    $dbHandler->disconnect;

	    close($inSock);
	    &debug("Connexion closed");

	    #
	    # end of child process
	    #
	    exit(0);
	}
    }
}

#
# End of file
#
