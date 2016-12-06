#!/bin/bash -e
#
#
# Startup script for Datafari
#
#

if (( EUID == 0 )); then
   echo "You need to be a non-root user to run this script." 1>&2
   exit 100
fi

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/set-datafari-env.sh"
source "${DIR}/utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE


if is_running $CATALINA_PID; then
    echo "Error: Tomcat seems to be already running with PID $(cat $CATALINA_PID)"
    exit 1
fi

if is_running $MCF_PID_FILE; then
    echo "Error: MCF Agent seems to be already running with PID $(cat $MCF_PID_FILE)"
    exit 1
fi

if is_running $SOLR_PID_FILE; then
   echo "Error : Solr seems to be already running with PID $(cat $SOLR_PID_FILE)"
   exit 1
fi

if is_running $CASSANDRA_PID_FILE; then
   echo "Error : Cassandra seems to be already running with PID $(cat $CASSANDRA_PID_FILE)"
   exit 1
fi

if is_running $ZK_PID_FILE; then
   echo "Error : Zookeeper seems to be already running with PID $(cat $ZK_PID_FILE)"
   exit 1
fi



if  [[ "$STATE" = *installed* ]];
then
	# Configure ELK
	echo "Configure ELK"
	cd $ELASTICSEARCH_HOME/bin
	sudo -E su datafari -p -c "bash elasticsearch -p $ELASTICSEARCH_PID_FILE" &
	sleep 7
	sudo -E su datafari -p -c "sed -i '/pid\.file/c\pid.file: ${KIBANA_PID_FILE}' $KIBANA_HOME/config/kibana.yml"
	cd $KIBANA_HOME/bin
	sudo -E su datafari -p -c "bash kibana" &
	sleep 10
	kibana_config=$(curl -s http://localhost:9200/.kibana/config/_search | jq -r '.hits.hits | .[0] | ._id')
	curl -H 'Content-Type: application/json' -XPUT -d @/opt/datafari/elk/logstash/templates/datafari-monitoring-template.json http://localhost:9200/_template/datafari-monitoring
	curl -H 'Content-Type: application/json' -XPUT -d @/opt/datafari/elk/logstash/templates/datafari-statistic-template.json http://localhost:9200/_template/datafari-statistics
	curl -H 'Content-Type: application/json' -XPUT -d @/opt/datafari/elk/save/index-pattern-kibana-monitoring.json http://localhost:9200/.kibana/index-pattern/monitoring
	curl -H 'Content-Type: application/json' -XPUT -d @/opt/datafari/elk/save/index-pattern-kibana-statistics.json http://localhost:9200/.kibana/index-pattern/statistics
	curl -H 'Content-Type: application/json' -XPOST -d '{"doc":{"defaultIndex": "monitoring"}}' http://localhost:9200/.kibana/config/${kibana_config}/_update
	curl -s -XPOST localhost:9200/_bulk --data-binary "@/opt/datafari/elk/save/datafari-bulk-kibana.json"
	sudo -E su datafari -p -c "kill $(cat $KIBANA_PID_FILE)"
	sudo -E su datafari -p -c "kill $(cat $ELASTICSEARCH_PID_FILE)"
	sudo -E su datafari -p -c "rm $KIBANA_PID_FILE"
	echo "ELK successfully configured"

	echo "Start postgres and cassandra and add ManifoldCF database"
	sudo su postgres -c "rm -rf ${DATAFARI_HOME}/pgsql/data"
	sudo su postgres -c "mkdir -m 700 ${DATAFARI_HOME}/pgsql/data"
	sudo su datafari -c "rm -rf ${DATAFARI_HOME}/cassandra/data"
	sudo su datafari -c "mkdir ${DATAFARI_HOME}/cassandra/data"
	sudo su datafari -c "rm -rf ${DATAFARI_HOME}/zookeeper/data"
        sudo su datafari -c "mkdir -m 700 ${DATAFARI_HOME}/zookeeper/data"
	CASSANDRA_INCLUDE=$CASSANDRA_ENV
	# Redirect stdout and stderr to log file to ease startup issues investigation
	sudo -E su datafari -p -c "$CASSANDRA_HOME/bin/cassandra -p $CASSANDRA_PID_FILE &>$DATAFARI_LOGS/cassandra-startup.log"
	# Note: Cassandra start command returns 0 even if something goes wrong at startup. 
	# This is why hereafter we check pid and we see if the Cassandra ports are open.

	# Get the process ID assigned to Cassandra
	pid=$(head -n 1 $CASSANDRA_PID_FILE)

	# Check if Cassandra process is running
	if ps -p $pid > /dev/null 
	then
		echo "Cassandra process running with PID ${pid} --- OK"
	else
		echo "/!\ ERROR: Cassandra process is not running."
	fi
	
	sudo su postgres -c "${DATAFARI_HOME}/pgsql/bin/initdb -U postgres -A password --pwfile=${DATAFARI_HOME}/pgsql/pwd.conf -E utf8 -D ${DATAFARI_HOME}/pgsql/data"
	sudo su postgres -c "cp ${DATAFARI_HOME}/pgsql/postgresql.conf.save ${DATAFARI_HOME}/pgsql/data/postgresql.conf"
	sudo LD_LIBRARY_PATH=${DATAFARI_HOME}/pgsql/lib su postgres -p -c "${DATAFARI_HOME}/pgsql/bin/pg_ctl -D ${DATAFARI_HOME}/pgsql/data -l ${DATAFARI_LOGS}/pgsql.log start"
	cd ${MCF_HOME}
	sudo -E su datafari -p -c "bash initialize.sh"
	
	echo "Checking if Cassandra is up and running ..."
	# Try to connect on Cassandra's JMX port 7199
	nc -z localhost 7199 
	nc_return=$?

	# Try to connect on Cassandra's CQLSH port 9042
	nc -z localhost 9042
	nc_return2=$? 
	
	cassandra_status=$((nc_return+nc_return2))

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
		nc_return2=$? 
	
		cassandra_status=$((nc_return+nc_return2))

		((retries++))
    	done

	if [ $cassandra_status -ne 0 ]; then
		echo "/!\ ERROR: Cassandra startup has ended with errors; please check log file ${DATAFARI_LOGS}/cassandra-startup.log"
	else
		
        echo "Cassandra startup completed successfully --- OK"
		sudo -E su datafari -p -c "$CASSANDRA_HOME/bin/cqlsh -f $DATAFARI_HOME/bin/common/config/cassandra/tables"
	fi
fi


echo "Start zookeeper"
cd "${DATAFARI_HOME}/zookeeper/bin"
sudo -E su datafari -p -c "bash zkServer.sh start"

echo "Start Tomcat"
cd $TOMCAT_HOME/bin
sudo -E su datafari -p -c "bash startup.sh"

if  [[ "$STATE" = *installed* ]];
then

	echo "Uploading configuration to zookeeper"
	"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/FileShare/conf" -confname FileShare
	"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/Statistics/conf" -confname Statistics
	"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/Promolink/conf" -confname Promolink
    	cd "${DATAFARI_HOME}/bin/common"
	echo "Uploading MCF configuration"
	sudo -E su datafari -p -c "${JAVA_HOME}/bin/java -Dorg.apache.manifoldcf.configfile=${MCF_HOME}/properties.xml -cp ./*:${MCF_HOME}/lib/mcf-core.jar:${MCF_HOME}/lib/* com.francelabs.manifoldcf.configuration.script.BackupManifoldCFConnectorsScript RESTORE config/manifoldcf/init"
	sudo su datafari -c "sed -i 's/\(STATE *= *\).*/\1initialized/' $INIT_STATE_FILE"

else
	sudo LD_LIBRARY_PATH=${DATAFARI_HOME}/pgsql/lib su postgres -p -c "${DATAFARI_HOME}/pgsql/bin/pg_ctl -D ${DATAFARI_HOME}/pgsql/data -l ${DATAFARI_LOGS}/pgsql.log start"
	sudo -E su datafari -p -c "CASSANDRA_INCLUDE=$CASSANDRA_ENV $CASSANDRA_HOME/bin/cassandra -p $CASSANDRA_PID_FILE 1>/dev/null"
fi

cd $MCF_HOME/../bin

if  [[ "${OCR,,}" = *true* ]];
then
	echo "Using Tesseract OCR..."
	cp -f ${MCF_HOME}/tika-config.jar ${MCF_HOME}/connector-lib/	
else
	rm -f ${MCF_HOME}/connector-lib/tika-config.jar
	
fi
sudo -E su datafari -p -c "export PATH=$PATH && bash mcf_crawler_agent.sh start"
sudo -E su datafari -p -c "SOLR_INCLUDE=$SOLR_ENV $SOLR_INSTALL_DIR/bin/solr start"


if  [[ "$STATE" = *installed* ]];
then
	curl "http://localhost:8983/solr/admin/collections?action=CREATE&name=FileShare&numShards=1&replicationFactor=1&property.lib.path=/opt/datafari/solr/solrcloud/FileShare/"
	curl "http://localhost:8983/solr/admin/collections?action=CREATE&name=Statistics&numShards=1&replicationFactor=1"
	curl "http://localhost:8983/solr/admin/collections?action=CREATE&name=Promolink&numShards=1&replicationFactor=1"
fi



