#!/bin/bash -e
#
#
# Startup script for Datafari
#
#

DIR=../../../debian7/bin
source "dev-datafari.properties"
source "set-datafari-env-devmode.sh"
source "${DIR}/utils.sh"






if is_running $SOLR_PID_FILE; then
   echo "Error : Solr seems to be running already with PID $(cat $SOLR_PID_FILE)"
   exit 1
fi

if is_running $CASSANDRA_PID_FILE; then
   echo "Error : Cassandra seems to be running already with PID $(cat $CASSANDRA_PID_FILE)"
   exit 1
fi

if is_running $ZK_PID_FILE; then
   echo "Error : Zookeeper seems to be already running with PID $(cat $ZK_PID_FILE)"
   exit 1
fi





if  [[ "$STATE" = *installed* ]];
then

# Configuration Solr and ZK
sed -i -e "s/@NODEHOST@/localhost/g" ${DATAFARI_HOME}/tomcat/conf/datafari.properties
sed -i -e "s/@NODEHOST@/localhost/g" ${DATAFARI_HOME}/solr/bin/solr.in.sh
sed -i -e "s/@SOLRHOSTS@/localhost:2181/g" ${DATAFARI_HOME}/solr/bin/solr.in.sh
sed -i -e "s/@SOLRHOSTS@/localhost:2181/g" ${DATAFARI_HOME}/tomcat/conf/datafari.properties
sed -i -e "s/@NUMSHARDS@/1/g" ${DATAFARI_HOME}/tomcat/conf/datafari.properties
sed -i -e "s/@ISMAINNODE@/true/g" ${DATAFARI_HOME}/tomcat/conf/datafari.properties
mkdir $DATAFARI_HOME/solr/solrcloud
chmod -R 777 ${DATAFARI_HOME}/solr/solrcloud
mv ${DATAFARI_HOME}/solr/solr_home/FileShare ${DATAFARI_HOME}/solr/solrcloud
mv ${DATAFARI_HOME}/solr/solr_home/Statistics ${DATAFARI_HOME}/solr/solrcloud
mv ${DATAFARI_HOME}/solr/solr_home/Promolink ${DATAFARI_HOME}/solr/solrcloud
chmod -R 777 ${DATAFARI_HOME}/pid


echo "First launch"
# Start ZK

ZK_INCLUDE=$ZK_ENV $ZK_HOME/bin/zkServer.sh start

echo "Uploading configuration to zookeeper"
sh "${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/FileShare/conf" -confname FileShare
sh "${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/Statistics/conf" -confname Statistics
sh "${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/Promolink/conf" -confname Promolink
echo "wait for 10 seconds"
sleep 10
SOLR_INCLUDE=$SOLR_ENV $SOLR_INSTALL_DIR/bin/solr start

echo "delete collections"
curl "http://localhost:8983/solr/admin/collections?action=DELETE&name=FileShare"
curl "http://localhost:8983/solr/admin/collections?action=DELETE&name=Statistics"
curl "http://localhost:8983/solr/admin/collections?action=DELETE&name=Promolink"
echo "create Solr collections"
curl "http://localhost:8983/solr/admin/collections?action=CREATE&name=FileShare&numShards=1&replicationFactor=1&property.lib.path=${DATAFARI_HOME}/solr/solrcloud/FileShare/"
curl "http://localhost:8983/solr/admin/collections?action=CREATE&name=Statistics&numShards=1&replicationFactor=1"
curl "http://localhost:8983/solr/admin/collections?action=CREATE&name=Promolink&numShards=1&replicationFactor=1"


CASSANDRA_INCLUDE=$CASSANDRA_ENV $CASSANDRA_HOME/bin/cassandra -p $CASSANDRA_PID_FILE

# Check if Cassandra process is running
	cassandra_process=$(ps -Alf | grep $pid | grep org.apache.cassandra.service.CassandraDaemon)

	if [ -z "$cassandra_process" ]; then
		echo "/!\ ERROR: Cassandra process is not running."
	else
		echo "Cassandra process running with PID ${pid} --- OK"
	fi
	
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
		echo "/!\ ERROR: Cassandra startup has ended with errors; "
		exit 
	else
		
        echo "Cassandra startup completed successfully --- OK"
        echo "Initialization of Cassandra"
		# Configuration of the Cassandra database and creation of the user admin
		../../../cassandra/bin/cqlsh -f ../../../datafari-cassandra/conf/dev-env/tables
		../../../cassandra/bin/cqlsh -f ../../../datafari-cassandra/conf/dev-env/create-admin-dev.txt
	fi
	
	
sed -i '' 's/\(STATE *= *\).*/\1initialized/' "dev-datafari.properties"
echo "Datafari initialized and running"


else


ZK_INCLUDE=$ZK_ENV $ZK_HOME/bin/zkServer.sh start
CASSANDRA_INCLUDE=$CASSANDRA_ENV $CASSANDRA_HOME/bin/cassandra -p $CASSANDRA_PID_FILE
SOLR_INCLUDE=$SOLR_ENV $SOLR_INSTALL_DIR/bin/solr start



fi