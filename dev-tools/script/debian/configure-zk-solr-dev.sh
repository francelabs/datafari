#!/bin/bash -e
#
#
# Initialize ZK and Solr
#
#

DIR=../../../debian7/bin

source "set-datafari-env-devmode.sh"
source "${DIR}/utils.sh"


sed -i -e "s/@NODEHOST@/localhost/g" ${DATAFARI_HOME}/tomcat/conf/datafari.properties
sed -i -e "s/@NODEHOST@/localhost/g" ${DATAFARI_HOME}/solr/bin/solr.in.sh
sed -i -e "s/@SOLRHOSTS@/localhost:2181/g" ${DATAFARI_HOME}/solr/bin/solr.in.sh
sed -i -e "s/@SOLRHOSTS@/localhost:2181/g" ${DATAFARI_HOME}/tomcat/conf/datafari.properties


sed -i -e "s/@NUMSHARDS@/1/g" ${DATAFARI_HOME}/tomcat/conf/datafari.properties
sed -i -e "s/@NUMSHARDS@/1/g" ${DATAFARI_HOME}/bin/start-datafari.sh
sed -i -e "s/@ISMAINNODE@/true/g" ${DATAFARI_HOME}/tomcat/conf/datafari.properties
mkdir $DATAFARI_HOME/solr/solrcloud
mv ${DATAFARI_HOME}/solr/solr_home/FileShare ${DATAFARI_HOME}/solr/solrcloud
mv ${DATAFARI_HOME}/solr/solr_home/Statistics ${DATAFARI_HOME}/solr/solrcloud
mv ${DATAFARI_HOME}/solr/solr_home/Promolink ${DATAFARI_HOME}/solr/solrcloud

echo "Uploading configuration to zookeeper"
"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/FileShare/conf" -confname FileShare
"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/Statistics/conf" -confname Statistics
"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/Promolink/conf" -confname Promolink
sleep 10

echo "create Solr collections"
curl "http://localhost:8983/solr/admin/collections?action=CREATE&name=FileShare&numShards=1&replicationFactor=1&property.lib.path=${DATAFARI_HOME}/solr/solrcloud/FileShare/"
curl "http://localhost:8983/solr/admin/collections?action=CREATE&name=Statistics&numShards=1&replicationFactor=1"
curl "http://localhost:8983/solr/admin/collections?action=CREATE&name=Promolink&numShards=1&replicationFactor=1"