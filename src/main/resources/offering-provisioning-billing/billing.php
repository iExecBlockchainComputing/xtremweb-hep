<?php
error_reporting(E_ALL);

/*==========================================================================
  
  Copyright 2015  E. URBAH
                  at LAL, Univ Paris-Sud, IN2P3/CNRS, Orsay, France
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--     http://www.apache.org/licenses/LICENSE-2.0
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
  XtremWeb-HEP Billing :  PHP script displaying billing views
  
  ==========================================================================*/
$userRightId_SuperUser = 37;

/*--------------------------------------------------------------------------
  'maintenance' variable :     Set it to TRUE if you want to prevent access
                               to the content of your server to general users.
  'maintenanceAddrs' array :   Keep it up to date with the list of the
                               IP addresses allowed to access the content of
                               your server during maintenance work.
  'b_db_selectable' boolean :  Permits maintainers to choose database server,
                               database user and schema
  --------------------------------------------------------------------------*/
$maintenance      = FALSE;
$maintenanceAddrs = array( '134.158.76.204' => 'mac-xwhep',
                           '134.158.89.155' => 'lodygensky',
                           '134.158.90.77'  => 'urbah'      );
$b_db_selectable  = TRUE;

/*--------------------------------------------------------------------------*/
$remoteAddr       = $_SERVER['REMOTE_ADDR'];
$serverName       = $_SERVER['SERVER_NAME'];
$scriptName       = $_SERVER['PHP_SELF'];

$scriptFolder     = substr($scriptName, 0,
                           strrpos($scriptName, DIRECTORY_SEPARATOR) + 1);

$b_verbose        = ( strpos($scriptName, 'verbose') !== FALSE );

$b_maintainer     = array_key_exists($remoteAddr, $maintenanceAddrs);
/*
echo "\$maintenance = '",              $maintenance,            "'<br>\n";
echo "(\$maintenance === 'ALL') = '", ($maintenance === 'ALL'), "'<br>\n";
echo "\$b_maintainer = '",             $b_maintainer,           "'<br>\n";
*/

if  ( ($maintenance === 'ALL') or ($maintenance and ! $b_maintainer) )
{
  if  ( $maintenance === 'ALL' )
      { header($_SERVER['SERVER_PROTOCOL'].' 503 Service Unavailable'); }
  
  echo '<html><head><title>', $serverName. ' : ', $scriptName,
       ' : Server maintenance</title></head>';
?>
  
<html>
  <body>
    <h1>
      <center><u>2015/01/12</u></center>
      <br>
      This server is under maintenance.<br>
      Services would be available soon.<br><br>
      Sorry for inconveniences.<br><br>
    </h1>
    <address><a href="mailto:lodygens at lal.in2p3.fr">Oleg Lodygensky</a></address>
  </body>
</html>

<?php
  return;
}

/*==========================================================================
  Begin of main code
  ==========================================================================*/

/*--------------------------------------------------------------------------
  Analyse protocol
  --------------------------------------------------------------------------*/
$server_protocol = $_SERVER['SERVER_PROTOCOL'];
$request_method  = $_SERVER['REQUEST_METHOD'];
$https           = $_SERVER['HTTPS'];
$forw_proto      = $_SERVER['HTTP_X_FORWARDED_PROTO'];
$forw_ssl        = $_SERVER['HTTP_X_FORWARDED_SSL'];
$ssl_protocol    = $_SERVER['SSL_PROTOCOL'];

$is_HTTPS        = ( isset($https)      and ($https      === 'on'   ) ) or
                   ( isset($forw_proto) and ($forw_proto === 'https') ) or
                   ( isset($forw_ssl)   and ($forw_ssl   === 'on'   ) ) or
                     isset($ssl_protocol);

/*--------------------------------------------------------------------------
  Analyse URL
  --------------------------------------------------------------------------*/
$path_info    = $_SERVER['PATH_INFO'];
$query_string = $_SERVER['QUERY_STRING'];

$arguments    = explode("&", $query_string);
if  ( in_array('verbose', $arguments) )
    { $b_verbose = TRUE; }

parse_str($query_string, $arg_vars);
$view = $arg_vars['View'];

/*--------------------------------------------------------------------------
  Define the page title, icon and styles
  --------------------------------------------------------------------------*/
