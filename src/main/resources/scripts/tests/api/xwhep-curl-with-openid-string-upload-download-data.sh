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
#  Shell script issuing REST 'send', 'get', 'uploaddata', 'downloaddata' and
#  'remove' commands to XtremWeb-HEP.
#
#  It requires the 'OPENID_STRING' environment variable to be exported with a
#  value matching  '^$OPENID_STRING_PATTERN'  (see below).
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


#---------------------------------------------------------------------------
#  Register the data inside XtremWeb-HEP using a REST command with
#  1 parameter :  send?XMLDESC=...
#---------------------------------------------------------------------------
Q='"'
XWHEP_TYPE='data'
XWHEP_UUID="$(uuidgen -t)"
DATAFILE="$(basename "$0")"

XWHEP_DATA="XMLDESC=<xwhep> <$XWHEP_TYPE uid=$Q$XWHEP_UUID$Q name=${Q}Example of $XWHEP_TYPE: $DATAFILE${Q}/> </xwhep>"
XWHEP_DATA_OPENID="$XWHEP_DATA&$OPENID_STRING"

echo;  echo "#-----#  curl  --data-binary $XWHEP_DATA  \
	     $XWHEP_BASE_URL/send  #-----#"
MESSAGE="$(curl  -s  -S  $CURL_CHECK_CERT_OPTION  \
	   --data-binary "$XWHEP_DATA_OPENID"  "$XWHEP_BASE_URL/send")"
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

echo "$MESSAGE"  |  perl  -wpe  's#"\s+#"\n  #g'


#---------------------------------------------------------------------------
#  Request XtremWeb-HEP to display the data properties,
#  using a REST command with 1 parameter :  get/$XWHEP_UUID
#---------------------------------------------------------------------------
XWHEP_URL="$XWHEP_BASE_URL/get/$XWHEP_UUID"
echo;  echo "#-----#  curl  $XWHEP_URL   #-----#"
curl  -s  -S  $CURL_CHECK_CERT_OPTION  "$XWHEP_URL?$OPENID_STRING"  |  \
  perl  -wpe  's#"\s+#"\n  #g'


#---------------------------------------------------------------------------
#  REST command to XtremWeb-HEP with parameters :
#  uploaddata/UUID?DATASIZE=...&DATASHASUM=...   --post-data=DATAFILE=...
#  Upload is NOT handled by 'wget', we really have to use 'curl'.
#---------------------------------------------------------------------------
DATAFILE="$0"
DATASIZE=$(stat -c '%s' "$DATAFILE")
DATAMD5SUM=$(md5sum "$DATAFILE"  |   cut -d ' ' -f 1)
XWHEP_URL="$XWHEP_BASE_URL/uploaddata/$XWHEP_UUID?DATASIZE=$DATASIZE&DATASHASUM=$DATAMD5SUM"
XWHEP_URL_OPENID="$XWHEP_URL&$OPENID_STRING"

CURL_UPLOAD_PARAM="DATAFILE=@$DATAFILE"
echo;  echo "#-----#  curl -F $CURL_UPLOAD_PARAM  $XWHEP_URL  #-----#"
MESSAGE="$(curl  -s  -S  $CURL_CHECK_CERT_OPTION  -F "$CURL_UPLOAD_PARAM"  "$XWHEP_URL_OPENID")"
# echo "$MESSAGE"  |  perl -wpe 's#(<[^/])#\n$1#g'  > /dev/stderr

if    expr "$MESSAGE" : '.*You *are *not *logged *in'  > /dev/null; then
  echo;  echo  "$0:  XtremWeb-HEP has NOT recognized the 'OPENID_STRING'"  \
               "at the end of following URL : '$XWHEP_URL_OPENID'"         \
               > /dev/stderr
  exit 2
elif  expr "$MESSAGE" : '.*openid *delegation *error'  > /dev/null; then
  echo;  echo  "$0:  You must export 'OPENID_STRING' with a renewed"  \
               "value"  > /dev/stderr
  exit 3
fi

echo "$MESSAGE"  |  perl  -wpe  's#"\s+#"\n  #g'


#---------------------------------------------------------------------------
#  Request XtremWeb-HEP to display the data properties,
#  using a REST command with 1 parameter :  get/$XWHEP_UUID
#---------------------------------------------------------------------------
XWHEP_URL="$XWHEP_BASE_URL/get/$XWHEP_UUID"
echo;  echo "#-----#  curl  $XWHEP_URL   #-----#"
curl  -s  -S  $CURL_CHECK_CERT_OPTION  "$XWHEP_URL?$OPENID_STRING"  |  \
  perl  -wpe  's#"\s+#"\n  #g'


#---------------------------------------------------------------------------
#  Request XtremWeb-HEP to send the data content,
#  using a REST command with 1 parameter :  downloaddata/$XWHEP_UUID
#---------------------------------------------------------------------------
XWHEP_URL="$XWHEP_BASE_URL/downloaddata/$XWHEP_UUID"
echo;  echo "#-----#  curl  $XWHEP_URL  |  wc   #-----#"
curl  -s  -S  $CURL_CHECK_CERT_OPTION  "$XWHEP_URL?$OPENID_STRING"  |  wc


#-----------------------------------------------------------------------------
#  REST commands to XtremWeb-HEP with the object UUID :  remove,  get
#-----------------------------------------------------------------------------
for  XWHEP_COMMAND  in  remove  get
do
  XWHEP_URL="$XWHEP_BASE_URL/$XWHEP_COMMAND/$XWHEP_UUID"
  echo;  echo "#-----#  curl  $XWHEP_URL   #-----#"
  curl  -s  -S  $CURL_CHECK_CERT_OPTION  "$XWHEP_URL?$OPENID_STRING"  |  \
    perl  -wpe  's#"\s+#"\n  #g'
done
