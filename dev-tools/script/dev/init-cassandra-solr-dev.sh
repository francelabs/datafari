#!/bin/bash -e
#
#
# Initialize ZK and Solr
#
#


DIR=../../../debian7/bin

source "set-datafari-env-devmode.sh"
source "${DIR}/utils.sh"

# Configuration of the Cassandra database and creation of the user admin
../../../cassandra/bin/cqlsh -f ../../../datafari-cassandra/conf/dev-env/tables
../../../cassandra/bin/cqlsh -f create-admin-dev.txt

echo "Uploading configuration to zookeeper"
sh "${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/FileShare/conf" -confname FileShare
sh "${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/Statistics/conf" -confname Statistics
sh "${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/Promolink/conf" -confname Promolink

echo "delete collections"
curl "http://localhost:8983/solr/admin/collections?action=DELETE&name=FileShare"
curl "http://localhost:8983/solr/admin/collections?action=DELETE&name=Statistics"
curl "http://localhost:8983/solr/admin/collections?action=DELETE&name=Promolink"
echo "create Solr collections"
curl "http://localhost:8983/solr/admin/collections?action=CREATE&name=FileShare&numShards=1&replicationFactor=1&property.lib.path=${DATAFARI_HOME}/solr/solrcloud/FileShare/"
curl "http://localhost:8983/solr/admin/collections?action=CREATE&name=Statistics&numShards=1&replicationFactor=1"
curl "http://localhost:8983/solr/admin/collections?action=CREATE&name=Promolink&numShards=1&replicationFactor=1"