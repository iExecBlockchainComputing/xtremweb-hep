#!/bin/sh
#=============================================================================
#
#  Copyright (C) 2012 E. URBAH
#                     at LAL, Univ Paris-Sud, IN2P3/CNRS, Orsay, France
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
#  For a Live CD registered as an XtremWeb-HEP application, this shell script
#  submits to XtremWeb-HEP a job running this Live CD with a context disk and
#  an user application disk.
#
#  By default, the user application disk is the 'defaultdirinuri' of the
#  Live CD.
#
#  Syntax:  xw-submit-live-cd.sh  [-v | --verbose]
#                                 --livecd <Live CD>
#                                 --context <Context disk>
#                                 [--userapp <User application disk>]
#                                 [--xwconfig <Client configuration file for
#                                              XtremWeb-HEP>]
#
#=============================================================================


#=============================================================================
#
#  GLOBAL VARIABLES
#
#=============================================================================
if [ -z "${OSTYPE##linux*}" ]; then
  DATE_FORMAT='--rfc-3339=seconds'
else
  DATE_FORMAT='+%Y-%m-%d %H:%M:%S%z'
fi

DISK_EXTENSIONS_UC="'ISO', 'VDI', 'VHD', 'VMDK'"
DISK_EXTENSIONS_LC=$(echo "$DISK_EXTENSIONS_UC"  |  tr  'A-Z'  'a-z')
DISK_EXTENSIONS="$DISK_EXTENSIONS_UC, $DISK_EXTENSIONS_LC"


#-----------------------------------------------------------------------------
#  GLOBAL VARIABLES containing parameters provided to this script
#-----------------------------------------------------------------------------
LIVE_CD=''
CONTEXT_DISK=''
USER_APP_DISK=''
CFG=''
VERBOSE=''

#-----------------------------------------------------------------------------
#  GLOBAL VARIABLES set as return values by functions
#-----------------------------------------------------------------------------
XW_MESSAGE=''                 #  Set by function  xw_action_success_or_fatal
XW_ATTRIBUTE_VALUE=''         #  Set by function  extract_xw_attribute_value
DATA_NAME=''                  #  Set by function  verify_data_get_name_and_uri
DATA_URI=''                   #  Set by functions verify_data_get_name_and_uri
                              #               and xw_send_file


#=============================================================================
#  Function  debug_message (Message part, ...)
#=============================================================================
debug_message ()
{
  echo "$(date "$DATE_FORMAT")  $0  DEBUG: " "$@"  > /dev/stderr
}


#=============================================================================
#  Function  info_message (Message part, ...)
#=============================================================================
info_message ()
{
  echo "$(date "$DATE_FORMAT")  $0  INFO: " "$@"
}


#=============================================================================
#  Function  fatal (Message part, ...)
#=============================================================================
fatal ()
{
  RC=$?
  if [ $RC -eq 0 ]; then RC=1; fi
  
  echo "$(date "$DATE_FORMAT")  $0  FATAL:  ${*:-Ctrl+C}"  > /dev/stderr
  exit $RC
}


#=============================================================================
#
#  MAIN
#
#=============================================================================

#-----------------------------------------------------------------------------
#  Process parameters
#-----------------------------------------------------------------------------
while [ $# -gt 0 ]
do

  case "$1" in
    
    --livecd | --liveCD | --liveCDuri | --livecduri | --hda | --hdauri )
      shift
      LIVE_CD="$1"
      ;;
  
    --context )
      shift
      CONTEXT_DISK="$1"
      ;;
  
    --userapp | --userappli | --userapplication )
      shift
      USER_APP_DISK="$1"
      ;;
    
    --xwconfig )
      shift
      CFG="--xwconfig $1"
      unset X509_USER_PROXY
      ;;
    
    -v | --verbose | --debug )
      VERBOSE=1
      ;;
    
    -h | --help | '-?' )
      echo "Usage:  $0  [-v | --verbose]  --livecd <Live CD> "  \
                        "--context <Context disk> "  \
                        "[--userapp <User application disk>] "  \
                        "[--xwconfig <Client configuration file for"  \
                                     "XtremWeb-HEP>]"
      echo "By default, the user application disk is the 'defaultdirinuri'"  \
           "of the Live CD"
      exit 0
      ;;
    
  esac
  
  shift
  
