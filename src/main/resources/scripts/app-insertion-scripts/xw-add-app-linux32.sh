#!/bin/sh
#=============================================================================
#
# Copyright [2018] [CNRS] Etienne Urbah
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
#  Shell script for insertion inside XtremWeb-HEP of an application for Linux
#  32 bits :  Either 1 binary, or 1 script + 1 binary or zip
#
#  Parameters :   1: XtremWeb-HEP Client configuration file
#                    (for SUPER_USER rights)
#                 2: Application binary or script
#                 3: Optional binary, or ZIP containing application binaries,
#                    libraries, ...
#                 4: Optional default input file
#
#=============================================================================
if [ '(' $# -lt 2 ')' -o '(' $# -gt 4 ')' ]; then
  echo "Usage :  $0  Client_Config_File  App_File "  \
       "[BIN_or_ZIP_file  [Default_Input_File]]"
  exit 1
fi

#-----------------------------------------------------------------------------
#  Application files
#-----------------------------------------------------------------------------
XW_CONF="${1:-$HOME/.xtremweb/xtremweb.client.conf}"
XW_APP_FILE="$2"
XW_APP_BIN="$3"
XW_APP_DEFAULT_INPUT="$4"

#-----------------------------------------------------------------------------
#  Check the presence of files given in parameter
#-----------------------------------------------------------------------------
for FILE in "$XW_CONF" "$XW_APP_FILE" "$XW_APP_BIN" "$XW_APP_DEFAULT_INPUT"
do
  if [ '(' "$FILE" ')' -a \
       '(' '(' ! -f "$FILE" ')' -o '(' ! -r  "$FILE" ')' ')' ]; then
    echo "File '$FILE' NOT found or NOT readable"
    exit 2
  fi
done

#-----------------------------------------------------------------------------
#  Get the current XtremWeb-HEP user
#-----------------------------------------------------------------------------
unset X509_USER_PROXY
XW_BIN=$(ls -d /opt/xwhep-client-*  |  tail -1)/bin

echo
XW_USER=$($XW_BIN/xwversion --xwconfig "$XW_CONF" --xwverbose  |  \
          grep '^ *login *='  |  sed -e 's/^ *login *= *//')
if [ -z "$XW_USER" ]; then
  echo "No access to XtremWeb-HEP with '$XW_CONF'"
  exit 3
fi
echo "XW_USER=$XW_USER"

#-----------------------------------------------------------------------------
#  Get the access rights of the current XtremWeb-HEP user
#-----------------------------------------------------------------------------
XW_MESSAGE=$($XW_BIN/xwusers --xwconfig "$XW_CONF" "$XW_USER" --xwformat xml)
RC=$?
if [ $RC -ne 0 ]; then
  echo "$XW_MESSAGE"
  exit $RC
fi
XW_RIGHTS=$(echo "$XW_MESSAGE"   |  sed -e 's/"  */"\n  /g'  |  \
            grep '^ *rights *='  |
            sed -e 's/^ *rights *= *"\([^"]*\)".*$/\1/')
echo "        Rights=$XW_RIGHTS"
if [ "$XW_RIGHTS" != "SUPER_USER" ]; then
  echo "        Rights != SUPER_USER"
  echo -n "Application will NOT be public. "  \
          "Do you really want to continue [y/N] "
  read ANSWER
  if [ '(' "$ANSWER" != "Y" ')' -a '(' "$ANSWER" != "y" ')' ]; then
    exit 4
  fi
fi


#=============================================================================
#
#  Function xw_send_data (xw_file_path, xw_dont_zip)
#
#=============================================================================
xw_send_data ()
{
  XW_FILE_PATH="$1"
  XW_DONT_ZIP="$2"
  XW_FILE_NAME=$(basename "$XW_FILE_PATH")
  
  #---------------------------------------------------------------------------
  #  Insert the file inside XtremWeb-HEP
  #---------------------------------------------------------------------------
  XW_MESSAGE=$($XW_BIN/xwsenddata --xwconfig "$XW_CONF" $XW_DONT_ZIP  \
               "$XW_FILE_NAME" "$XW_FILE_PATH")
  RC=$?
  XW_FILE_URI=$(echo "$XW_MESSAGE"  |  grep '^xw://')
  if [ '(' $RC -ne 0 ')' -o '(' -z "$XW_FILE_URI" ')' ]; then
    echo $XW_MESSAGE
    exit $RC
  fi
  
  #---------------------------------------------------------------------------
  #  Verify the XtremWeb-HEP attributes of the inserted file
  #---------------------------------------------------------------------------
  XW_FILE_SIZE=$(stat -c '%s' "$XW_FILE_PATH")
  XW_FILE_MD5=$(md5sum "$XW_FILE_PATH" | cut -d ' ' -f 1)
  
  XW_ERROR=
  XW_TYPE=
  $XW_BIN/xwdatas --xwconfig "$XW_CONF" "$XW_FILE_URI" --xwformat xml  |  \
    sed -e 's/= *"/ /g; s/"  */\n/g'  |  \
  (
    while read ATTRIB VALUE; do
      if   [ "$ATTRIB" = "name" ]; then
        echo "  $ATTRIB=\"$VALUE\""
        if [ "$VALUE" != "$XW_FILE_NAME" ]; then
          XW_ERROR="$XW_ERROR  $ATTRIB=$VALUE!=$XW_FILE_NAME"
        fi
      elif [ "$ATTRIB" = "uri" ]; then
        echo "  $ATTRIB=\"$VALUE\""
        if [ "$VALUE" != "$XW_FILE_URI" ]; then
          XW_ERROR="$XW_ERROR  $ATTRIB=$VALUE!=$XW_FILE_URI"
        fi
      elif [ "$ATTRIB" = "type" ]; then
        echo "  $ATTRIB=\"$VALUE\""
        XW_TYPE="$VALUE"
      elif [ "$ATTRIB" = "size" ]; then
        # echo "  $ATTRIB=\"$VALUE\""
        XW_ATTR_SIZE="$VALUE"
      elif [ "$ATTRIB" = "md5" ]; then
        # echo "  $ATTRIB=\"$VALUE\""
        XW_ATTR_MD5="$VALUE"
      fi
    done
    
    #-------------------------------------------------------------------------
    #  XtremWeb-HEP filename and URI must match those already known
    #-------------------------------------------------------------------------
    if [ "$XW_ERROR" ]; then
      echo
      echo "ERROR in XtremWeb-HEP file:$XW_ERROR"
      exit 5
    fi
    
    #-------------------------------------------------------------------------
    #  If the XtremWeb-HEP size and SHASUM are those of the inserted file, OK
    #-------------------------------------------------------------------------
    if [ '(' "$XW_ATTR_SIZE" = "$XW_FILE_SIZE" ')' -a  \
         '(' "$XW_ATTR_MD5"  = "$XW_FILE_MD5"  ')' ]; then
      exit 0
    fi
    
    #-------------------------------------------------------------------------
    #  Here the XtremWeb-HEP attributes are NOT those of the inserted file.
    #  If zipping is NOT allowed, that is an error.
    #-------------------------------------------------------------------------
    if [ "$XW_DONT_ZIP" ]; then
      echo
      echo "Original file:                     size=$XW_FILE_SIZE "  \
           "md5=$XW_FILE_MD5"
      echo
      echo "ERROR in XtremWeb-HEP attributes:  size=$XW_ATTR_SIZE "  \
           "md5=$XW_ATTR_MD5"
      exit 6
    fi
  
    #-----------------------------------------------------------------------
    #  Here the XtremWeb-HEP attributes are NOT those of the inserted file,
    #  but zipping is allowed.
    #  Download the file stored inside XtremWeb-HEP to verify that
    #  XtremWeb-HEP had just zipped it.
    #-----------------------------------------------------------------------
    echo
    XW_FILE_DOWNLOAD="$XW_FILE_NAME.download"
    $XW_BIN/xwdownload --xwconfig "$XW_CONF" --xwout "$XW_FILE_DOWNLOAD"  \
                       "$XW_FILE_URI" > /dev/null
    RC=$?
    if [ $RC -ne 0 ]; then
      exit $RC
    fi
    
    XW_FILE_DOWNLOAD_TYPE=$(file "$XW_FILE_DOWNLOAD"  |  \
                            sed -e "s/^ *$XW_FILE_DOWNLOAD *: *//")
    
    #-------------------------------------------------------------------------
    #  If the type of the downloaded file is NOT ZIP, that is an error
    #-------------------------------------------------------------------------
    if ! expr "$XW_FILE_DOWNLOAD_TYPE" : "Zip archive data" > /dev/null; then
      echo "Original file:                      size=$XW_FILE_SIZE "  \
           "md5=$XW_FILE_MD5"
      echo
      echo "ERROR in XtremWeb-HEP attributes:   size=$XW_ATTR_SIZE "  \
           "md5=$XW_ATTR_MD5  type='$XW_TYPE'"
      echo
      echo "ERROR in downloaded file:         "  \
           " size="$(stat -c '%s' "$XW_FILE_DOWNLOAD")  \
           " md5="$(md5sum "$XW_FILE_DOWNLOAD" | cut -d ' ' -f 1)  \
           " type=$XW_FILE_DOWNLOAD_TYPE"
      exit 7
    fi
    
    #-------------------------------------------------------------------------
    #  If the size and SHASUM of the downloaded file are NOT those of the
    #  inserted file, that is an error
    #-------------------------------------------------------------------------
    if [ '(' $(gunzip -c "$XW_FILE_DOWNLOAD" | wc -c) !=  \
             "$XW_FILE_SIZE" ')' -o \
         '(' $(gunzip -c "$XW_FILE_DOWNLOAD" | md5sum | cut -d ' ' -f 1) !=  \
             "$XW_FILE_MD5" ')' ]; then
      echo "Original file:                      size=$XW_FILE_SIZE "  \
           "md5=$XW_FILE_MD5"
      echo
      echo "ERROR in XtremWeb-HEP attributes:   size=$XW_FILE_SIZE "  \
           "md5=$XW_FILE_MD5  type='$XW_TYPE'"
      echo
      echo "ERROR in unzipped downloaded file:"  \
           " size="$(gunzip -c "$XW_FILE_DOWNLOAD" | wc -c)  \
           " md5="$(gunzip -c "$XW_FILE_DOWNLOAD" | md5sum | cut -d ' ' -f 1)  \
           " type=$XW_FILE_DOWNLOAD_TYPE"
      exit 8
    fi
    
    #-------------------------------------------------------------------------
    #  Here the size and SHASUM of the downloaded file are those of the inserted
    #  file, OK
    #-------------------------------------------------------------------------
    rm "$XW_FILE_DOWNLOAD"
  )
  
  return $?
}
#=============================================================================
#  End of function xw_send_data
#=============================================================================


#-----------------------------------------------------------------------------
#  Insert the application binary or script inside XtremWeb-HEP with option to
#  forbid ZIP, then verify it
#-----------------------------------------------------------------------------
echo
xw_send_data  "$XW_APP_FILE"  --xwdontzip
RC=$?
if [ $RC -ne 0 ]; then
  echo
  ( set -x; $XW_BIN/xwrm --xwconfig "$XW_CONF" "$XW_FILE_URI" )
  exit $RC
fi
XW_APP_FILE_URI="$XW_FILE_URI"

#-----------------------------------------------------------------------------
#  Insert and verify the application binary or ZIP inside XtremWeb-HEP
#-----------------------------------------------------------------------------
XW_BASE_DIRIN_SPEC=
XW_DEFAULT_DIRIN_SPEC=

if [ "$XW_APP_BIN" ]; then
  echo
  xw_send_data  "$XW_APP_BIN"
  RC=$?
  if [ $RC -ne 0 ]; then
    echo
    (
      set -x
      $XW_BIN/xwrm --xwconfig "$XW_CONF" "$XW_APP_FILE_URI"
      $XW_BIN/xwrm --xwconfig "$XW_CONF" "$XW_FILE_URI"
    )
    exit $RC
  fi
  XW_BASE_DIRIN_URI="$XW_FILE_URI"
  XW_BASE_DIRIN_SPEC="basedirinuri   =\"$XW_FILE_URI\""
  
  #-----------------------------------------------------------------------------
  #  Insert and verify the application binary or ZIP inside XtremWeb-HEP
  #-----------------------------------------------------------------------------
  if [ "$XW_APP_DEFAULT_INPUT" ]; then
    echo
    xw_send_data  "$XW_APP_DEFAULT_INPUT"
    RC=$?
    if [ $RC -ne 0 ]; then
      echo
      (
        set -x
        $XW_BIN/xwrm --xwconfig "$XW_CONF" "$XW_APP_FILE_URI"
        $XW_BIN/xwrm --xwconfig "$XW_CONF" "$XW_BASE_DIRIN_URI"
        $XW_BIN/xwrm --xwconfig "$XW_CONF" "$XW_FILE_URI"
      )
      exit $RC
    fi
    XW_DEFAULT_DIRIN_SPEC="defaultdirinuri=\"$XW_FILE_URI\""
  fi
fi

#-----------------------------------------------------------------------------
#  Create inside XtremWeb-HEP the application without any binary
#-----------------------------------------------------------------------------
XW_APP_NAME=$(basename "$XW_APP_FILE")
XW_MESSAGE=$($XW_BIN/xwsendapp --xwconfig "$XW_CONF" $XW_APP_NAME  \
             deployable linux x86)
RC=$?
XW_APP_UID=$(echo "$XW_MESSAGE"  |  grep '^xw://'  |  sed -e 's=^.*/==')
if [ '(' $RC -ne 0 ')' -o '(' -z "$XW_APP_UID" ')' ]; then
  echo $XW_MESSAGE
  exit $RC
fi

#-----------------------------------------------------------------------------
#  Create the XML file for insertion of the application binaries inside
#  XtremWeb-HEP
#-----------------------------------------------------------------------------
echo
XW_XML="xw_app_$XW_APP_NAME.xml"
rm -f "$XW_XML"
grep -v '^ *$'  << END_OF_XML  > "$XW_XML"
<?xml version='1.0' encoding='UTF-8'?>
<app uid        ="$XW_APP_UID"
 name           ="$XW_APP_NAME"
 $XW_BASE_DIRIN_SPEC
 $XW_DEFAULT_DIRIN_SPEC
 linux_ix86uri  ="$XW_APP_FILE_URI"
 linux_amd64uri ="$XW_APP_FILE_URI"
 linux_x86_64uri="$XW_APP_FILE_URI" />
END_OF_XML

cat "$XW_XML"

#-----------------------------------------------------------------------------
#  Using the XML file, insert the application binaries inside XtremWeb-HEP
#-----------------------------------------------------------------------------
echo
$XW_BIN/xwsendapp --xwconfig "$XW_CONF" --xwxml "$XW_XML"
RC=$?
if [ $RC -ne 0 ]; then
  exit $RC
fi

#-----------------------------------------------------------------------------
#  Display the XtremWeb-HEP attributes of the inserted application
#-----------------------------------------------------------------------------
rm "$XW_XML"
echo
$XW_BIN/xwapps --xwconfig "$XW_CONF" "$XW_APP_NAME" --xwformat xml  |  \
  sed -e 's/"  */"\n  /g'