echo "<html>\n",
     "  <head>\n",
     "    <title>", $serverName, " : ", $scriptName, "</title>\n",
     '    <link rel="icon" type="image/jpeg" href="jeep-nb-sandfond.jpg"/>',
         "\n",
     '    <style type="text/css">', "\n",
     "      .big, .bold     { font-family: Arial, Helvetica, sans-serif;\n",
     "                        font-weight: bold; }\n",
     "      .big            { font-size: 125%; }\n",
     "      .whiteSmoke     { background-color: WhiteSmoke; }\n",
     "      .myButton       { background-color: White;\n",
     "                        font-weight: bold;\n",
     "                        color: DarkBlue;\n",
     "                        border: 0; }\n",
     "      .myButton:hover { text-decoration: underline;\n",
     "                        cursor: pointer; }\n",
     "    </style>\n",
     "  </head>\n",
     "  <body>\n";

if  ( $b_verbose )
    {
      echo "    $scriptName : $serverName called from $remoteAddr<br>\n",
           "    $scriptName : Server Protocol : '$server_protocol'<br>\n",
           "    $scriptName : Request method :  '$request_method'<br>\n",
           "    $scriptName : Path Info :       '$path_info'<br>\n",
           "    $scriptName : Query string :    '$query_string'<br>\n",
           "    $scriptName : Arguments :       ", count($arguments), " : '",
                implode("' '", $arguments), "'<br>\n",
           "    $scriptName : ";  var_dump($arg_vars);  echo "<br>\n",
           "    <br>\n",
           "    $scriptName : HTTPS :           '$https'<br>\n",
           "    $scriptName : Forw. Protocol :  '$forw_protocol'<br>\n",
           "    $scriptName : Forw. SSL :       '$forw_ssl'<br>\n",
           "    $scriptName : SSL Protocol :    '$ssl_protocol'<br>\n",
           "    $scriptName : Request Protocol : HTTP",
                 ($is_HTTPS ? 'S' : ''), "<br>\n";
    }

if  ( ! $is_HTTPS )
    {
      echo "    $scriptName : NOT called using HTTPS<br>\n";
      return;
    }

/*--------------------------------------------------------------------------
  Analyse X509 certificate of the Client
  --------------------------------------------------------------------------*/
$ssl_client_cert = $_SERVER['SSL_CLIENT_CERT'];
$x509_issuer     = $_SERVER['SSL_CLIENT_I_DN'];
$x509_subject    = $_SERVER['SSL_CLIENT_S_DN'];
$x509_verified   = $_SERVER['SSL_CLIENT_VERIFY'];

if  ( $b_verbose )
    {
      echo "    <br>\n",
           "    $scriptName : SSL Client Cert : ",
           (empty($ssl_client_cert) ? 'Absent' : 'Present '), "<br>\n",
           "    $scriptName : X509 Issuer :     '$x509_issuer'<br>\n",
           "    $scriptName : X509 Subject :    '$x509_subject'<br>\n",
           "    $scriptName : X509 verified :   '$x509_verified'<br>\n";
    }

if  ( ($x509_verified === 'NONE') or empty($x509_issuer)
                                  or empty($x509_subject) )
    {
      echo "    $scriptName : NO verified X509 certificate<br>\n";
      return;
    }

if  ( isset($ssl_client_cert) )
{
  $cert_data = openssl_x509_parse($ssl_client_cert);

  $common_name = ( is_array($cert_data['subject']['CN']) ?
                   $cert_data['subject']['CN'][0] :
                   $cert_data['subject']['CN']);
  if  ( $b_verbose )
      { echo "    $scriptName : Common Name :     '$common_name'<br>\n"; }
}

echo '    <img align=right src="" alt="', $x509_subject, ' &nbsp;"/>', "\n",
     '    <span class="big">&nbsp; XtremWeb-HEP Billing : &nbsp; ', $view,
         "</span><br>\n    <br>\n";

/*--------------------------------------------------------------------------
  Verify that the PHP function permitting to connect to MySQL is available.
  When using 'yum', this function is provided by the 'php-mysql' package.
  --------------------------------------------------------------------------*/
$mysql                   = 'mysqli';
$mysql_connect           = $mysql.'_connect';
$mysql_connect_available = function_exists($mysql_connect);
if      ( ! $mysql_connect_available )
        {
          echo "    $scriptName : Function '", $mysql_connect,
               "' is NOT available<br>\n";
          return;
        }