done

#-----------------------------------------------------------------------------
#  Verify that mandatory parameters are really provided
#-----------------------------------------------------------------------------
[ "$LIVE_CD" ]  ||  fatal  "For the Live CD, you must provide its"  \
                           "XtremWeb-HEP name or uri (please try --help)"

[ "$CONTEXT_DISK" ]  ||  fatal  "For the context disk, you must provide"  \
                        "its path or its XtremWeb-HEP uri (please try --help)"

#-----------------------------------------------------------------------------
#  XtremWeb-HEP commands are searched in the standard install location
#-----------------------------------------------------------------------------
XW_PROG="xtremweb.client"
XW_BIN=$(ls -d /opt/xwhep-client-*  |  tail -1)/bin
[ -f "$XW_BIN/$XW_PROG" ]  ||  fatal  "'$XW_PROG' NOT found"

XW_APPS="$XW_BIN/xwapps"
XW_DATAS="$XW_BIN/xwdatas"
XW_GROUPS="$XW_BIN/xwgroups"
XW_SUBMIT="$XW_BIN/xwsubmit"
XW_STATUS="$XW_BIN/xwstatus"
XW_RESULTS="$XW_BIN/xwresults"
XW_SEND_DATA="$XW_BIN/xwsenddata"


#=============================================================================
#
#  Function  xw_action_success_or_fatal ()
#
#  Parameters :    XtremWeb-HEP action
#                  XtremWeb-HEP params ...
#
#  Variable set :  XW_MESSAGE :   Output of XtremWeb-HEP
#
#=============================================================================
xw_action_success_or_fatal ()
{
  XW_MESSAGE=$( [ -z "$VERBOSE" ]  ||  set -x;  "$@" )  ||  \
    fatal  "$XW_MESSAGE"
}


