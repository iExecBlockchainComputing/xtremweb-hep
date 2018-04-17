#!/bin/bash

echo 'Starting Grafana...'
/run.sh "$@" &


echo "***************"
ls dashboards
echo "***************"

AddDataSource() {
  curl -H "Content-Type: application/json" \
   -X POST -d "{\"name\":\"iexec_mysql\", 
              	\"type\":\"mysql\", 
			  	\"access\":\"proxy\",
			   	\"url\":\"$DBHOST:3306\",
				\"password\":\"$MYSQL_PASSWORD\",
				\"user\":\"$MYSQL_USER\",
				\"database\":\"$MYSQL_DB_NAME\",
				\"basicAuth\":false,
				\"isDefault\":true,
				\"jsonData\":{\"keepCookies\":[]},
				\"readOnly\":false}" \
   http://admin:$GF_SECURITY_ADMIN_PASSWORD@localhost:3000/api/datasources
}

AddDashBoards(){
	for filename in /dashboards/*.json; do
		curl -H "Content-Type: application/json" \
			-X POST -d "`cat $filename`" \
		http://admin:$GF_SECURITY_ADMIN_PASSWORD@localhost:3000/api/dashboards/db
	done
}

until AddDataSource; do
  echo 'Configuring Data Sources in Grafana...'
  sleep 1
done

until AddDashBoards; do
  echo 'Configuring Dashboards in Grafana...'
  	sleep 1
done

echo "Grafana has been configured"

wait
