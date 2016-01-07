#!/bin/bash -e
#
#
# Startup script for Datafari
#
#

if (( EUID != 0 )); then
   echo "You need to be root to run this script." 1>&2
   exit 100
fi

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/set-datafari-env.sh"
source "${DIR}/utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE


if is_running $CATALINA_PID; then
    echo "Error: Tomcat seems to be running already with PID $(cat $TOMCAT_PID_FILE)"
    exit 1
fi

if is_running $MCF_PID_FILE; then
    echo "Error: MCF Agent seems to be running already with PID $(cat $MCF_PID_FILE)"
    exit 1
fi

if is_running $SOLR_PID_FILE; then
   echo "Error : Solr seems to be running already with PID $(cat $SOLR_PID_FILE)"
   exit 1
fi

if is_running $CASSANDRA_PID_FILE; then
   echo "Error : Cassandra seems to be running already with PID $(cat $CASSANDRA_PID_FILE)"
   exit 1
fi

if  [[ "$STATE" = *installed* ]];
then
	echo "Start postgres and cassandra and add ManifoldCF database"
	
	rm -rf "${DATAFARI_HOME}/pgsql/data"
	mkdir "${DATAFARI_HOME}/pgsql/data"
	
	rm -rf "${DATAFARI_HOME}/cassandra/data"
	mkdir "${DATAFARI_HOME}/cassandra/data"
	
	CASSANDRA_INCLUDE=$CASSANDRA_ENV
	# Redirect stdout and stderr to log file to ease startup issues investigation
	$CASSANDRA_HOME/bin/cassandra -p $CASSANDRA_PID_FILE &>$DATAFARI_LOGS/cassandra-startup.log
	# Note: Cassandra start command returns 0 even if something goes wrong at startup. 
	# This is why hereafter we check pid and we see if the Cassandra ports are open.

	# Get the process ID assigned to Cassandra
	pid=$(head -n 1 $CASSANDRA_PID_FILE)

	# Check if Cassandra process is running
	cassandra_process=$(ps -Alf | grep $pid | grep org.apache.cassandra.service.CassandraDaemon)

	if [ -z "$cassandra_process" ]; then
		echo "/!\ ERROR: Cassandra process is not running."
	else
		echo "Cassandra process running with PID ${pid} --- OK"
	fi
	
	id -u postgres &>/dev/null || useradd postgres
	chown -R postgres "${DATAFARI_HOME}/pgsql"
	chmod -R 777 "${DATAFARI_LOGS}"
	chmod -R 777 "${DATAFARI_HOME}/pid"
	su postgres -c "${DATAFARI_HOME}/pgsql/bin/initdb -U postgres -A password --pwfile=${DATAFARI_HOME}/pgsql/pwd.conf -E utf8 -D ${DATAFARI_HOME}/pgsql/data"
	cp "${DATAFARI_HOME}/pgsql/postgresql.conf.save" "${DATAFARI_HOME}/pgsql/data/postgresql.conf"
	su postgres -c "${DATAFARI_HOME}/pgsql/bin/pg_ctl -D ${DATAFARI_HOME}/pgsql/data -l ${DATAFARI_LOGS}/pgsql.log start"
	cd "${DATAFARI_HOME}/mcf/mcf_home"
	bash "initialize.sh"
	
	echo "Checking if Cassandra is up and running ..."
	# Try to connect on Cassandra's JMX port 7199
	nc -z localhost 7199 
	nc_return=$?

	# Try to connect on Cassandra CQLSH port 9042
	nc -z localhost 9042 
	let "cassandra_status = nc_return + $?"

	retries=1
    while (( retries < 6 && cassandra_status != 0 )); do
		echo "Cassandra doesn't reply to requests on ports 7199 and/or 9042. Sleeping for a while and trying again... retry ${retries}"

		# Sleep for a while
        sleep 2s
		
		# Try again to connect to Cassandra
		echo "Checking if Cassandra is up and running ..."
		nc -z localhost 7199 
		nc_return=$?

		nc -z localhost 9042 
		let "cassandra_status = nc_return + $?"

		let "retries++"
    done

	if [ $cassandra_status -ne 0 ]; then
		echo "/!\ ERROR: Cassandra startup has ended with errors; please check log file ${DATAFARI_LOGS}/cassandra-startup.log"
	else
		
        echo "Cassandra startup completed successfully --- OK"
		$CASSANDRA_HOME/bin/cqlsh -f $DATAFARI_HOME/bin/common/config/cassandra/tables 
	fi
fi

cd $TOMCAT_HOME/bin
bash startup.sh

if  [[ "$STATE" = *installed* ]];
then
	cd "${DATAFARI_HOME}/bin/common"
	"${JAVA_HOME}/bin/java" -cp DatafariScripts.jar com.francelabs.manifoldcf.configuration.script.BackupManifoldCFConnectorsScript RESTORE config/manifoldcf/monoinstance
	sed -i "s/\(STATE *= *\).*/\1initialized/" $INIT_STATE_FILE

else
	su postgres -c "${DATAFARI_HOME}/pgsql/bin/pg_ctl -D ${DATAFARI_HOME}/pgsql/data -l ${DATAFARI_LOGS}/pgsql.log start"
	CASSANDRA_INCLUDE=$CASSANDRA_ENV $CASSANDRA_HOME/bin/cassandra -p $CASSANDRA_PID_FILE 1>/dev/null
fi

cd $MCF_HOME/../bin
bash mcf_crawler_agent.sh start

SOLR_INCLUDE=$SOLR_ENV $SOLR_INSTALL_DIR/bin/solr start
