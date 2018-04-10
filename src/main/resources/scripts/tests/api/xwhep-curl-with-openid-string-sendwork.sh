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
#  Shell script issuing REST commands to submit a work and retrieve the
#  results :  getapps,  get,  sendwork,  downloaddata,  remove
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
if  ! expr  "$OPENID_STRING"  :  "$OPENID_STRING_PATTERN"  > /dev/null;  then
  echo  "$0:  You must export the 'OPENID_STRING' environment variable"  \
        "with a value matching '^$OPENID_STRING_PATTERN'"  > /dev/stderr
  exit 2
fi

Q='"'


#-----------------------------------------------------------------------------
#  Retrieve from XtremWeb-HEP the UUIDs of available applications
#-----------------------------------------------------------------------------
XWHEP_URL="$XWHEP_BASE_URL/getapps"
echo;  echo  "#-----#  curl  $XWHEP_BASE_URL/getapps   #-----#"
MESSAGE="$(curl  -s  -S  $CURL_CHECK_CERT_OPTION  \
           "$XWHEP_BASE_URL/getapps?$OPENID_STRING")"
# echo  "$MESSAGE"  |  perl  -wpe  's#(<[^/])#\n$1#g'  > /dev/stderr

if    expr  "$MESSAGE"  :  '.*You *are *not *logged *in'  > /dev/null;  then
  echo;  echo  "$0:  XtremWeb-HEP has NOT recognized the 'OPENID_STRING'"  \
               "at the end of following URL : '$XWHEP_WORK_OPENID'"         \
               > /dev/stderr
  exit 3
elif  expr  "$MESSAGE"  :  '.*openid *delegation *error'  > /dev/null;  then
  echo;  echo  "$0:  You must export 'OPENID_STRING' with a renewed"  \
               "value"  > /dev/stderr
  exit 4
fi

XWHEP_APP_UUIDS="$(echo  "$MESSAGE"  |  grep  'value *="'  |  \
                   sed  -e  's|[^\-]*value *= *"\([^"]*\)|\1  |g;  s|".*$||')"

#-----------------------------------------------------------------------------
echo;  echo  Lookup the name of each application in order to find "'ls'"
#-----------------------------------------------------------------------------
XWHEP_LS_UUID=''

for  XWHEP_APP_UUID  in  $XWHEP_APP_UUIDS
do
#  echo;  echo  "#-----#  curl  $XWHEP_BASE_URL/get/$XWHEP_APP_UUID   #-----#"
  MESSAGE="$(curl  -s  -S  $CURL_CHECK_CERT_OPTION  \
             "$XWHEP_BASE_URL/get/$XWHEP_APP_UUID?$OPENID_STRING")"
  XWHEP_APP_NAME="$(echo "$MESSAGE"  |  grep ' name *="'  |  \
                    sed  -e  's|^.* name="\([^"]*\)".*$|\1|')"
  echo  -n  .  # "'$XWHEP_APP_NAME'"
  if  [ "$XWHEP_APP_NAME" = "ls" ];  then
    XWHEP_LS_UUID="$XWHEP_APP_UUID"
    echo
    break
  fi
done

if  [ -z "$XWHEP_LS_UUID" ];  then
  echo;  echo  "$0:  Application 'ls' NOT found in XtremWeb-HEP"  > /dev/stderr
  exit 5
fi

#-----------------------------------------------------------------------------
#  Submit to XtremWeb-HEP a work using an application
#-----------------------------------------------------------------------------
XWHEP_UUID="$(uuidgen -t)"
XWHEP_WORK="XMLDESC=<xwhep> <work uid=$Q$XWHEP_UUID$Q appuid=${Q}$XWHEP_LS_UUID${Q}/> </xwhep>"
XWHEP_WORK_OPENID="$XWHEP_WORK&$OPENID_STRING"

echo;  echo  "#-----#  curl  --data-binary $XWHEP_WORK  \
              $XWHEP_BASE_URL/sendwork  #-----#"
MESSAGE="$(curl  -s  -S  $CURL_CHECK_CERT_OPTION  \
           --data-binary "$XWHEP_WORK_OPENID"  "$XWHEP_BASE_URL/sendwork")"
# echo  "$MESSAGE"  |  perl  -wpe  's#(<[^/])#\n$1#g'  > /dev/stderr

if    expr  "$MESSAGE"  :  '.*You *are *not *logged *in'  > /dev/null;  then
  echo;  echo  "$0:  XtremWeb-HEP has NOT recognized the 'OPENID_STRING'"  \
               "at the end of following URL : '$XWHEP_WORK_OPENID'"        \
               > /dev/stderr
  exit 3
elif  expr  "$MESSAGE"  :  '.*openid *delegation *error'  > /dev/null;  then
  echo;  echo  "$0:  You must export 'OPENID_STRING' with a renewed"  \
               "value"  > /dev/stderr
  exit 4
fi

# echo  "$MESSAGE"  |  \
#   perl  -wpe  's#(password=")[^"]*#${1}*****#; s#"\s+#"\n  #g'

