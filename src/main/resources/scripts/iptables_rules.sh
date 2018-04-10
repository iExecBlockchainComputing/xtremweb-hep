#!/bin/sh
#
# Copyrights     : CNRS
# Author         : Simon Delamare
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

# Input
echo Input
/sbin/iptables -P INPUT ACCEPT
# Output
echo Output
/sbin/iptables -P OUTPUT ACCEPT
# Routage
echo Routage
/sbin/iptables -P FORWARD ACCEPT

# Communication allowed on loopback
echo Loopback
/sbin/iptables -A OUTPUT -o lo -j ACCEPT

# Output allowed on established incoming connections 
echo established 
/sbin/iptables -A OUTPUT -m state --state RELATED,ESTABLISHED -j ACCEPT 

# Allow a given port range on output 
echo ports
/sbin/iptables -A OUTPUT -p tcp --dport 4000:4100 -j ACCEPT 

# Deny access to LAN
echo 10
/sbin/iptables -A OUTPUT --destination 10.0.0.0/8 -j REJECT 
echo 172
/sbin/iptables -A OUTPUT --destination 172.16.0.0/12 -j REJECT
echo 192
/sbin/iptables -A OUTPUT --destination 192.168.0.0/24 -j REJECT
echo "done"
