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
#  Shell script issuing REST 'send' commands to XtremWeb-HEP to register
#  objects with an UUID and a name.
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
#  Loop on XtremWeb-HEP types
#-----------------------------------------------------------------------------
for  XWHEP_TYPE  in  usergroup  user  group  app   host  session  \
                     groupwork  sessionwork  work  task  data
do

  echo;  echo  "#=====#  $XWHEP_TYPE  #=====#"
  
  #---------------------------------------------------------------------------
  #  REST command to XtremWeb-HEP with 1 command parameter :
  #  send?XmlDescription
  #---------------------------------------------------------------------------
  XWHEP_UUID="$(uuidgen -t)"
  XWHEP_DATA="XMLDESC=<xwhep> <$XWHEP_TYPE uid=$Q$XWHEP_UUID$Q name=${Q}Example of $XWHEP_TYPE${Q}/> </xwhep>"
  XWHEP_DATA_OPENID="$XWHEP_DATA&$OPENID_STRING"
  
  echo;  echo  "#-----#  curl  --data-binary $XWHEP_DATA  \
                $XWHEP_BASE_URL/send  #-----#"
  MESSAGE="$(curl  -s  -S  $CURL_CHECK_CERT_OPTION  \
             --data-binary "$XWHEP_DATA_OPENID"  "$XWHEP_BASE_URL/send")"
  # echo  "$MESSAGE"  |  perl  -wpe  's#(<[^/])#\n$1#g'  > /dev/stderr
  
  if    expr  "$MESSAGE"  :  '.*You *are *not *logged *in'  > /dev/null;  then
    echo;  echo  "$0:  XtremWeb-HEP has NOT recognized the 'OPENID_STRING'"  \
                 "at the end of following URL : '$XWHEP_URL_OPENID'"         \
                 > /dev/stderr
    exit 3
  elif  expr  "$MESSAGE"  :  '.*openid *delegation *error'  > /dev/null;  then
    echo;  echo  "$0:  You must export 'OPENID_STRING' with a renewed"  \
                 "value"  > /dev/stderr
    exit 4
  fi
  
  echo  "$MESSAGE"  |  \
    perl  -wpe  's#(password=")[^"]*#${1}*****#; s#"\s+#"\n  #g'
  
  #---------------------------------------------------------------------------
  #  If XtremWeb-HEP has given back a non null return code,
  #  do NOT try to display the object properties
  #---------------------------------------------------------------------------
  XWHEP_RETURNCODE="$(expr  "$MESSAGE"  :  '.* RETURNCODE="\([^"]*\)"')"
  # echo;  echo "XtremWeb-HEP Return Code = '$XWHEP_RETURNCODE'" > /dev/stderr
  if  [ "$XWHEP_RETURNCODE" ]  &&  \
      [ "$XWHEP_RETURNCODE" != "NULL" ]  &&  \
      [ "$XWHEP_RETURNCODE" != "ZERO" ];  then
    echo
    continue
  fi
  
  #---------------------------------------------------------------------------
  #  REST commands to XtremWeb-HEP with the object UUID :  get,  remove,  get
  #---------------------------------------------------------------------------
  for  XWHEP_COMMAND  in  get  remove  get
  do
    XWHEP_URL="$XWHEP_BASE_URL/$XWHEP_COMMAND/$XWHEP_UUID"
    echo;  echo "#-----#  curl  $XWHEP_URL   #-----#"
    MESSAGE="$(curl  -s  -S  $CURL_CHECK_CERT_OPTION  \
              "$XWHEP_URL?$OPENID_STRING")"
    echo "$MESSAGE"  |  perl  -wpe  's#"\s+#"\n  #g'
    #-------------------------------------------------------------------------
    #  If the answer to 'get' is empty, do NOT try to remove the object
    #-------------------------------------------------------------------------
    if  [ "$XWHEP_COMMAND" = "get" ]  &&  \
        expr  "$MESSAGE"  :  \
              '.*< *XMLVector  *SIZE *= *"0" *> *< */XMLVector *>'  \
              > /dev/null;  then
      break
    fi
  done
  
  echo

done