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

version=$(python -V 2>&1 | grep -Po '(?<=Python )(.+)')
if [[ -z "$version" ]]
then

echo "No Python detected! Please install Python 2.7.x"
exit 1
else

case "$(python --version 2>&1)" in
    *" 2.7"*)
        echo "Compatible Python version detected"
        ;;
    *)
        echo "Wrong Python version! Please install Python 2.7.X"
        exit 1
        ;;
esac

fi

if type -p java; then
    echo found java executable in PATH
    _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    echo found java executable in JAVA_HOME
    _java="$JAVA_HOME/bin/java"
else
    echo "no Java detected. Please install Java. Program will exit."
    exit
fi

if [[ "$_java" ]]; then
    version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo version "$version"
    if [[ "$version" > "1.8" ]]; then
        echo Java version detected is OK

    else
        echo Java version is not >=1.8. Please install at least Java 8. Program will exit
        exit
    fi
fi

if [ -e "$JAVA_HOME"/bin/java ]; then
	echo "JAVA HOME is correctly set"
else
	echo "Environment variable JAVA_HOME is not properly set." 1>&2
	exit 1
fi


if  [[ "$STATE" = *installed* ]];
then
	# Configure ELK
	echo "Configure ELK"
	cd $ELASTICSEARCH_HOME/bin
	run_as ${DATAFARI_USER} "bash elasticsearch -p $ELASTICSEARCH_PID_FILE" &
	
	#Test if Elasticsearch is up, if not then exit
	waitElasticsearch
	
	#Sleep till Elasticsearch finishes its configuration
	sleep 5
	
	run_as ${DATAFARI_USER} "sed -i '/pid\.file/c\pid.file: ${KIBANA_PID_FILE}' $KIBANA_HOME/config/kibana.yml"
	cd $KIBANA_HOME/bin
	run_as ${DATAFARI_USER} "bash kibana" &
	
	#Test if Kibana is up, if not then exit
	waitKibana
	
	#Sleep till Kibana finishes its configuration
	sleep 5
		
	kibana_config=$(curl -s http://localhost:9200/.kibana/config/_search | jq -r '.hits.hits | .[0] | ._id')
	curl -H 'Content-Type: application/json' -XPUT -d @${LOGSTASH_HOME}/templates/datafari-monitoring-template.json http://localhost:9200/_template/datafari-monitoring
	curl -H 'Content-Type: application/json' -XPUT -d @${LOGSTASH_HOME}/templates/datafari-statistic-template.json http://localhost:9200/_template/datafari-statistics
	curl -H 'Content-Type: application/json' -XPUT -d @${ELK_HOME}/save/index-pattern-kibana-monitoring.json http://localhost:9200/.kibana/index-pattern/monitoring
	curl -H 'Content-Type: application/json' -XPUT -d @${ELK_HOME}/save/index-pattern-kibana-statistics.json http://localhost:9200/.kibana/index-pattern/statistics
	curl -H 'Content-Type: application/json' -XPOST -d '{"doc":{"defaultIndex": "monitoring"}}' http://localhost:9200/.kibana/config/${kibana_config}/_update
	curl -s -XPOST localhost:9200/_bulk --data-binary "@${ELK_HOME}/save/datafari-bulk-kibana.json"
	run_as ${DATAFARI_USER} "kill $(cat $KIBANA_PID_FILE)"
	run_as ${DATAFARI_USER} "kill $(cat $ELASTICSEARCH_PID_FILE)"
	run_as ${DATAFARI_USER} "rm $KIBANA_PID_FILE"
	echo "ELK successfully configured"

	echo "Start postgres and cassandra and add ManifoldCF database"
	sudo su postgres -c "rm -rf ${DATAFARI_HOME}/pgsql/data"
	sudo su postgres -c "mkdir -m 700 ${DATAFARI_HOME}/pgsql/data"
	run_as ${DATAFARI_USER} "rm -rf ${DATAFARI_HOME}/cassandra/data"
	run_as ${DATAFARI_USER} "mkdir ${DATAFARI_HOME}/cassandra/data"
	run_as ${DATAFARI_USER} "rm -rf ${DATAFARI_HOME}/zookeeper/data"
  run_as ${DATAFARI_USER} "mkdir -m 700 ${DATAFARI_HOME}/zookeeper/data"
	CASSANDRA_INCLUDE=$CASSANDRA_ENV
	# Redirect stdout and stderr to log file to ease startup issues investigation
	run_as ${DATAFARI_USER} "$CASSANDRA_HOME/bin/cassandra -p $CASSANDRA_PID_FILE &>$DATAFARI_LOGS/cassandra-startup.log"
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
	
	echo "Checking if Cassandra is up and running ..."
	# Try to connect on Cassandra's JMX port 7199 and CQLSH port 9042
  cassandra_status=0
	retries=1

	exec 6<>/dev/tcp/127.0.0.1/7199 || cassandra_status=1
	exec 6>&- # close output connection
	exec 6<&- # close input connection

	exec 6<>/dev/tcp/127.0.0.1/9042 || cassandra_status=1
	exec 6>&- # close output connection
	exec 6<&- # close input connection
  while (( retries < 10 && cassandra_status != 0 )); do
    echo "Cassandra doesn't reply to requests on ports 7199 and/or 9042. Sleeping for a while and trying again... retry ${retries}"
    
    cassandra_status=0
    
    # Sleep for a while
    sleep 5s
                
    exec 6<>/dev/tcp/127.0.0.1/7199 || cassandra_status=1
		exec 6>&- # close output connection
		exec 6<&- # close input connection

		exec 6<>/dev/tcp/127.0.0.1/9042 || cassandra_status=1
		exec 6>&- # close output connection
		exec 6<&- # close input connection

    ((retries++))
  done

	if [ $cassandra_status -ne 0 ]; then
		echo "/!\ ERROR: Cassandra startup has ended with errors; please check log file ${DATAFARI_LOGS}/cassandra-startup.log"
	else
		
        echo "Cassandra startup completed successfully --- OK"
		run_as ${DATAFARI_USER} "$CASSANDRA_HOME/bin/cqlsh -f $DATAFARI_HOME/bin/common/config/cassandra/tables"
	fi
fi


echo "Start zookeeper"
cd "${DATAFARI_HOME}/zookeeper/bin"
run_as ${DATAFARI_USER} "bash zkServer.sh start"

if  [[ "$STATE" = *installed* ]];
then
	cd ${MCF_HOME}
	echo "Init ZK sync for MCF"
	run_as ${DATAFARI_USER} "bash setglobalproperties.sh & sleep 3"
	run_as ${DATAFARI_USER} "bash initialize.sh"
	echo "Uploading configuration to zookeeper"
	"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/FileShare/conf" -confname FileShare
	"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/Statistics/conf" -confname Statistics
	"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/Promolink/conf" -confname Promolink
  cd "${DATAFARI_HOME}/bin/common"
	echo "Uploading MCF configuration - waiting up to 2 minutes"
	run_as ${DATAFARI_USER} "nohup ${JAVA_HOME}/bin/java -Dorg.apache.manifoldcf.configfile=${MCF_HOME}/properties.xml -cp ./*:${MCF_HOME}/lib/mcf-core.jar:${MCF_HOME}/lib/* com.francelabs.manifoldcf.configuration.script.BackupManifoldCFConnectorsScript RESTORE config/manifoldcf/init 2>/dev/null &"
	pid_mcf_upload=$(pgrep -f "com.francelabs.manifoldcf.configuration.script.BackupManifoldCFConnectorsScript" )
	spin='-\|/'
	i=0
	while kill -0 $pid_mcf_upload 2>/dev/null
		do
  			i=$(( (i+1) %4 ))
  			printf "\r${spin:$i:1}"
  			sleep .2
			done
	echo "end uploading MCF conf"	
	
	run_as ${DATAFARI_USER} "sed -i 's/\(STATE *= *\).*/\1initialized/' $INIT_STATE_FILE"

else
	sudo LD_LIBRARY_PATH=${DATAFARI_HOME}/pgsql/lib su postgres -p -c "${DATAFARI_HOME}/pgsql/bin/pg_ctl -D ${DATAFARI_HOME}/pgsql/data -l ${DATAFARI_LOGS}/pgsql.log start"
	run_as ${DATAFARI_USER} "CASSANDRA_INCLUDE=$CASSANDRA_ENV $CASSANDRA_HOME/bin/cassandra -p $CASSANDRA_PID_FILE 1>/dev/null"
	waitCassandra
fi

cd $MCF_HOME/../bin

run_as ${DATAFARI_USER} "export PATH=$PATH && bash mcf_crawler_agent.sh start"
run_as ${DATAFARI_USER} "SOLR_INCLUDE=$SOLR_ENV $SOLR_INSTALL_DIR/bin/solr start"


if  [[ "$STATE" = *installed* ]];
then
	curl "http://localhost:8983/solr/admin/collections?action=CREATE&name=FileShare&numShards=1&replicationFactor=1&property.lib.path=${SOLR_INSTALL_DIR}/solrcloud/FileShare/"
	curl "http://localhost:8983/solr/admin/collections?action=CREATE&name=Statistics&numShards=1&replicationFactor=1"
	curl "http://localhost:8983/solr/admin/collections?action=CREATE&name=Promolink&numShards=1&replicationFactor=1"
fi

echo "Start Tomcat"
cd $TOMCAT_HOME/bin
run_as ${DATAFARI_USER} "bash startup.sh"



