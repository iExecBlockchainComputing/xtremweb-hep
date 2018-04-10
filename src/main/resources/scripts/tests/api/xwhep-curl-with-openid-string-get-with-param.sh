#!/bin/sh
#=============================================================================
#
#  Copyright 2013  E. URBAH
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
#  Shell script issuing REST 'get' commands to XtremWeb-HEP with an openid
#  string and 1 command parameter.
#
#  It requires the 'OPENID_STRING' environment variable to be exported with
#  a value matching  '^$OPENID_STRING_PATTERN'  (see below).
#  On expiration, this value has to be renewed.
#
#=============================================================================
CURL_CHECK_CERT_OPTION='--insecure'

#-----------------------------------------------------------------------------
#  Variables which are stable
#-----------------------------------------------------------------------------
OPENID_STRING_PATTERN='AUTH_NONCE=.*&AUTH_EMAIL='
XWHEP_SERVER='xwservpub.lal.in2p3.fr'
XWHEP_PORT=4326
XWHEP_BASE_URL="https://$XWHEP_SERVER:$XWHEP_PORT"

#-----------------------------------------------------------------------------
#  Verify that 'curl' is installed
#-----------------------------------------------------------------------------
type  curl  > /dev/null  2>&1
if  [ $? -ne 0 ];  then
  echo  "$0:  This script requires the 'curl' package.  Please install it."  \
    > /dev/stderr
  exit  1
fi

#-----------------------------------------------------------------------------
#  Environment variable 'OPENID_STRING' must be exported, and set again after
#  each expiration
#-----------------------------------------------------------------------------
if  ! expr "$OPENID_STRING" : "$OPENID_STRING_PATTERN"  > /dev/null; then
  echo  "$0:  You must export the 'OPENID_STRING' environment variable"  \
        "with a value matching '^$OPENID_STRING_PATTERN'"  > /dev/stderr
  exit 2
fi

#-----------------------------------------------------------------------------
#  REST command to XtremWeb-HEP with 1 command parameter : getuserbylogin/oleg
#-----------------------------------------------------------------------------
XWHEP_URL="$XWHEP_BASE_URL/getuserbylogin/oleg"
XWHEP_URL_OPENID="$XWHEP_URL?$OPENID_STRING"

echo;  echo "#-----#  curl  $XWHEP_URL  #-----#"
MESSAGE="$(curl  -s  -S  $CURL_CHECK_CERT_OPTION  "$XWHEP_URL_OPENID")"
# echo "$MESSAGE"  |  perl -wpe 's#(<[^/])#\n$1#g'  > /dev/stderr

if    expr "$MESSAGE" : '.*You *are *not *logged *in'  > /dev/null; then
  echo;  echo  "$0:  XtremWeb-HEP has NOT recognized the 'OPENID_STRING'"  \
               "at the end of following URL : '$XWHEP_URL_OPENID'"         \
               > /dev/stderr
  exit 3
elif  expr "$MESSAGE" : '.*openid *delegation *error'  > /dev/null; then
  echo;  echo  "$0:  You must export 'OPENID_STRING' with a renewed"  \
               "value"  > /dev/stderr
  exit 4
fi

echo "$MESSAGE"  |  \
  perl  -wpe  's#(password=")[^"]*#${1}*****#; s#"\s+#"\n  #g'


if [ "$(uname | cut -c 1-6)" = "CYGWIN" ]; then
  exec  2> /dev/null
fi

#-----------------------------------------------------------------------------
#  Loop on REST 'get' commands to XtremWeb-HEP with 1 command parameter
#  administrator,  oleg,  ls,  pc-89155.lal.in2p3.fr,  task
#-----------------------------------------------------------------------------
for  XWHEP_UID  in  8d053af3-35fd-4e9e-a15b-08b6bb3be646  \
                    fc700ad4-6706-40e2-b4ea-3298ec1f49d0  \
                    da6ec799-1d7f-4093-9712-556c4b2d3b73  \
                    74291f6f-57cf-432b-aa1b-09995efc368e  \
                    5772940b-cab1-4785-93ba-4ea9ef9f6216
do
  XWHEP_URL="$XWHEP_BASE_URL/get/$XWHEP_UID"
  echo;  echo "#-----#  curl  $XWHEP_URL   #-----#"
  curl  -s  -S  $CURL_CHECK_CERT_OPTION  "$XWHEP_URL?$OPENID_STRING"  |  \
    perl  -wpe  's#"\s+#"\n  #g'
done
