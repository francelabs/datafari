#!/bin/bash -e
#
#
# Send configuration to Zookeeper
#
#

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/../set-datafari-env.sh"
source "${DIR}/../utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE

nodes(){
        numrunningsolrnodes="`curl --silent 'http://localhost:8080/datafari-solr/zookeeper?detail=true&path=/live_nodes' | ${DATAFARI_HOME}/command/jq .znode.prop.children_count`"
}

NUMSHARDS="`echo ${NUMSHARDS} | tr -d '\r'`"

echo "Uploading configuration to zookeeper"
# Start SSL configuration
"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -zkhost @SOLRHOSTS@ -cmd clusterprop -name urlScheme -val https
# End SSL configuration
"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost @SOLRHOSTS@ -confdir "${DATAFARI_HOME}/solr/solrcloud/Duplicates/conf" -confname Duplicates
"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost @SOLRHOSTS@ -confdir "${DATAFARI_HOME}/solr/solrcloud/Entities/conf" -confname Entities
"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost @SOLRHOSTS@ -confdir "${DATAFARI_HOME}/solr/solrcloud/FileShare/conf" -confname Init
"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost @SOLRHOSTS@ -confdir "${DATAFARI_HOME}/solr/solrcloud/Statistics/conf" -confname Statistics
"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost @SOLRHOSTS@ -confdir "${DATAFARI_HOME}/solr/solrcloud/Promolink/conf" -confname Promolink
"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost @SOLRHOSTS@ -confdir "${DATAFARI_HOME}/solr/solrcloud/Access/conf" -confname Access
"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost @SOLRHOSTS@ -confdir "${DATAFARI_HOME}/solr/solrcloud/Monitoring/conf" -confname Monitoring
"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost @SOLRHOSTS@ -confdir "${DATAFARI_HOME}/solr/solrcloud/Crawl/conf" -confname Crawl
"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost @SOLRHOSTS@ -confdir "${DATAFARI_HOME}/solr/solrcloud/Logs/conf" -confname Logs
"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost @SOLRHOSTS@ -confdir "${DATAFARI_HOME}/solr/solrcloud/Logs/conf" -confname GenericAnnotator