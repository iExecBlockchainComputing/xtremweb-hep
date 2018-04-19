#!/bin/sh

# check that the server name has been defined
if [ -z $SCHEDULER_DOMAIN ] ; then
	echo "The domain name of the scheduler is missing"; exit 1
fi

# Modify etc hosts if an ip is given in parameters
if [ ! -z $SCHEDULER_IP ] ; then
	echo "$SCHEDULER_IP $SCHEDULER_DOMAIN" >> /etc/hosts
fi

# download the certificate from the server
echo "Downloading certificate from scheduler..."
echo -n | openssl s_client -connect $SCHEDULER_DOMAIN:443 | sed -ne "/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p" > /iexec/certificate/xwscheduler.crt
if [ -s /iexec/certificate/xwscheduler.crt ]
then
	echo "Certificate has been downloaded"
else
	echo "Certificate couldn't be downloaded, stopping now"
	exit 0
fi

echo "Converting downloaded certificate to x509 DER..."
openssl x509 -outform der -in /iexec/certificate/xwscheduler.crt -out /iexec/certificate/xwscheduler.pem
sed -i "s/^DISPATCHERS=.*/DISPATCHERS=$SCHEDULER_DOMAIN/g" /iexec/conf/xtremweb.worker.conf

# import the server certificate into the container keystore
keytool -import -alias localhost -file /iexec/certificate/xwscheduler.pem -trustcacerts -keystore /etc/ssl/certs/java/cacerts -storepass changeit -v -noprompt

# update the SSLKEYSTORE to point to the container's keystore
sed -i "s/^SSLKEYSTORE=.*/SSLKEYSTORE=\/etc\/ssl\/certs\/java\/cacerts/g" /iexec/conf/xtremweb.worker.conf
sed -i "s/^SSLKEYPASSWORD=.*/SSLKEYPASSWORD=changeit/g" /iexec/conf/xtremweb.worker.conf

# add the TMPDIR variable if defined
if [ ! -z $TMPDIR ] ; then
	sed -i "s/^#TMPDIR=.*/TMPDIR=$TMPDIR/g" /iexec/conf/xtremweb.worker.conf
fi

# Add defined SHAREDAPPS variable if defined, otherwise use a default value
if [ ! -z $SHAREDAPPS ] ; then
	sed -i "s/^#SHAREDAPPS=.*/SHAREDAPPS=$SHAREDAPPS/g" /iexec/conf/xtremweb.worker.conf
else sed -i "s/^#SHAREDAPPS=.*/SHAREDAPPS=docker/g" /iexec/conf/xtremweb.worker.conf
fi

# Add SHAREDPACKAGES only if it is given in parameters
if [ ! -z $SHAREDPACKAGES ] ; then
	sed -i "s/^#SHAREDPACKAGES=.*/SHAREDPACKAGES=$SHAREDPACKAGES/g" /iexec/conf/xtremweb.worker.conf
fi

# Set default value for Login
if [ ! -z $LOGIN ] ; then
	sed -i "s/^LOGIN=.*/LOGIN=$LOGIN/g" /iexec/conf/xtremweb.worker.conf
else sed -i "s/^LOGIN=.*/LOGIN=vworker/g" /iexec/conf/xtremweb.worker.conf
fi

# Set default value for Password
if [ ! -z $PASSWORD ] ; then
	sed -i "s/^PASSWORD=.*/PASSWORD=$PASSWORD/g" /iexec/conf/xtremweb.worker.conf
else sed -i "s/^PASSWORD=.*/PASSWORD=vworkerp/g" /iexec/conf/xtremweb.worker.conf
fi

# Set the LoggerLevel
if [ ! -z $LOGGERLEVEL ] ; then
	sed -i "s/^LOGGERLEVEL=.*/LOGGERLEVEL=$LOGGERLEVEL/g" /iexec/conf/xtremweb.worker.conf
fi

# Set the flag for sandboxing
if [ ! -z $SANDBOXENABLED ] ; then
	sed -i "s/^SANDBOXENABLED=.*/SANDBOXENABLED=$SANDBOXENABLED/g" /iexec/conf/xtremweb.worker.conf
fi

# Change a flag in the docker start script to still be able to debug
sed -i "s/TESTINGONLY=.*/TESTINGONLY=FALSE/g" /iexec/bin/xwstartdocker.sh

# Add the bin folder to the path
echo "export PATH=/iexec/bin:\$PATH" >> /root/.bashrc
source .bashrc

cat /etc/hosts

iexecWorkerYmlFile=/iexec/conf/iexec-worker.yml

sed -i "s/path:.*/path: /iexec/wallet/wallet.json/g"    iexecWorkerYmlFile
sed -i "s/password:.*/password: \"$WALLETPASSWORD\"/g"  iexecWorkerYmlFile


# Start the worker
/iexec/bin/xtremweb.worker console