elseif  ( $b_verbose )
        { echo "    $scriptName : Function '", $mysql_connect,
               "' is available<br>\n"; }

/*--------------------------------------------------------------------------
  Database server, database user and schema
  --------------------------------------------------------------------------*/
$variableNames  = array('dbserver', 'dbuser', 'dbschema');

if  ( $b_db_selectable )
    {
      foreach  ( $variableNames as $variableName )
               { ${$variableName} = $arg_vars[$variableName]; }
      if  ( $b_verbose )
          { echo "    $scriptName : dbserver='$dbserver', ",
                 "dbuser='$dbuser', dbschema='$dbschema'<br>\n"; }
    }

$b_database_specified = TRUE;
foreach  ( $variableNames as $variableName )
         {
           if  ( empty(${$variableName}) )
               { $b_database_specified = FALSE; }
         }

/*--------------------------------------------------------------------------
  If necessary, read database configuration file
  --------------------------------------------------------------------------*/
if  ( ! $b_database_specified )
{
  $configFileName = 'database.conf';

  $configFilePath = /* $scriptFolder. */ $configFileName;
  if  ( $b_verbose )
      { echo "    $scriptName : File '$configFilePath'<br>\n"; }

  $lines = file($configFilePath, FILE_IGNORE_NEW_LINES);
  if  ( $lines === FALSE )
      {
        echo "    $scriptName : File '$configFilePath' : Read ERROR<br>\n";
        return;
      }
  if  ( $b_verbose )
      { echo "    $scriptName : File '$configFilePath' has ", count($lines),
             " lines<br>\n"; }

  foreach  ( $lines as $myLine )
  {
    $myLine = ltrim($myLine);
    if  ( $myLine[0] === '#' )
        { continue; }
    $tokens = explode('=', $myLine);
    $token0 = trim($tokens[0]);
    foreach  ( $variableNames as $variableName )
    {
      if  ( ($variableName === $token0) and (empty(${$variableName})) )
          { ${$variableName} = trim($tokens[1]); }
    }
  }
  unset($lines);
  $b_database_specified = TRUE;
}

/*--------------------------------------------------------------------------
  Create a form for begin date, end date and view names
  --------------------------------------------------------------------------*/
echo '    <form action="', $scriptName, '" method="GET">', "\n";

/*--------------------------------------------------------------------------
  Text inputs permitting the maintainers to choose database server, database
  user and schema
  --------------------------------------------------------------------------*/
if   ( $b_db_selectable and $b_maintainer )
{
  echo '      dbserver : <input type="text" class="bold" name="dbserver" ',
              'value="', $dbserver, '" ', "$db_input_attrs> &nbsp; &nbsp;\n",
       '      dbuser   : <input type="text" class="bold" name="dbuser" ',
              'value="', $dbuser,   '" ', "$db_input_attrs> &nbsp; &nbsp;\n",
       '      dbschema : <input type="text" class="bold" name="dbschema" ',
              'value="', $dbschema, '" ', "$db_input_attrs> &nbsp; &nbsp;",
              "<br>\n",
       "      <br>\n";
}
/*  else  'readonly style="background-color: LightGray"'  /*

/*--------------------------------------------------------------------------
  Text inputs for begin and end dates, and display of the correct format
  --------------------------------------------------------------------------*/
$date_pattern = '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]';
$begin_date   = $arg_vars['Begin_date'];
$end_date     = $arg_vars['End_date'];

if  ( empty($begin_date) )
    { $begin_date = date('Y').'-01-01'; }
if  ( empty($end_date) )
    { $end_date   = date('Y-m-d'); }

echo '      Begin date : <input type="text" name="Begin_date" ',
            'value="', $begin_date, '"> &nbsp; &nbsp;', "\n",
     '        End date : <input type="text" name=  "End_date" ',
              'value="',$end_date, '"> &nbsp; &nbsp;', "\n";

if  ( ! fnmatch($date_pattern, $begin_date) or
      ! fnmatch($date_pattern, $end_date  ) )
    {
      $b_bad_date = TRUE;
      echo "<em>The format of dates must be 'YYYY-MM-DD'</em>";
    }

echo "      <br>\n",
     "      <br>\n";

if  ( ! $b_database_specified )
    { return; }

/*--------------------------------------------------------------------------
  Read another database configuration file
  --------------------------------------------------------------------------*/