#-----------------------------------------------------------------------------
#  If XtremWeb-HEP has given back a non null return code, abort
#---------------------------------------------------------------------------==
XWHEP_RETURNCODE="$(expr  "$MESSAGE"  :  '.* RETURNCODE="\([^"]*\)"')"
# echo;  echo "XtremWeb-HEP Return Code = '$XWHEP_RETURNCODE'" > /dev/stderr
if  [ "$XWHEP_RETURNCODE" ]  &&  \
    [ "$XWHEP_RETURNCODE" != "NULL" ]  &&  \
    [ "$XWHEP_RETURNCODE" != "ZERO" ];  then
  echo  "$XWHEP_RETURNCODE"
  exit 6
fi


echo
#-----------------------------------------------------------------------------
echo "Looping on the status of the work.   You can interrupt the loop with"  \
     "(ctrl)C if you wish so."
#-----------------------------------------------------------------------------
WORK_STATUS=''

while  true
do
  
  #---------------------------------------------------------------------------
  #  Retrieve from XtremWeb-HEP the properties of the work
  #---------------------------------------------------------------------------
  XWHEP_URL="$XWHEP_BASE_URL/get/$XWHEP_UUID"
  # echo;  echo "#-----#  curl  $XWHEP_URL   #-----#"
  MESSAGE="$(curl  -s  -S  $CURL_CHECK_CERT_OPTION  \
            "$XWHEP_URL?$OPENID_STRING")"
  
  #---------------------------------------------------------------------------
  #  If the credentials are expired, ask for a new OPENID_STRING
  #---------------------------------------------------------------------------
  if  expr  "$MESSAGE"  :  '.*You *are *not *logged *in'  > /dev/null  ||
      expr  "$MESSAGE"  :  '.*openid *delegation *error'  > /dev/null;  then
    echo
    echo  "Your credentials have expired. "  \
          "Please enter new OPENID_STRING :"  > /dev/tty
    read  OPENID_STRING
    continue
  fi
  
  #---------------------------------------------------------------------------
  #  If the answer to 'get' is empty, abort
  #---------------------------------------------------------------------------
  if  expr  "$MESSAGE"  :  \
            '.*< *XMLVector  *SIZE *= *"0" *> *< */XMLVector *>'  > /dev/null
  then
    echo "$MESSAGE"  |  perl  -wpe  's#"\s+#"\n  #g'
    exit 6
  fi
  
  #---------------------------------------------------------------------------
  #  Extract the status of the job
  #---------------------------------------------------------------------------
  WORK_STATUS_NEW="$(expr  "$MESSAGE"  :  '.* status="\([^"]*\)"')"
  if  [ "$WORK_STATUS_NEW" = "$WORK_STATUS" ];  then
    echo  -n  .
  else
    WORK_STATUS="$WORK_STATUS_NEW"
    echo
    echo  -n  "Work $XWHEP_UUID :  $WORK_STATUS"
  fi
  
  if  [ "$WORK_STATUS" = "ERROR"     ]  ||  \
      [ "$WORK_STATUS" = "COMPLETED" ];  then
    break
  fi
  
  sleep 2
  
done

echo


#---------------------------------------------------------------------------
#  Extract the URI of the result, then download it and display its content
#---------------------------------------------------------------------------
XW_RESULT_UID="$(expr  "$MESSAGE"  :  \
                       '.* resulturi="xw://'"$XWHEP_SERVER"'/\([^"]*\)"')"
echo;  echo  "XW_RESULT_UID='$XW_RESULT_UID'"

if  [ "$XW_RESULT_UID" ];  then
  RESULT_FILENAME="Result-Of-$XWHEP_UUID"
  XWHEP_URL="$XWHEP_BASE_URL/downloaddata/$XW_RESULT_UID"
  echo;  echo "#-----#  curl  $XWHEP_URL   #-----#"
  MESSAGE="$(curl  -s  -S  $CURL_CHECK_CERT_OPTION  \
            "$XWHEP_URL?$OPENID_STRING"  >  "$RESULT_FILENAME")"
  echo "$MESSAGE"  |  perl  -wpe  's#"\s+#"\n  #g'
  
  RESULT_CONTENT_TYPE="$(file  "$RESULT_FILENAME")"
  if  expr  "$RESULT_CONTENT_TYPE"  :  '.* [Tt]ext *$'  > /dev/null;  then
    cat   "$RESULT_FILENAME"
    rm    "$RESULT_FILENAME"
  else
    echo  "$RESULT_CONTENT_TYPE"
  fi
fi

#---------------------------------------------------------------------------
#  REST commands to XtremWeb-HEP with the object UUID :  remove,  get
#---------------------------------------------------------------------------
for  XWHEP_COMMAND  in  remove  get
do
  XWHEP_URL="$XWHEP_BASE_URL/$XWHEP_COMMAND/$XWHEP_UUID"
  echo;  echo "#-----#  curl  $XWHEP_URL   #-----#"
  MESSAGE="$(curl  -s  -S  $CURL_CHECK_CERT_OPTION  \
            "$XWHEP_URL?$OPENID_STRING")"
  echo "$MESSAGE"  |  perl  -wpe  's#"\s+#"\n  #g'
done

echo
