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




#
# File    : xtremweb.jra2.pl
# Author  : Oleg Lodygens (lodygens at lal.in2p3.fr)
# Date    : Novembre 26th, 2008
# Purpose : EDGeS monitoring; this script downloads XWHEP servers datas and stores them to a MySQL database
# Params  : [-h|--help] [–d DEBUGLEVEL] [-db DBname] [-u DB user] [-p DB user password]
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
use XML::Simple;
use Data::Dumper;
use UNIVERSAL 'isa';

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
my $scriptName = `basename $0`;
my $ROOTDIR = `dirname $0`;
my $HEAD="";
chomp ($scriptName);
chomp ($ROOTDIR);
$ROOTDIR = `pwd` if ($ROOTDIR eq ".");
chomp ($ROOTDIR);
$ROOTDIR="$ROOTDIR/..";
my $RANDOM = randomInteger();

#
# XWHEP directories
#
my $bindir="$ROOTDIR/bin";
my $confdir="$ROOTDIR/conf";
my $libdir="$ROOTDIR/lib";
my $keydir="$ROOTDIR/keystore";
my $workerkeys="$keydir/xwhepworker.keys";

#
# XWHEP variables
#
my $WORKERCLASS="xtremweb.worker.Worker";
my $JARFILENAME="xtremweb.jar";
my $JARFILE="$libdir/$JARFILENAME";

#
# JAVA
#
my $JAVA="java -Djavax.net.ssl.trustStore=$workerkeys -cp $JARFILE $WORKERCLASS ";

#
# ARCH values
#
my $PPC="ppc";
my $INTEL="ix86";
my $LINUX="linux";
my $WIN32="win32";
my $MACOSX="macosx";

#
# MySQL configuration
#
my $user = "root";
my $password = "";
my $DBHOST = "localhost";
my $database = "xtremweb";
my $clusterName = "XWHEP";


#
# XWHEP scripts
#
my $xwapps="$bindir/xwapps";
my $xwalive="$bindir/xwalive";
my $xwdatas="$bindir/xwdatas";
my $xwdownload="$bindir/xwdownload";
my $xwusers="$bindir/xwusers";
my $xwusergroups="$bindir/xwusergroups";
my $xwrm="$bindir/xwrm";
my $xwchmod="$bindir/xwchmod";
my $xwsendwork="$bindir/xwsendwork";
my $xwsendapp="$bindir/xwsendapp";
my $xwsenddata="$bindir/xwsenddata";
my $xwsenduser="$bindir/xwsenduser";
my $xwsendusergroup="$bindir/xwsendusergroup";
my $xwsubmit="$bindir/xwsubmit";
my $xwworks="$bindir/xwworks";

#
# Files
#
my $workerCfg;
my $jdlFileName;
my $shFileName;
my $tmpFileName;
my $xmlFileName;
my $userProxyFile;

#
# This hashtable stores EGEE job IDs
#
my %pilotJobs = ();
#
# This hashtable stores XWHEP jobs
#
my %xwhepJobs = ();


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
    cleanup ("Caught SIGINT", 1);
}


#
# This prints out usage and exits
#
sub usage {
    print "Usage : $scriptName [-h|--help] [-d level] \n";
    print "\t -h|--help    : gets this help\n";
    print "\t -d  0|1|2|3  : sets debug level\n";
    print "\t -db database : DB name\n";
    print "\t -u  user     : DB user name\n";
    print "\t -p  password : DB user password\n";
    exit 1;
}


#
# This returns a random integer < 10K
#
sub randomInteger {
    return int(rand(10000));
}
#
# This gets date and time
# @return `date "+%m/%d/%y %H:%M:%S"`
sub dateAndTime {
    my $d;
    chomp($d=`date "+%m/%d/%y %H:%M:%S"`);
    return $d;
}
#
# This prints the script name, the current date and the provided message
# @param msg to print
#
sub printTimed {
    my $text = "$HEAD $scriptName ".dateAndTime()." @_\n";
    print $text;
}
#
# This prints debugs
# @param msg to print
#
sub debugLevel {
    return "debug" if ($debug <= $DEBUG);
    return "info"  if ($debug <= $INFO);
    return "warn"  if ($debug <= $WARN);
    return "error" if ($debug <= $ERROR);
}
#
# This prints debugs
# @param msg to print
#
sub debug {
    printTimed "[DEBUG] : @_" if ($debug <= $DEBUG);
}
#
# This prints infos
# @param msg to print
#
sub info {
    printTimed "[INFO] : @_" if ($debug <= $INFO);
}
#
# This prints warnings
# @param msg to print
#
sub warn {
    printTimed "WARN : @_" if ($debug <= $WARN);
}
#
# This prints errors
# @param msg to print
#
sub error {
    printTimed "ERROR : @_" if ($debug <= $ERROR);
}
#
# This prints fatal errors
# This stops the script immediatly
# @param msg to print
#
sub fatal {
    cleanup ("FATAL : @_", 1);
}


