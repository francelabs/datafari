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

# Configuration of the Cassandra database and creation of the user admin
../../../cassandra/bin/cqlsh -f ../../../datafari-cassandra/conf/dev-env/tables
../../../cassandra/bin/cqlsh -f create-admin-dev.txt

sed -i '' 's/\(STATE *= *\).*/\1initialized/' "dev-datafari.properties"


else


ZK_INCLUDE=$ZK_ENV $ZK_HOME/bin/zkServer.sh start
CASSANDRA_INCLUDE=$CASSANDRA_ENV $CASSANDRA_HOME/bin/cassandra -p $CASSANDRA_PID_FILE
SOLR_INCLUDE=$SOLR_ENV $SOLR_INSTALL_DIR/bin/solr start



fi