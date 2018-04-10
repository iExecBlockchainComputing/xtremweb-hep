#!/bin/sh
#=============================================================================
#
#  Copyright 2015  E. URBAH
#                  at LAL, Univ Paris-Sud, IN2P3/CNRS, Orsay, France
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
#  Shell script showing an example of usage of openSSL to :
#
#  -  Create a CA   private key and a CA   self signed certificate (PEM)
#
#  -  Create a User private key and a User self signed certificate (PEM)
#
#  -  Sign the User cert with the CA cert (PEM)
#
#  -  Bring together the User key (PEM) and the User cert (PEM) into a User
#     P12 bundle which can be imported inside a web browser
#
#
#  Requirements :
#
#  -  For security, the execution folder must have EMPTY Group and Other perms
#
#  -  The execution folder must contain 3 files with following content :
#     .ca :  Password protecting the CA   private key
#     .pk :  Password protecting the User private key
#     .p12 : Password protecting the User P12 bundle
#
#=============================================================================

#-----------------------------------------------------------------------------
#  Constants which may be freely modified :
#  Number of days the certificates will be valid after creation for :
#  - the CA
#  - the User
#-----------------------------------------------------------------------------
CA_DAYS=30
USER_DAYS=30

#-----------------------------------------------------------------------------
#  Constants which may be modified with caution :
#  Number of bits for the private keys of :
#  - the CA
#  - the User
#-----------------------------------------------------------------------------
CA_NUMBER_OF_BITS=2048
USER_NUMBER_OF_BITS=2048

#-----------------------------------------------------------------------------
#  Verify that the execution folder has EMPTY Group and Other perms
#-----------------------------------------------------------------------------
PERMS="$(ls -dog .  |  cut -c 5-10)"
if  [ "$PERMS" != "------" ]; then
  echo "$0 :  Execution folder has '$PERMS' Group and Other perms. "  \
       "They must be EMPTY."  > /dev/stderr
  exit 1
fi

#-----------------------------------------------------------------------------
#  Verify that the required password files do exist and are NOT empty
#-----------------------------------------------------------------------------
RC=0
for  FILE  in  .ca  .pk  .p12; do
  if  [ ! -s "$FILE" ]; then
    echo "$0 :  Missing or empty file '$FILE'"  > /dev/stderr
    RC=2
  fi
done
[ $RC -eq 0 ]  ||  exit $RC

USER_UPPER="$(echo $USER    |  tr '[a-z]' '[A-Z'])"
DOMAIN="$(hostname -f       |  sed -e 's=^[^.]*\.==')"

set -xe
umask 077

#-----------------------------------------------------------------------------
#  openSSL configuration file for the CA
#-----------------------------------------------------------------------------
cat  > $USER-ca.cnf  << END_OF_CA_CONF
[ req ]
prompt                 = no
default_bits           = $CA_NUMBER_OF_BITS
distinguished_name     = req_distinguished_name
x509_extensions        = v3_ca
dirstring_type         = nobmp

[ req_distinguished_name ]
countryName            = FR
localityName           = Orsay
organizationName       = $USER_UPPER
organizationalUnitName = $USER_UPPER Test CA
commonName             = $USER_UPPER Test CA

[ v3_ca ]
subjectKeyIdentifier   = hash
authorityKeyIdentifier = keyid:always,issuer:always
basicConstraints       = critical, CA:true, pathlen:0
keyUsage               = critical, keyCertSign
END_OF_CA_CONF

#-----------------------------------------------------------------------------
#  openSSL configuration file for the USER
#-----------------------------------------------------------------------------
cat  > $USER.cnf  << END_OF_USER_CONF
[ req ]
prompt                 = no
default_bits           = $USER_NUMBER_OF_BITS
distinguished_name     = req_distinguished_name
x509_extensions        = v3_user
dirstring_type         = nobmp

[ req_distinguished_name ]
countryName            = FR
localityName           = Orsay
organizationName       = $USER_UPPER
commonName             = $USER_UPPER Test
emailAddress           = $USER@$DOMAIN

[ v3_user ]
subjectKeyIdentifier   = hash
authorityKeyIdentifier = keyid:always,issuer:always
basicConstraints       = critical, CA:false
extendedKeyUsage       = critical, clientAuth
subjectAltName         = email:$USER@$DOMAIN
END_OF_USER_CONF

#-----------------------------------------------------------------------------
#  Purge files to be created
#-----------------------------------------------------------------------------
rm -f  $USER-ca-key.pem  $USER-ca-cert.pem  $USER-key.pem  \
       $USER-cert-self-signed.pem  $USER-cert.pem  $USER-test.p12

#-----------------------------------------------------------------------------
#  Create a CA private key and a CA self signed certificate (PEM)
#-----------------------------------------------------------------------------
openssl  genrsa  -passout file:.ca  -out $USER-ca-key.pem  $CA_NUMBER_OF_BITS

openssl  req  -config $USER-ca.cnf  -x509  -new  -passin file:.ca  \
              -key $USER-ca-key.pem  -out $USER-ca-cert.pem  -days $CA_DAYS

openssl  x509  -noout  -text  -certopt no_pubkey,no_sigdump  \
               -in $USER-ca-cert.pem

#-----------------------------------------------------------------------------
#  Create a User private key and a User self signed certificate (PEM)
#-----------------------------------------------------------------------------
openssl  genrsa  -passout file:.pk  -out $USER-key.pem  $USER_NUMBER_OF_BITS

openssl  req  -config $USER.cnf  -x509  -new  -passin file:.pk     \
              -key $USER-key.pem  -out $USER-cert-self-signed.pem  \
              -days $USER_DAYS

openssl  x509  -noout  -text  -certopt no_pubkey,no_sigdump  \
               -in $USER-cert-self-signed.pem

#-----------------------------------------------------------------------------
#  Sign the User cert with the CA cert (PEM)
#-----------------------------------------------------------------------------
openssl  x509  -CAkeyform PEM  -CAkey $USER-ca-key.pem  -CAform  PEM  \
               -CA $USER-ca-cert.pem  -CAcreateserial   -inform  PEM  \
               -in $USER-cert-self-signed.pem           -outform PEM  \
               -out $USER-cert.pem  -days $USER_DAYS

openssl  x509  -noout  -text  -certopt no_pubkey,no_sigdump  \
               -in $USER-cert.pem

#-----------------------------------------------------------------------------
#  Bring together the User key (PEM) and the User cert (PEM) into a User P12
#  bundle which can be imported inside a web browser
#-----------------------------------------------------------------------------
openssl  pkcs12  -export  -passin file:.pk  -inkey $USER-key.pem  \
                 -in $USER-cert.pem  -certfile $USER-ca-cert.pem  \
                 -name "$USER_UPPER Test"  -passout file:.p12     \
                 -out $USER-test.p12

openssl  pkcs12  -info  -nokeys  -passin file:.p12  -in $USER-test.p12