#=============================================================================
#
#  Function  extract_xw_attribute_value ()
#
#  From the XML output of an XtremWeb-HEP command, extract an attribute value
#
#  Parameters :
#      XW_DESCRIPTION :      Description of the XtremWeb-HEP object
#      XW_ATTRIBUTE_NAME :   Name of the XtremWeb-HEP attribute to extract
#      XW_XML :              XML output of an XtremWeb-HEP command
#
#  Variable set :
#      XW_ATTRIBUTE_VALUE :  Extracted value of the XtremWeb-HEP attribute
#
#=============================================================================
extract_xw_attribute_value ()
{
  XW_DESCRIPTION="$1"
  XW_ATTRIBUTE_NAME="$2"
  XW_XML="$3"
  XW_ATTRIBUTE_VALUE=$(perl -we  \
    'if ( $ARGV[0] =~ m/\s'"$XW_ATTRIBUTE_NAME"'\s*=\s*"([^"]+)"/i )
        {print $1}'  "$XW_XML")
  [ "$XW_ATTRIBUTE_VALUE" ]  ||  \
    fatal  "Can NOT extract '$XW_ATTRIBUTE_NAME' of $XW_DESCRIPTION from"  \
           "$(echo  "'$XW_XML'"  |  perl  -wpe 's/"\s+/"\n  /g')"
}


#=============================================================================
#
#  Function  verify_data_get_name_and_uri ()
#
#  Verify the reference of a data, get its name and try to get its URI.
#
#  Parameters :
#      DATA_DESCRIPTION :       Data description
#      DATA_REF :               Local file path or http(s):// or xw:// or UUID
#      ACCEPTABLE_EXTENSIONS :  Optional list of acceptable extensions
#
#  Variables set :
#      DATA_NAME :              Real data name
#      DATA_URI :               Data URI
#
#=============================================================================
verify_data_get_name_and_uri ()
{
  DATA_DESCRIPTION="$1"
  DATA_REF="$2"
  ACCEPTABLE_EXTENSIONS="$3"
  
  DATA_NAME=''
  DATA_URI=''
  
  D1='[0-9A-Fa-f]'
  D4="$D1$D1$D1$D1"
  UUID_PATTERN="$D4$D4-$D4-$D4-$D4-$D4$D4$D4"

  #---------------------------------------------------------------------------
  #  If the data reference begins with 'http://' or 'https://', extract its
  #  name
  #---------------------------------------------------------------------------
  if [ '(' -z "${DATA_REF##http://*}"  ')' -o  \
       '(' -z "${DATA_REF##https://*}" ')' ]; then
    
    DATA_NAME="${DATA_REF%%[/?]download}"
    DATA_NAME="${DATA_NAME##*/}"
    DATA_NAME="${DATA_NAME##*&filename=}"
    
  #---------------------------------------------------------------------------
  #  If the data reference begins with 'xw://' or looks like an UUID, verify
  #  in the XtremWeb-HEP server that it exists, has a name and has an uid
  #---------------------------------------------------------------------------
  elif [ -z "${DATA_REF##xw://*}" ]  ||  \
       expr "$DATA_REF" : "$UUID_PATTERN\$" > /dev/null; then
    
    xw_action_success_or_fatal  "$XW_DATAS"  $CFG  "$DATA_REF"  --xwformat xml
    
    OBJECT_TYPE=$( perl -we 'if ( $ARGV[0] =~ m/<([^\s]+)\s+uid=/i )
                             {print lc($1)}'  "$XW_MESSAGE" )  ||  \
      fatal  "Can NOT retrieve type of '$DATA_REF' from '$XW_MESSAGE'"
    
    [ "$OBJECT_TYPE" = "data" ]  ||  \
      fatal  "XtremWeb-HEP object '$DATA_REF' is of type '$OBJECT_TYPE'"  \
             "instead of 'data'"
    
    extract_xw_attribute_value  "data '$DATA_REF'"  'name'  "$XW_MESSAGE"
    DATA_NAME="$XW_ATTRIBUTE_VALUE"
    
    extract_xw_attribute_value  "data '$DATA_REF'"  'uri'   "$XW_MESSAGE"
    DATA_URI="$XW_ATTRIBUTE_VALUE"

  #---------------------------------------------------------------------------
  #  Otherwise, the data reference is the name of a local file.
  #  It has NO uri yet.
  #---------------------------------------------------------------------------
  else
    [ -r "$DATA_REF" ]  ||  \
      fatal  "Can NOT read $DATA_DESCRIPTION '$DATA_REF'"
    DATA_NAME="${DATA_REF##*/}"
  fi
  
  #---------------------------------------------------------------------------
  #  If acceptable extensions are given, verify that DATA_NAME ends with one
  #---------------------------------------------------------------------------
  if [ "$ACCEPTABLE_EXTENSIONS" ]; then
    DATA_EXTENSION="${DATA_NAME%.[Zz][Ii][Pp]}"
    DATA_EXTENSION="${DATA_EXTENSION##*.}"
    if [ -z "$DATA_EXTENSION" ]  ||  \
       ! expr  "$ACCEPTABLE_EXTENSIONS" : ".*'$DATA_EXTENSION'"  > /dev/null
    then
      fatal  "$DATA_DESCRIPTION '$DATA_NAME' does NOT end with one of"  \
             "$ACCEPTABLE_EXTENSIONS"
    fi
  fi

  #---------------------------------------------------------------------------
  #  If the data reference begins with 'http://' or 'https://', verify that
  #  it can be downloaded
  #---------------------------------------------------------------------------
  if [ '(' -z "${DATA_REF##http://*}"  ')' -o  \
       '(' -z "${DATA_REF##https://*}" ')' ]; then
    
    ( [ -z "$VERBOSE" ]  ||  set -x
      wget  -nv  --timeout=5  --tries=1  -O /dev/null  "$DATA_REF" )  ||  \
      fatal  "Can NOT retrieve $DATA_DESCRIPTION '$DATA_REF'"
    
    DATA_URI="$DATA_REF"
  fi
}


#=============================================================================
#
#  Function  xw_send_file ()
#
#  Insert the local file given in parameter inside XtremWeb-HEP
#
#  Parameter :     FILE_PATH :  Local file path
#
#  Variable set :  DATA_URI :   Data URI
#
#=============================================================================
xw_send_file ()
{
  FILE_PATH="$1"
  FILE_NAME="${FILE_PATH##*/}"
  xw_action_success_or_fatal  "$XW_SEND_DATA"  $CFG  "$FILE_NAME"  \
                                                     "$FILE_PATH"
  DATA_URI=$(echo "$XW_MESSAGE"  |  grep '^xw://')
  [ "$DATA_URI" ]  ||  fatal_message_clean_exit  "$XW_MESSAGE"
}


#=============================================================================
#
#  MAIN again
#
#=============================================================================

#-----------------------------------------------------------------------------
#  Verify that the Live CD is an XtremWeb-HEP application, then get its name
#-----------------------------------------------------------------------------
[ -z "$VERBOSE" ]  ||  echo  > /dev/stderr

xw_action_success_or_fatal  "$XW_APPS"  $CFG  "$LIVE_CD"  --xwformat xml
XW_LIVE_CD_XML="$XW_MESSAGE"
    
OBJECT_TYPE=$( perl -we 'if ( $ARGV[0] =~ m/<([^\s]+)\s+uid=/i )
                         {print lc($1)}'  "$XW_LIVE_CD_XML" )  ||  \
  fatal  "Can NOT retrieve type of '$LIVE_CD' from '$XW_LIVE_CD_XML'"

[ "$OBJECT_TYPE" = "app" ]  ||  \
  fatal  "XtremWeb-HEP object '$LIVE_CD' is of type '$OBJECT_TYPE'"  \
         "instead of 'app'"

extract_xw_attribute_value  "app '$LIVE_CD'"  'name'  "$XW_LIVE_CD_XML"
LIVE_CD_NAME="$XW_ATTRIBUTE_VALUE"
LIVE_CD_DEFAULT=''

#-----------------------------------------------------------------------------
#  Verify the reference, get the name and try to get the URI of :
#  -  the context disk,
#  -  the user application disk (if provided).
#  Once BOTH references have been verified : 
#    If any reference is the name of a local file, add it to XtremWeb-HEP.
#-----------------------------------------------------------------------------
[ -z "$VERBOSE" ]  ||  echo  > /dev/stderr
verify_data_get_name_and_uri  'context disk'  "$CONTEXT_DISK"  \
                                              "$DISK_EXTENSIONS"
CONTEXT_DISK_NAME="$DATA_NAME"
CONTEXT_DISK_URI="$DATA_URI"

if [ "$USER_APP_DISK" ]; then
  verify_data_get_name_and_uri  'user application disk'  "$USER_APP_DISK"  \
                                                         "$DISK_EXTENSIONS"
  USER_APP_DISK_NAME="$DATA_NAME"
  #---------------------------------------------------------------------------
  #  Here BOTH references have been verified
  #---------------------------------------------------------------------------
  [ "$DATA_URI"  ]  ||  xw_send_file  "$USER_APP_DISK"
  USER_APP_DISK_URI="$DATA_URI"

#-----------------------------------------------------------------------------
#  If the user application disk is NOT provided :
#  -  Its uri is the defaultdirinuri of the Live CD,
#  -  Get its name.
#-----------------------------------------------------------------------------
else
  extract_xw_attribute_value  "app '$LIVE_CD_NAME'"  'defaultdirinuri'  \
                              "$XW_LIVE_CD_XML"
  LIVE_CD_DEFAULT="$XW_ATTRIBUTE_VALUE"
  
  xw_action_success_or_fatal  "$XW_DATAS"  $CFG  "$LIVE_CD_DEFAULT"  \
                              --xwformat xml
  extract_xw_attribute_value  "data '$LIVE_CD_DEFAULT'"  'name'  "$XW_MESSAGE"
  USER_APP_DISK_NAME="$XW_ATTRIBUTE_VALUE"
  [ "USER_APP_DISK_NAME" ]  ||  fatal "Can NOT retrieve 'name' of"  \
                                "'defaultdirinuri' from '$XW_MESSAGE'"
  
  USER_APP_DISK_URI="$LIVE_CD_DEFAULT"
fi

if [ -z "$CONTEXT_DISK_URI"  ]; then
  xw_send_file  "$CONTEXT_DISK"
  CONTEXT_DISK_URI="$DATA_URI"
fi

#-----------------------------------------------------------------------------
#  Zipped files are automatically unzipped by XtremWeb-HEP
#-----------------------------------------------------------------------------
LIVE_CD_NAME="${LIVE_CD_NAME%.[Zz][Ii][Pp]}"
CONTEXT_DISK_NAME="${CONTEXT_DISK_NAME%.[Zz][Ii][Pp]}"
USER_APP_DISK_NAME="${USER_APP_DISK_NAME%.[Zz][Ii][Pp]}"

#-----------------------------------------------------------------------------
#  If groups are available, take the first one
#-----------------------------------------------------------------------------
xw_action_success_or_fatal  "$XW_GROUPS"  $CFG
XW_MESSAGE=$(  echo   "$XW_MESSAGE"  |  grep  '^ *UID='  |  head  -1)
XW_GROUP_UID=$(echo   "$XW_MESSAGE"  |  cut   -d "'"  -f 2)
XW_GROUP_NAME=$(echo  "$XW_MESSAGE"  |  cut   -d "'"  -f 4)
GROUP=${XW_GROUP_UID:+--xwgroup "$XW_GROUP_UID"}

#-----------------------------------------------------------------------------
#  Submit to XtremWeb-HEP a job running the Live CD with the context disk and
#  the user application disk
#-----------------------------------------------------------------------------
if [ "$VERBOSE" ];  then
  echo  > /dev/stderr
  debug_message "XW_GROUP_NAME     ='$XW_GROUP_NAME'"
  debug_message "LIVE_CD_NAME      ='$LIVE_CD_NAME'"
  debug_message "LIVE_CD_DEFAULT   ='$LIVE_CD_DEFAULT'"
  debug_message "CONTEXT_DISK_NAME ='$CONTEXT_DISK_NAME'"
  debug_message "CONTEXT_DISK_URI  ='$CONTEXT_DISK_URI'"
  debug_message "USER_APP_DISK_NAME='$USER_APP_DISK_NAME'"
  debug_message "USER_APP_DISK_URI ='$USER_APP_DISK_URI'"
fi
[ -z "$VERBOSE" ]  ||  echo  > /dev/stderr

xw_action_success_or_fatal  "$XW_SUBMIT"  $CFG  $GROUP  "$LIVE_CD_NAME"  \
                            --scratch "$USER_APP_DISK_NAME"              \
                            --xwenv   "$USER_APP_DISK_URI"               \
                            --context "$CONTEXT_DISK_NAME"               \
                            --xwenv   "$CONTEXT_DISK_URI"

XW_JOB_UID=$(echo "$XW_MESSAGE"  |  grep  '^xw://')

#-----------------------------------------------------------------------------
#  Job detailed status
#-----------------------------------------------------------------------------
echo
"$XW_STATUS"  $CFG  "$XW_JOB_UID"  --xwformat xml  |  \
  perl  -wpe 's/"\s+/"\n  /g'

echo
#-----------------------------------------------------------------------------
echo "Looping on job status.   You can interrupt the loop with (ctrl)C if"  \
     "you wish so."
#-----------------------------------------------------------------------------
XW_STATUS_OLD=''
while true; do
  sleep 5
  XW_MESSAGE=$("$XW_RESULTS"  $CFG  "$XW_JOB_UID")
  XW_STATUS_NEW=$(echo  "$XW_MESSAGE"  |  \
              perl  -wpe  "s/^.*STATUS='([^']*)'.*\$/\$1/")
  if [ '(' -z "${XW_MESSAGE##*ERROR*:*}"  ')'  -o  \
       '(' "$XW_STATUS_NEW" = "ERROR"     ')'  -o  \
       '(' "$XW_STATUS_NEW" = "COMPLETED" ')' ]; then break; fi
  if [ "$XW_STATUS_NEW" != "$XW_STATUS_OLD" ]; then
    echo
    echo  -n  "$XW_MESSAGE "  |  sed  -e  's/, *[A-Za-z_]*=NULL//g'
    XW_STATUS_OLD="$XW_STATUS_NEW"
  else
    echo  -n  .
  fi
done

echo
echo  "$XW_MESSAGE"