$configFileName = '.DS_Store';
$configFilePath = /* $scriptFolder. */ $configFileName;
/* echo $scriptName, " : File '", $configFilePath, "'<br>\n"; */
$line = trim(file_get_contents($configFilePath, FILE_IGNORE_NEW_LINES));
if  ( $line === FALSE )
    {
      echo "    $scriptName : File '$configFilePath' : Read ERROR<br>\n";
      return;
    }
/* echo $scriptName, " : File '", $configFilePath, "' : '",$line,"'<br>\n"; */

/*--------------------------------------------------------------------------
  Connect to MySQL
  --------------------------------------------------------------------------*/
$logmsg       = "    $scriptName : '$mysql_connect' to schema '$dbschema' ".
                "of '$dbserver' as '$dbuser' ";
$retry_button = "    <br>\n".
                '    <input type="submit" value="Retry">'."<br>\n";

if  ( $b_verbose )
    {
      echo "    <br>\n",
           $logmsg, "...<br>\n";
    }
$connection = $mysql_connect($dbserver, $dbuser,
                      implode(array_reverse(explode("n", $line))),
                             $dbschema);
if      ( ! $connection )
        {
          echo $logmsg, ": ERROR ", mysqli_connect_errno(), " :<br>\n",
                        mysqli_connect_error(), "<br>\n", $retry_button;
          return;
        }
elseif  ( $b_verbose )
        { echo $logmsg, "succeeded : '", mysqli_get_host_info($connection),
                        "'<br>\n"; }

/*--------------------------------------------------------------------------
  Inside the subject DN and the issuer DN of the Client certificate,
  replace quotes and blanks with underscores, then extract the tokens.
  Then try to calculate the XtremWeb-HEP login (NOT reliable).
  --------------------------------------------------------------------------*/
foreach  ( array('subject', 'issuer') as $stakeholder )
{
  $x509_stakeholder          = strtr(${'x509_'.$stakeholder}, "' ", "__");
  $x509_tokens[$stakeholder] = explode('/', substr($x509_stakeholder, 1));
  
  $xw_tokens = array( 3 => NULL );  unset($xw_tokens[3]);     # Next index = 4
  foreach  ( $x509_tokens[$stakeholder] as $num => $x509_token )
  {
    list($field) = explode('=', $x509_token);
    switch  ( $field )
    {
      case 'CN' :
        $xw_tokens[0] = $x509_token;
        break;
      case 'OU' :
        $xw_tokens[1] = $x509_token;
        break;
      case 'O' :
        $xw_tokens[2] = $x509_token;
        break;
      case 'C' :
        $xw_tokens[3] = $x509_token;
        break;
      case 'emailAddress' :
        unset($x509_tokens[$stakeholder][$num]);
        break;
      default :
        $xw_tokens[] = $x509_token;
        break;
    }
  }
  ksort($xw_tokens);
  $xw_dn[$stakeholder] = implode(',', $xw_tokens);
  if  ( $b_verbose )
      { echo "    $scriptName : XW $stakeholder : '", $xw_dn[$stakeholder],
             "'<br>\n"; }
}

$sql_query = "select userRightId, isdeleted from users where login like '%".
             $xw_dn['subject']."_".$xw_dn['issuer']."'";
if  ( $b_verbose )
    { echo "    $scriptName : SQL query='$sql_query'<br>\n"; }

$sql_query = "select login, userRightId, isdeleted from users ".
                       "where (login like '%".
             implode("%') and (login like '%", $x509_tokens['subject']).
                     "%') and (login like '%".
             implode("%') and (login like '%", $x509_tokens['issuer'])."%')";
if  ( $b_verbose )
    { echo "    $scriptName : SQL query='$sql_query'<br>\n"; }

/*--------------------------------------------------------------------------
  Verify that the DN of the Client certificate
  is a registered XtremWeb-HEP user
  --------------------------------------------------------------------------*/
$resultSet = mysqli_query($connection, $sql_query);   
if  ( ! $resultSet )
    {
      echo "    $scriptName : $sql_query: ERROR ", mysqli_errno($connection),
           " :<br>\n", mysqli_error($connection), "<br>\n", $retry_button;
      return;
    }

$num_rows = mysqli_num_rows($resultSet);
if  ( $num_rows < 1 )
    {
      echo "    $scriptName : User NOT found --> Access NOT authorized<br>\n",
           $retry_button;
      return;
    }

