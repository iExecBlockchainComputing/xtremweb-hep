#!/bin/sh

# check that the server name has been defined
if [ -z $SCHEDULER_DOMAIN ] ; then
	echo "The domain name of the scheduler is missing"; exit 1
fi

# Modify etc hosts if an ip is given in parameters
if [ ! -z $SCHEDULER_IP ] ; then
	echo \"$SCHEDULER_IP $SCHEDULER_DOMAIN\" >> /etc/hosts
fi

# Populate the correct Login and Password for the client
if [ -z $LOGIN ] ; then
	echo " The login for the client is needed to be able to connect "; exit 1
else sed -i "s/^LOGIN=.*/LOGIN=$LOGIN/g" /iexec/conf/xtremweb.client.conf
fi

if [ -z $PASSWORD ] ; then
	echo " The password for the client is needed to be able to connect "; exit 1
else sed -i "s/^PASSWORD=.*/PASSWORD=$PASSWORD/g" /iexec/conf/xtremweb.client.conf
fi

# download the certificate from the server
echo "Downloading certificate from scheduler..."
echo -n | openssl s_client -connect $SCHEDULER_DOMAIN:443 | sed -ne "/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p" > /iexec/certificate/xwscheduler.crt
echo "Converting downloaded certificate to x509 DER..."
openssl x509 -outform der -in /iexec/certificate/xwscheduler.crt -out /iexec/certificate/xwscheduler.pem

sed -i "s/^DISPATCHERS=.*/DISPATCHERS=$SCHEDULER_DOMAIN/g" /iexec/conf/xtremweb.client.conf

# import the server certificate into the container keystore
keytool -import -alias localhost -file /iexec/certificate/xwscheduler.pem -trustcacerts -keystore /etc/ssl/certs/java/cacerts -storepass changeit -v -noprompt

# update the SSLKEYSTORE to point to the container's keystore
sed -i "s/^SSLKEYSTORE=.*/SSLKEYSTORE=\/etc\/ssl\/certs\/java\/cacerts/g" /iexec/conf/xtremweb.client.conf
sed -i "s/^SSLKEYPASSWORD=.*/SSLKEYPASSWORD=changeit/g" /iexec/conf/xtremweb.client.conf

cat /etc/hosts
