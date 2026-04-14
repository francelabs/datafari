#!/bin/bash -e

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/../set-datafari-env.sh"
source "${DIR}/../utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE

nodes(){
        numrunningsolrnodes="`curl --silent 'http://localhost:8080/datafari-solr/zookeeper?detail=true&path=/live_nodes' | ${DATAFARI_HOME}/command/jq .znode.prop.children_count`"
}

NUMSHARDS="$(echo ${NUMSHARDS} | tr -d '\r')"

SOLR_ZK_CLI="$DATAFARI_HOME/solr/bin/solr"

echo "Uploading configuration to zookeeper"

# Start SSL configuration
"${DATAFARI_HOME}/solr/bin/solr" cluster --property urlScheme --value https --zk-host @SOLRHOSTS@
# Upload configs
"$SOLR_ZK_CLI" zk upconfig -z @SOLRHOSTS@ -d "${DATAFARI_HOME}/solr/solrcloud/Duplicates/conf" -n Duplicates
"$SOLR_ZK_CLI" zk upconfig -z @SOLRHOSTS@ -d "${DATAFARI_HOME}/solr/solrcloud/Entities/conf" -n Entities
"$SOLR_ZK_CLI" zk upconfig -z @SOLRHOSTS@ -d "${DATAFARI_HOME}/solr/solrcloud/FileShare/conf" -n Init
"$SOLR_ZK_CLI" zk upconfig -z @SOLRHOSTS@ -d "${DATAFARI_HOME}/solr/solrcloud/Statistics/conf" -n Statistics
"$SOLR_ZK_CLI" zk upconfig -z @SOLRHOSTS@ -d "${DATAFARI_HOME}/solr/solrcloud/Promolink/conf" -n Promolink
"$SOLR_ZK_CLI" zk upconfig -z @SOLRHOSTS@ -d "${DATAFARI_HOME}/solr/solrcloud/Access/conf" -n Access
"$SOLR_ZK_CLI" zk upconfig -z @SOLRHOSTS@ -d "${DATAFARI_HOME}/solr/solrcloud/Monitoring/conf" -n Monitoring
"$SOLR_ZK_CLI" zk upconfig -z @SOLRHOSTS@ -d "${DATAFARI_HOME}/solr/solrcloud/Crawl/conf" -n Crawl
"$SOLR_ZK_CLI" zk upconfig -z @SOLRHOSTS@ -d "${DATAFARI_HOME}/solr/solrcloud/Logs/conf" -n Logs
"$SOLR_ZK_CLI" zk upconfig -z @SOLRHOSTS@ -d "${DATAFARI_HOME}/solr/solrcloud/GenericAnnotator/conf" -n GenericAnnotator
"$SOLR_ZK_CLI" zk upconfig -z @SOLRHOSTS@ -d "${DATAFARI_HOME}/solr/solrcloud/VectorMain/conf" -n VectorMain
