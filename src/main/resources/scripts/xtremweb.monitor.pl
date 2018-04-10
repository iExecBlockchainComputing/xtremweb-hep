#!/usr/bin/perl


#
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
# Date      : Feb, 2nd 2006
# Author    : Oleg Lodygensky (lodygens �  lal in2p3 fr)
# Platforms : Linux
#
# * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! *
# * This is not automatically installed by install process *
# * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! *
#
# This monitors XtremWeb processes and kills them if available memory (RAM + Swap) is less than 30%
#
# Parameters : -d to debug
#              -u to give a user name (default is xtremweb)
#              -fml to give free memory limit under which processes are killed (default is 30)
#

use strict;
use English;
use IO::Handle;
use File::Basename;
use DirHandle;
use integer;

# Synchronize the Perl IOs
autoflush STDIN 1;
autoflush STDERR 1;
autoflush STDOUT 1;

#
# trap CTRL+C
#
use sigtrap 'handler' => \&ctrlcHandler, 'INT';

#
# this is called whenever user hits CTRL+C
#
sub ctrlcHandler {
    print "Caught SIGINT\n";
    exit 1;
}


#---------------------  global variables  -----------------------------------
my $debug      = 0;
my $repertoire = dirname ($0);
my $hostname   =`uname -n`;
my $output     =$repertoire."/".$hostname."_log.txt";
my $userName   ="xtremweb";

my $FREEMEMLIMIT = 30;
my $USEDMEMLIMIT = 100 - $FREEMEMLIMIT;

my $FREE         = "/usr/bin/free";
my $SLEEP        = 2;

if ( -x "/usr/bin/free") {
    $FREE="/usr/bin/free";
}
else {
    if ( -x "/usr/sbin/free") {
        $FREE="/usr/sbin/free";
    }
    else {
        &fatal("Can't find 'free'");
    }
}

#
# debug levels
#
my $DEBUG = 0;
my $INFO  = 1;
my $WARN  = 2;
my $ERROR = 3;
my $debug = $INFO;



#----------------------------------------------------------------------------

#---------------------    local methods   -----------------------------------
sub help(){
    print "Usage: $0 [-h] [-d] [-u userName] [-fml xx]\n";
    print "\t -h to get this help \n";
    print "\t -d to turn debug mode on\n";
    print "\t -u userName to manage userName user processes (default is xtremweb)\n";
    print "\t -fml to give free memory limit under which processes are killed (default is 10)\n";
    exit 1;
}


# Perl trim function to remove whitespace from the start and end of the string
sub trim($)
{
    my $string = shift;
    $string =~ s/^\s+//;
    $string =~ s/\s+$//;
    return $string;
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

#
# This prints out fatal error and exits
#
sub fatal($) {
    print "FATAL : @_\n";
    exit 1;
}


#----------------------------------------------------------------------------

#---------------------     main method    -----------------------------------
#----------------------------------------------------------------------------
my $arg;
while ($arg = shift) {
    if ($arg eq '-h') {
        help();
    }
    if ($arg eq '-d') {
        $debug = $DEBUG;
    }
    if ($arg eq '-u') {
        $userName = shift;
    }
    if ($arg eq '-fml') {
        $FREEMEMLIMIT = shift;
    }
}

$USEDMEMLIMIT = 100 - $FREEMEMLIMIT;

while (1) {
#
# Retreive mem info
#
    open (INPUT, "$FREE -t | grep Total | sed 's/:/ /g' | sed 's/^[[:space:]][[:space:]]*//g' | sed 's/[[:space:]][[:space:]]*/:/g' |");
    my $ligne;
    my ($header, $totalMem, $usedMem, $freeMem);
    while($ligne = <INPUT>) {
        chop($ligne);
        ($header, $totalMem, $usedMem, $freeMem) = split(":", $ligne);
	}

    my $pourcentFreeMem = $freeMem  * 100 / $totalMem;
    &debug("TotalMem = $totalMem   UsedMem = $usedMem  FreeMem=$freeMem   \%FreeMem=$pourcentFreeMem");

#
# mem starvation ?
#
    if ($pourcentFreeMem > $FREEMEMLIMIT) {
        next;
    }

    &warn("TotalMem = $totalMem   UsedMem = $usedMem  FreeMem=$freeMem   \%FreeMem=$pourcentFreeMem");

#
# these are hash tables associating PID, %CPU and %MEM
#
    my %processes = ();

#
# parse all processes
#
    open (INPUT, "ps -o pid,user,%cpu,%mem,comm -U $userName | sed 's/^[[:space:]][[:space:]]*//g' | sed 's/[[:space:]][[:space:]]*/:/g' |");
    my $ligne;
    while ($ligne = <INPUT>) {

        chop($ligne);

        &debug("ps = $ligne");

        my ($pid, $user, $cpu, $mem, $command) = split(":", $ligne);
        $user=trim($user);
        $pid=trim($pid);
        $cpu=trim($cpu);
        $mem=trim($mem);
        $command=trim($command);

        if ($user =~ /USER/) {
            &debug("Bypassing header");
            next;
        }

        &debug("pid='$pid'   user = '$user'  cpu='$cpu'   mem='$mem'   command='$command'");
        
        if (!$processes{$pid}) {
            $processes{$pid} = {};
        }
        
        $processes{$pid}->{pid} = $pid;
        $processes{$pid}->{user} = $user;
        $processes{$pid}->{cpu} = $cpu;
        $processes{$pid}->{mem} = $mem;
        $processes{$pid}->{command} = $command;
        &debug("pid='$pid'   user = '$processes{$pid}->{user}'  cpu='$processes{$pid}->{cpu}'   mem='$processes{$pid}->{mem}'   command='$processes{$pid}->{command}'");
    }
    
    
#
# kill any starving process
#

    my $process;
    my $maxprocess;
    my $maxmem = 0;

    foreach $process (keys %processes) {

        if ($processes{$process}->{command} =~ /java/) {
            &info("We keep process  PID=".$process." (cpu=".$processes{$process}->{cpu}.", mem=".$processes{$process}->{mem}.", command=".$processes{$process}->{command}.")");
            next;
        }

        if($maxmem <= $processes{$process}->{mem}) {
            $maxmem = $processes{$process}->{mem};
            $maxprocess = $processes{$process};
        }

#				&info("".$processes{$process}->{command}."  -> mem = ".$processes{$process}->{mem}." USEDMEM = ".$USEDMEMLIMIT);

#				if ($processes{$process}->{mem} > $USEDMEMLIMIT) {
#						&warn("We kill process ".$process." (cpu=".$processes{$process}->{cpu}.", mem=".$processes{$process}->{mem}.", command=".$processes{$process}->{command}.")");
#						`kill -9 $process`;
#				}
		}

    &warn("Killing PID = ".$maxprocess->{pid}." ; CMD = ".$maxprocess->{command}." ; CPU = ".$maxprocess->{cpu}." ; MEM = ".$maxprocess->{mem});
		`kill -9 $maxprocess->{pid}`;
    
    sleep (1);
}

#
# End of file
#
