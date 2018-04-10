REM 
REM  Acknowledgment : XtremWeb-HEP is based on XtremWeb 1.8.0 by inria : http://www.xtremweb.net/
REM  Web            : http://www.xtremweb-hep.org
REM  
REM       This file is part of XtremWeb-HEP.
REM 
REM Copyright [2018] [CNRS] Oleg Lodygensky

REM Licensed under the Apache License, Version 2.0 (the "License");
REM you may not use this file except in compliance with the License.
REM You may obtain a copy of the License at

REM     http://www.apache.org/licenses/LICENSE-2.0

REM Unless required by applicable law or agreed to in writing, software
REM distributed under the License is distributed on an "AS IS" BASIS,
REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM See the License for the specific language governing permissions and
REM limitations under the License.
REM 
REM 



java -Djavax.net.ssl.trustStore=..\keystore\xwhepclient.keys -cp ..\lib\xtremweb.jar;..\lib\bcprov-jdk16-140.jar xtremweb.client.Client --xwconfig "%USERPROFILE%\.xtremweb\xtremweb.client.conf"  --xwgui