#
# this prints 1st header
# @param msg to print
#
sub header1 {
    $HEAD = "";
    my $text = "** @_\n";
    print $text;
}
#
# this prints 2nd level header
# @param msg to print
#
sub header2 {
    $HEAD = "   ";
    my $text = "** @_\n";
    print $text;
}


#
# This removes temp files and datas
# This stops the script immediatly
#
sub deleteFiles {

    return if ($debug <= $DEBUG);

    `rm -f $workerCfg`     if( -f $workerCfg );
    `rm -f $shFileName`    if( -f $shFileName );
    `rm -f $jdlFileName`   if( -f $jdlFileName );
    `rm -f $tmpFileName`   if( -f $tmpFileName );
    `rm -f $xmlFileName`   if( -f $xmlFileName );
}
#
# This removes temp files and datas
# This stops the script immediatly
# @param : a String to display as message
# @param : this script return code
#
sub cleanup {
    my $msg=shift;
    my $RC=shift;

    header1 ("Cleaning up : ".dateAndTime()."  ($msg)");
    &deleteFiles();

    exit $RC;
}

#
# This sends a command to XWHEP server
# On error this stops the script immediatly
# @param main message always displayed
# @param errormsg displayed on error
# @return the stdout of the command 
#
sub sendCommand {
    my $msg=shift;
    my $errormsg=shift;

    header2 ($msg);

    my $CMD="@_";
    $CMD="@_";

    debug ("Executing $CMD");

    my $ret;
    chomp($ret=`$CMD`);

    my $RC = $?;

    if ($RC != 0) {
	error ($errormsg);
	$ret = "";
    }
    return $ret;
}

#
# This writes an XWHEP work description to XML file
# @param the XML file name
# @param the work to write to file
#
sub workToXMLFile {
    my $fileName = shift;
    my $xwhepWork = shift;
    my $XML;
    `rm -f $fileName`;
    &debug("XML = ".$fileName);
    open (XML, "> ".$fileName);
    print XML "<work ";
    print XML " uid='".$xwhepWork->{uid}."'"                         if($xwhepWork->{uid} ne "" );
    print XML " userproxy='".$xwhepWork->{userproxy}."'"             if($xwhepWork->{userproxy} ne "" );
    print XML " sessionuid='".$xwhepWork->{sessionuid}."'"           if($xwhepWork->{sessionuid} ne "" );
    print XML " groupuid='".$xwhepWork->{groupuid}."'"               if($xwhepWork->{groupuid} ne "" );
    print XML " expectedhostuid='".$xwhepWork->{expectedhostuid}."'" if($xwhepWork->{expectedhostuid} ne "" );
    print XML " accessrights='".$xwhepWork->{accessrights}."'"       if($xwhepWork->{accessrights} ne "" );
    print XML " isservice='".$xwhepWork->{isservice}."'"             if($xwhepWork->{isservice} ne "" );
    print XML " isdeleted='".$xwhepWork->{isdeleted}."'"             if($xwhepWork->{isdeleted} ne "" );
    print XML " label='".$xwhepWork->{label}."'"                     if($xwhepWork->{label} ne "" );
    print XML " appuid='".$xwhepWork->{appuid}."'"                   if($xwhepWork->{appuid} ne "" );
    print XML " useruid='".$xwhepWork->{useruid}."'"                 if($xwhepWork->{useruid} ne "" );
    print XML " status='".$xwhepWork->{status}."'"                   if($xwhepWork->{status} ne "" );
    print XML " returncode='".$xwhepWork->{returncode}."'"           if($xwhepWork->{returncode} ne "" );
    print XML " server='".$xwhepWork->{server}."'"                   if($xwhepWork->{server} ne "" );
    print XML " cmdline='".$xwhepWork->{cmdline}."'"                 if($xwhepWork->{cmdline} ne "" );
    print XML " stdinuri='".$xwhepWork->{stdinuri}."'"               if($xwhepWork->{stdinuri} ne "" );
    print XML " dirinuri='".$xwhepWork->{dirinuri}."'"               if($xwhepWork->{dirinuri} ne "" );
    print XML " resulturi='".$xwhepWork->{resulturi}."'"             if($xwhepWork->{resulturi} ne "" );
    print XML " arrivaldate='".$xwhepWork->{arrivaldate}."'"         if($xwhepWork->{arrivaldate} ne "" );
    print XML " completeddate='".$xwhepWork->{completeddate}."'"     if($xwhepWork->{completeddate} ne "" );
    print XML " error_msg='".$xwhepWork->{error_msg}."'"             if($xwhepWork->{error_msg} ne "" );
    print XML " sendtoclient='".$xwhepWork->{sendtoclient}."'"       if($xwhepWork->{sendtoclient} ne "" );
    print XML " local='".$xwhepWork->{local}."'"                     if($xwhepWork->{local} ne "" );
    print XML " active='".$xwhepWork->{active}."'"                   if($xwhepWork->{active} ne "" );
    print XML " replicated='".$xwhepWork->{replicated}."'"           if($xwhepWork->{replicated} ne "" );
    print XML " maxretry='".$xwhepWork->{maxretry}."'"               if($xwhepWork->{maxretry} ne "" );
    print XML " minmemory='".$xwhepWork->{minmemory}."'"             if($xwhepWork->{minmemory} ne "" );
    print XML " mincpuspeed='".$xwhepWork->{mincpuspeed}."'"         if($xwhepWork->{mincpuspeed} ne "" );
    print XML " />";
    close(XML);
}