$row        = mysqli_fetch_assoc($resultSet);
$login      = $row['login'];
$is_deleted = $row['isdeleted'];
if  ( ($is_deleted === TRUE) or ($is_deleted === 'true') )
    {
      echo "    $scriptName : User deleted --> Access NOT authorized<br>\n",
           $retry_button;
      return;
    }

$userRightId = $row['userRightId'];
if  ( $b_verbose )
    { echo "    $scriptName : userRightId = '$userRightId'<br>\n"; }

if  ( $b_verbose )
    { echo "    <br>\n"; }

/*--------------------------------------------------------------------------
  Display available views.
  If the query string is the name of an available view, display this view.
  --------------------------------------------------------------------------*/
$b_list_views = TRUE;
$sql_query    = 'select table_name as "'.$dbserver.' &nbsp; : &nbsp; '.
                                        $dbschema.'" '.
                'from information_schema.tables '.
                "where (engine is null)                       and ".
                      "(table_schema = schema())              and ".
                      "(left(table_name, 5) = binary 'View_') and ".
                      "(table_name not like '%_Base')         and ".
                      "(table_name not like '%_Helper')";

while  ( isset($sql_query) )
{
  $query_view = '';                            /* Permits to exit from loop */
  
  if  ( $b_verbose )
      { echo "    $scriptName : $sql_query<br>\n"; }
  
  /*------------------------------------------------------------------------
    Flushing works only if the buffer is large enough  (4 kB)
    ------------------------------------------------------------------------*/
  for  ( $i = 1; $i <= 16; $i++ )
       { echo "                                ",
              "                                ",
              "                                ",
              "                                ",
              "                                ",
              "                                ",
              "                                ",
              "                                \n"; }
  flush();
  
  /*------------------------------------------------------------------------
    Query the content of the view from MySQL
    ------------------------------------------------------------------------*/
  $query_time = microtime(TRUE);
  
  $resultSet  = mysqli_query($connection, $sql_query/*, MYSQLI_USE_RESULT*/);   
  
  $query_time = round(microtime(TRUE) - $query_time, 1);
  
  if  ( ! $resultSet )
      { echo "    $scriptName : ", $sql_query, ": ERROR ".
             mysqli_errno($connection)." :<br>\n", mysqli_error($connection),
             "<br>\n", $retry_button; }
  else
      {
        $nb_rows = mysqli_num_rows($resultSet);
        
        if  ( $b_list_views )
            {
              if  ( $b_verbose or ($nb_rows <= 0) )
                  { echo "    ", ($b_verbose ? "$scriptName : " : ""),
                         "Query on available views returned <b>", $nb_rows,
                         '</b> rows in ', $query_time, " s<br>\n"; }
              if  ( $nb_rows <= 0 )
                  { echo $retry_button; }
            }
        else
            { echo "    ", ($b_verbose ? "$scriptName : " : ""),
                   '<span class="big">', $view, '</span> : &nbsp; ',
                   'Query returned <b>', $nb_rows, '</b> rows in ',
                   $query_time, " s<br>\n",
                   "    <br>\n"; }
        
        /*------------------------------------------------------------------
          Display the current view as a table
          ------------------------------------------------------------------*/
        if  ( $nb_rows > 0 )
        {
          $b_first_row = TRUE;
          echo '      <table border=',
                     ( $b_list_views ? '"3" rules="none" ' : '"1"' ),
                     ' cellspacing="0" cellpadding="4">', "\n";
          
          while  ( $row = mysqli_fetch_assoc($resultSet) )
          {
            /*--------------------------------------------------------------
              Display the table header
              --------------------------------------------------------------*/
            if  ( $b_first_row )
                {
                  $b_whiteSmoke = TRUE;
                  $columns      = array_keys($row);
                  echo "        <thead>\n",
                       '          <tr ',  ( $b_list_views ?
                                            ' class="big"' :
                                            ' class="whiteSmoke"' ),
                                 ">\n";
                  foreach  ( $columns as $value )
                  { echo '            <th>', $value, "</th>\n"; }
                  echo "          </tr>\n",
                       "        </thead>\n",
                       "        <tbody>\n";
                  $b_first_row = FALSE;
                }
            
            /*--------------------------------------------------------------
              Display the current table row
              --------------------------------------------------------------*/
            $b_whiteSmoke = ! ( $b_list_views or $b_whiteSmoke);
            echo '          <tr', ( $b_whiteSmoke ?
                                    ' class="whiteSmoke"' :
                                    '' ),
                           ">\n";
            foreach  ( $row as $value )
            {
              echo "            <td>";
              if  ( strncmp($value, 'View_', 5) != 0 )
                  { echo (isset($value) ? $value : '&nbsp;'); }
              else
              {
                echo '<input type="submit"', ' class="myButton"',
                     ' name="View" value="', $value, '">';
                if  ( $value === $view )
                    { $query_view = $view; }
              }
              echo "</td>\n";
            }
            echo "          </tr>\n";
          }
          
          /*----------------------------------------------------------------
            Terminate the table
            ----------------------------------------------------------------*/
          echo "        </tbody>\n",
               "      </table>\n";
        }
        
        /*------------------------------------------------------------------
          If the current view is the list of views, then terminate the form
          ------------------------------------------------------------------*/
        if  ( $b_list_views )
            { echo "    </form>\n"; }
      }
  
  mysqli_free_result($resultSet);
  
  $b_list_views = FALSE;
  
  /*------------------------------------------------------------------------
    If the user has selected a view, build the associated query string
    ------------------------------------------------------------------------*/
  if  ( empty($query_view) or $b_bad_date )
      { unset($sql_query); }
  else
      {
        $sql_query = "select * from ".$query_view;
        
        /*------------------------------------------------------------------
          Restrict to records belonging to the current user
          ------------------------------------------------------------------*/
        if  ( $userRightId < $userRightId_SuperUser )
            {
              unset($customer_title);
              if      ( strpos($query_view, 'View_Customer') === 0 )
                      { $customer_title = 'CustomerAccount'; }
              elseif  ( strpos($query_view, 'View_Billing_') === 0 )
                      { $customer_title = 
                        ( strpos($query_view, '_With_Long_Titles') ?
                          'CustomerAccount' :
                          'Cust' ); }
              if  ( isset($customer_title) )
                  {
                    $sql_query .=
                        " where ( (".$customer_title." = '".$login."')";
                    $sql_query .= ( strpos($dbschema, "test") !== FALSE ?
                        "      or (".$customer_title." = 'oleg') )" :
                        " )" );
                  }
            }
            
        /*------------------------------------------------------------------
          Restrict to records between begin and end dates
          ------------------------------------------------------------------*/
        if      ( $query_view === 'View_Billing_By_Day' )
                { $sql_query .= " and (Day >= '".$begin_date."')".
                                " and (Day <= '".$end_date  ."')"; }
        
        elseif  ( $query_view === 'View_Billing_By_Day_With_Long_Titles' )
                { $sql_query .= " and (BillingDay >= '".$begin_date."')".
                                " and (BillingDay <= '".$end_date  ."')"; }
        
        elseif  ( $query_view === 'View_Billing_By_Hour' )
                { $sql_query .=
                      " and (left(Hour, 10) >= '".$begin_date."')".
                      " and (left(Hour, 10) <= '".$end_date  ."')"; }
        
        elseif  ( $query_view === 'View_Billing_By_Hour_With_Long_Titles' )
                { $sql_query .=
                      " and (left(BillingHour, 10) >= '".$begin_date."')".
                      " and (left(BillingHour, 10) <= '".$end_date  ."')"; }
        
        elseif  ( $query_view === 'View_Billing_By_Month' )
                { $sql_query .=
                      " and (Month >= '".substr($begin_date, 0, 7)."')".
                      " and (Month <= '".substr($end_date  , 0, 7)."')"; }
        
        elseif  ( $query_view === 'View_Billing_By_Month_With_Long_Titles' )
                { $sql_query .=
                  " and (BillingMonth >= '".substr($begin_date, 0, 7)."')".
                  " and (BillingMonth <= '".substr($end_date  , 0, 7)."')"; }
        
        elseif  ( strpos($query_view, 'View_Billing_Details') === 0 )
                { $sql_query .=
                      " and (left(completedDate, 10) >= '".$begin_date."')".
                      " and (left(completedDate, 10) <= '".$end_date  ."')"; }
      }
}

mysqli_close($connection);

echo "  </body>\n";
echo "</html>\n";

?>
