#!/bin/sh

#
# -1- don't renice in container
# -2- we must remove LAUNCHERURL since Apache is not installed
#
sed -i "s/^V_NICE=.*//g" /iexec/bin/xtremwebconf.sh
sed -i "s/LAUNCHER.*//g" /iexec/conf/xtremweb.server.conf
sed -i "s/SSLKEYSTORE.*/SSLKEYSTORE=\/iexec\/keystore\/xwhepserver.keys/g" /iexec/conf/xtremweb.server.conf


# This will sed the value val given in parameter in the config file and set the value
# to $val if it is defined
replace_predefined_var_in_conf () {
   varName=$1;
   eval varValue=\$$varName
   if [ ! -z $varValue ] ; then
     sed -i "s/^$varName=.*/$varName=$varValue/g" /iexec/conf/xtremweb.server.conf
   fi
}

replace_predefined_var_in_conf DBHOST
replace_predefined_var_in_conf DBNAME
replace_predefined_var_in_conf DBUSER
replace_predefined_var_in_conf DBPASS
replace_predefined_var_in_conf ADMINLOGIN
replace_predefined_var_in_conf ADMINPASSWORD
replace_predefined_var_in_conf ADMINUID
replace_predefined_var_in_conf WORKERLOGIN
replace_predefined_var_in_conf WORKERPASSWORD
replace_predefined_var_in_conf WORKERUID
replace_predefined_var_in_conf LOGGERLEVEL
replace_predefined_var_in_conf BLOCKCHAINETHENABLED


iexecSchedulerYmlFile=/iexec/conf/iexec-scheduler.yml

sed -i "s/path:.*/path: \/iexec\/wallet\/wallet_scheduler.json/g"   $iexecSchedulerYmlFile
sed -i "s/password:.*/password: \"$WALLETPASSWORD\"/g"              $iexecSchedulerYmlFile
sed -i "s/clientAddress:.*/clientAddress: $ETHNODE/g"               $iexecSchedulerYmlFile
sed -i "s/rlcAddress:.*/rlcAddress: $RLCCONTRACT/g"                 $iexecSchedulerYmlFile
sed -i "s/iexecHubAddress:.*/iexecHubAddress: $IEXECHUBCONTRACT/g"  $iexecSchedulerYmlFile
sed -i "s/address:.*/address: $WORKERPOOLADDRESS/g"  				$iexecSchedulerYmlFile

replace_commented_var_in_conf () {
  varName=$1;
  eval varValue=\$$varName
  if [ ! -z $varValue ] ; then
    sed -i "s/^#$varName=.*/$varName=$varValue/g" /iexec/conf/xtremweb.server.conf
  fi
}

replace_commented_var_in_conf JWTETHISSUER
replace_commented_var_in_conf JWTETHSECRET
replace_commented_var_in_conf DELEGATEDREGISTRATION


# keystore is generated from the script xwhepgenkeys directly in the container
rm /iexec/keystore/cacerts
rm /iexec/keystore/*.keys
rm /iexec/keystore/*.p12
/iexec/bin/xwhepgenkeys
if [ $? -eq 0 ] ; then echo "Keystores generated" ; else echo "ERROR: keystores generation error"; exit 1 ;
fi


if [ ! -z $ALIVEPERIOD ] ; 
  then
    /iexec/bin/xtremweb.server console -DALIVEPERIOD=$ALIVEPERIOD
  else
    /iexec/bin/xtremweb.server console
fi