#
# This removes CR LF, ', " from param
# @param the string to clean
# @return the cleaned string
#
sub cleanString {
    my $str = shift;
    $str =~ s/\r\n|\n/./g;
    $str =~ s/\'//g;
    $str =~ s/\"//g;
    return $str;
}


# ============================================== #
#                  Main                          #
# ============================================== #


#
# parse command line args
#
my $arg;

my $privjobs=1;
my $groupjobs=1;
my $pubjobs=1;

while ($arg = shift) {
    if (($arg eq '-h') || ($arg eq '--help')) {
        usage;
    }
    if ($arg eq '-d') {
        $debug = shift;
    }
    if ($arg eq '-u') {
        $user = shift;
    }
    if ($arg eq '-p') {
        $password = shift;
    }
    if ($arg eq '-db') {
        $database = shift;
    }
}

#
# connect to MySQL
#
my $dbh = DBI->connect("DBI:mysql:$database:$DBHOST", $user, $password)
    or die "Couldn't connect to database: " . DBI->errstr;

#
# MySQL statements
#
# This retreives an app
my $reqGetApp = $dbh->prepare("select * from apps where uid=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This retreives a data
my $reqGetData = $dbh->prepare("select * from datas where uid=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This retreives a group
my $reqGetGroup = $dbh->prepare("select * from groups where uid=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This retreives a worker
my $reqGetHost = $dbh->prepare("select * from hosts where uid=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This retreives a session
my $reqGetSession = $dbh->prepare("select * from sessions where uid=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This retreives a task
my $reqGetTask = $dbh->prepare("select * from tasks where uid=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This retreives an usergroup
my $reqGetUserGroup = $dbh->prepare("select * from usergroups where uid=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This retreives an user
my $reqGetUser = $dbh->prepare("select * from users where uid=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This retreives a work
my $reqGetWork = $dbh->prepare("select * from works where uid=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;

# This inserts an app
my $reqInsertApp = $dbh->prepare("insert into apps set uid=?,owneruid=?,name=?,isservice=?,accessrights=?,avgexectime=?,minmemory=?,mincpuspeed=?,njobs=?,defaultstdinuri=?,basedirinuri=?defaultdirinuri=?,linux_ix86uri=?,linux_amd64uri=?,linux_ppcuri=?,macos_ix86uri=?,macos_ppcuri=?,win32_ix86uri=?,win32_amd64uri=?,javauri=?,osf1_alphauri=?,osf1_sparcuri=?,solaris_alphauri=?,solaris_sparcuri=?,ldlinux_ix86uri=?,ldlinux_amd64uri=?,ldlinux_ppcuri=?,ldmacos_ix86uri=?,ldmacos_ppcuri=?,ldwin32_ix86uri=?,ldwin32_amd64uri=?,ldjavauri=?,ldosf1_alphauri=?,ldosf1_sparcuri=?,ldsolaris_alphauri=?,ldsolaris_sparcuri=?,isdeleted=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This inserts a data
my $reqInsertData = $dbh->prepare("insert into datas set uid=?,owneruid=?,uri=?,accessrights=?,name=?,links=?,accessdate=?,insertiondate=?,status=?,type=?,os=?,cpu=?,md5=?,size=?,sendtoclient=?,replicated=?,isdeleted=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This inserts a group
my $reqInsertGroup = $dbh->prepare("insert into groups set uid=?,name=?,sessionuid=?,clientuid=?,isdeleted=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This inserts a worker
my $reqInsertHost = $dbh->prepare("insert into hosts set uid=?,nbjobs=?,timeout=?,avgexectime=?,lastalive=?,name=?,nbconnections=?,natedipaddr=?,ipaddr=?,hwaddr=?,timezone=?,os=?,cputype=?,cpunb=?,cpumodel=?,cpuspeed=?,totalmem=?,totalswap=?,timeshift=?,avgping=?,nbping=?,uploadbandwith=?,downloadbandwidth=?,owneruid=?,project=?,active=?,available=?,acceptbin=?,version=?,traces=?,userproxy=?,isdeleted=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This inserts a session
my $reqInsertSession = $dbh->prepare("insert into sessions set uid=?,name=?,clientuid=?,isdeleted=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This inserts a task
my $reqInsertTask = $dbh->prepare("insert into tasks set uid=?,hostuid=?,trial=?,status=?,insertiondate=?,startdate=?,laststartdate=?,alivecount=?,lastalive=?,removaldate=?,duration=?,isdeleted=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This inserts an usergroup
my $reqInsertUserGroup = $dbh->prepare("insert into usergroups set uid=?,label=?,project=?,isdeleted=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This inserts an user
my $reqInsertUser = $dbh->prepare("insert into users set uid=?,usergroupuid=?,nbjobs=?,certificate=?,login=?,password=?,email=?,fname=?,lname=?,country=?,rights=?,isdeleted=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This inserts a work
my $reqInsertWork = $dbh->prepare("insert into works set uid=?,userproxy=?,expectedhostuid=?,accessrights=?,sessionuid=?,groupuid=?,label=?,appuid=?,useruid=?,status=?,maxretry=?,minmemory=?,mincpuspeed=?,returncode=?,server=?,cmdline=?,stdinuri=?,dirinuri=?,resulturi=?,arrivaldate=?,completeddate=?,resultdate=?,error_msg=?,sendtoclient=?,local=?,active=?,replicated=?,isservice=?,isdeleted=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;


# This updates an app
my $reqUpdateApp = $dbh->prepare("update apps set uid=?,owneruid=?,name=?,isservice=?,accessrights=?,avgexectime=?,minmemory=?,mincpuspeed=?,njobs=?,defaultstdinuri=?,basedirinuri=?defaultdirinuri=?,linux_ix86uri=?,linux_amd64uri=?,linux_ppcuri=?,macos_ix86uri=?,macos_ppcuri=?,win32_ix86uri=?,win32_amd64uri=?,javauri=?,osf1_alphauri=?,osf1_sparcuri=?,solaris_alphauri=?,solaris_sparcuri=?,ldlinux_ix86uri=?,ldlinux_amd64uri=?,ldlinux_ppcuri=?,ldmacos_ix86uri=?,ldmacos_ppcuri=?,ldwin32_ix86uri=?,ldwin32_amd64uri=?,ldjavauri=?,ldosf1_alphauri=?,ldosf1_sparcuri=?,ldsolaris_alphauri=?,ldsolaris_sparcuri=?,isdeleted=? where uid=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This updates a data
my $reqUpdateData = $dbh->prepare("update datas set uid=?,owneruid=?,uri=?,accessrights=?,name=?,links=?,accessdate=?,insertiondate=?,status=?,type=?,os=?,cpu=?,md5=?,size=?,sendtoclient=?,replicated=?,isdeleted=? where uid=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This updates a group
my $reqUpdateGroup = $dbh->prepare("update groups set uid=?,name=?,sessionuid=?,clientuid=?,isdeleted=? where uid=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This updates a worker
my $reqUpdateHost = $dbh->prepare("update hosts set uid=?,nbjobs=?,timeout=?,avgexectime=?,lastalive=?,name=?,nbconnections=?,natedipaddr=?,ipaddr=?,hwaddr=?,timezone=?,os=?,cputype=?,cpunb=?,cpumodel=?,cpuspeed=?,totalmem=?,totalswap=?,timeshift=?,avgping=?,nbping=?,uploadbandwith=?,downloadbandwidth=?,owneruid=?,project=?,active=?,available=?,acceptbin=?,version=?,traces=?,userproxy=?,isdeleted=? where uid=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This updates a session
my $reqUpdateSession = $dbh->prepare("update sessions set uid=?,name=?,clientuid=?,isdeleted=? where uid=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This updates a task
my $reqUpdateTask = $dbh->prepare("update tasks set uid=?,hostuid=?,trial=?,status=?,insertiondate=?,startdate=?,laststartdate=?,alivecount=?,lastalive=?,removaldate=?,duration=?,isdeleted=? where uid=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This updates an usergroup
my $reqUpdateUserGroup = $dbh->prepare("update usergroups set uid=?,label=?,project=?,isdeleted=? where uid=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This updates an user
my $reqUpdateUser = $dbh->prepare("update users set uid=?,usergroupuid=?,nbjobs=?,certificate=?,login=?,password=?,email=?,fname=?,lname=?,country=?,rights=?,isdeleted=? where uid=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;
# This updates a work
my $reqUpdateWork = $dbh->prepare("update works set uid=?,userproxy=?,expectedhostuid=?,accessrights=?,sessionuid=?,groupuid=?,label=?,appuid=?,useruid=?,status=?,maxretry=?,minmemory=?,mincpuspeed=?,returncode=?,server=?,cmdline=?,stdinuri=?,dirinuri=?,resulturi=?,arrivaldate=?,completeddate=?,resultdate=?,error_msg=?,sendtoclient=?,local=?,active=?,replicated=?,isservice=?,isdeleted=? where uid=?")
    or die "Couldn't prepare statement: " . $dbh->errstr;



#
# retreiving config files
#
my $CFG;
opendir (CFG, $confdir) or die "Can't retreive config files";
my @fichiers = readdir (CFG);
closedir (CFG);

#
# this hashtable stores config files for each server found
#
my %servers = ();
my $serverfound = 0;

#
# loop on all config files
#
foreach my $entree (@fichiers){

    unless ($entree =~ /xtremweb.client.conf/ ) {
	&debug ("not a client conf file : ".$entree);
	next;
    }
    &debug ("client conf file : ".$entree);
    my $delimiteur = "=";
    my $server =`grep -E '^[[:space:]]*dispatcher.servers[[:space:]]*=' $confdir/$entree`;
    if($? != 0) {
	$server =`grep -E '^[[:space:]]*dispatcher.servers[[:space:]]*:' $confdir/$entree`;
	$delimiteur = ":";
    }
    if($server eq "") {
	&warn($entree." is not used : it has no dispatcher.servers");
	next;
    }
    my @splitarray = split($delimiteur, $server);
    $server = $splitarray[1];
    chomp($server);

    $delimiteur = "=";
    my $keystore =`grep -E '^[[:space:]]*SSLKeyStore[[:space:]]*=' $confdir/$entree`;
    if($? != 0) {
	$keystore =`grep -E '^[[:space:]]*SSLKeyStore[[:space:]]*:' $confdir/$entree`;
	$delimiteur = ":";
    }
    if($keystore eq "") {
	&warn($entree." is not used : it has no keyStore");
	next;
    }
    @splitarray = split($delimiteur, $keystore);
    $keystore = $splitarray[1];
    chomp($keystore);
    if( ! -f $keystore) {
	&warn($entree." is not used : can't find keyStore file ".$keystore);
	next;
    }
    
    $delimiteur = "=";
    my $login =`grep -E '^[[:space:]]*login[[:space:]]*=' $confdir/$entree`;
    if($? != 0) {
	$login =`grep -E '^[[:space:]]*login[[:space:]]*:' $confdir/$entree`;
	$delimiteur = ":";
    }
    if($login eq "") {
	&warn($entree." is not used : it has no login");
	next;
    }
    @splitarray = split($delimiteur, $login);
    $login = $splitarray[1];
    chomp($login);

    $delimiteur = "=";
    my $password =`grep -E '^[[:space:]]*password[[:space:]]*=' $confdir/$entree`;
    if($? != 0) {
	$password =`grep -E '^[[:space:]]*password[[:space:]]*:' $confdir/$entree`;
	$delimiteur = ":";
    }
    if($password eq "") {
	&warn($entree." is not used : it has no password");
	next;
    }
    @splitarray = split($delimiteur, $password);
    $password = $splitarray[1];
    chomp($password);

    &debug($entree." : S = ".$server.", K = ".$keystore.", L = ".$login.", P = ".$password);

    my $enreg = {};
    $enreg->{clientconf} = $confdir."/".$entree;
    $enreg->{server} = $server;
    $enreg->{keystore} = $keystore;
    $enreg->{login} = $login;
    $enreg->{password} = $password;

    $servers{$server}->{configs} = $enreg;

    $serverfound++;
}

if($serverfound < 1) {
    &fatal("No config file found");
}

&info("Found ".$serverfound." config files");


#
# main loop
#
while(1) {

#
# loop on servers
#
    foreach my $server (keys %servers) {

	my $clientConfFile = $servers{$server}->{configs}->{clientconf};

#
# ALIVE signal : this signal server the monitor is here
#
	sendCommand ("Sending alive to ".$servers{$server}->{configs}->{server},
		     "Can't send alive signal",
		     $xwalive, "--xwconfig", $clientConfFile);

##########################################################
#
# This downloads works
# If you want to download applications, users etc
# copy/paste (up to line 683) the following and modify it accordingly
#
##########################################################
#
# retreive remote works
#
	my $cmdWorks = sendCommand ("Retreiving jobs",
				    "Can't retreive jobs",
				    $xwworks, "--xwconfig", $clientConfFile);

	if ($cmdWorks ne "" ) {
	    my $works = XMLin($cmdWorks);

	    my @worksArray = ();
	    if(isa($works->{work}, "ARRAY")) {
		@worksArray = @{$works->{work}};
	    }
	    else {
		@worksArray = $works->{work};
	    };

#
# Loop on retreived remote works 
#    
	    foreach my $remotework (@worksArray) {

		&debug("Work ".$remotework->{uid});

#
# work already on local DB ?
#
		$reqGetWork->execute($remotework->{uid})
		    or die "Couldn't execute statement: " . $reqGetWork->errstr;

		if (my $localwork = $reqGetWork->fetchrow_hashref) {
#
# yes : update work on local DB
#
		    &debug("Updating work");
		    $reqUpdateWork->execute($remotework->{uid},$remotework->{userproxy},$remotework->{expectedhostuid},$remotework->{accessrights},$remotework->{sessionuid},$remotework->{groupuid},$remotework->{label},$remotework->{appuid},$remotework->{useruid},$remotework->{status},$remotework->{maxretry},$remotework->{minmemory},$remotework->{mincpuspeed},$remotework->{returncode},$remotework->{server},$remotework->{cmdline},$remotework->{stdinuri},$remotework->{dirinuri},$remotework->{resulturi},$remotework->{arrivaldate},$remotework->{completeddate},$remotework->{resultdate},$remotework->{error_msg},$remotework->{sendtoclient},$remotework->{local},$remotework->{active},$remotework->{replicated},$remotework->{isservice},$remotework->{isdeleted},$remotework->{uid})
			or &error("Couldn't execute statement: " . $reqInsertWork->errstr);
		}
		else {
#
# no, insert new work in local DB
#
		    &debug("Inserting work");
		    $reqInsertWork->execute($remotework->{uid},$remotework->{userproxy},$remotework->{expectedhostuid},$remotework->{accessrights},$remotework->{sessionuid},$remotework->{groupuid},$remotework->{label},$remotework->{appuid},$remotework->{useruid},$remotework->{status},$remotework->{maxretry},$remotework->{minmemory},$remotework->{mincpuspeed},$remotework->{returncode},$remotework->{server},$remotework->{cmdline},$remotework->{stdinuri},$remotework->{dirinuri},$remotework->{resulturi},$remotework->{arrivaldate},$remotework->{completeddate},$remotework->{resultdate},$remotework->{error_msg},$remotework->{sendtoclient},$remotework->{local},$remotework->{active},$remotework->{replicated},$remotework->{isservice},$remotework->{isdeleted})
			or &error("Couldn't execute statement: " . $reqInsertWork->errstr);
		}
	    }
	}
##########################################################
#
# End of work management
#
##########################################################
    }
#    header2 "Sleeping 5mn";
#    sleep 300;
    header2 "Sleeping 10s";
    sleep 10;
}


#
# Clean up
#
cleanup ("Ending : " . dateAndTime(), 0);


#
# End of file
#
