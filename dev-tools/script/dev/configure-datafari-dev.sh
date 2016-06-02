#!/bin/bash -e
#
#
# Initialize Configuration files Solr and Datafari
#
#

DIR=../../../debian7/bin

source "set-datafari-env-devmode.sh"
source "${DIR}/utils.sh"



# Configuration Solr and ZK
sed -i -e "s/@NODEHOST@/localhost/g" ${DATAFARI_HOME}/tomcat/conf/datafari.properties
sed -i -e "s/@NODEHOST@/localhost/g" ${DATAFARI_HOME}/solr/bin/solr.in.sh
sed -i -e "s/@SOLRHOSTS@/localhost:2181/g" ${DATAFARI_HOME}/solr/bin/solr.in.sh
sed -i -e "s/@SOLRHOSTS@/localhost:2181/g" ${DATAFARI_HOME}/tomcat/conf/datafari.properties
sed -i -e "s/@NUMSHARDS@/1/g" ${DATAFARI_HOME}/tomcat/conf/datafari.properties
sed -i -e "s/@ISMAINNODE@/true/g" ${DATAFARI_HOME}/tomcat/conf/datafari.properties
mkdir $DATAFARI_HOME/solr/solrcloud
chmod -R 777 ${DATAFARI_HOME}/solrcloud
mv ${DATAFARI_HOME}/solr/solr_home/FileShare ${DATAFARI_HOME}/solr/solrcloud
mv ${DATAFARI_HOME}/solr/solr_home/Statistics ${DATAFARI_HOME}/solr/solrcloud
mv ${DATAFARI_HOME}/solr/solr_home/Promolink ${DATAFARI_HOME}/solr/solrcloud
chmod -R 777 ${DATAFARI_HOME}/pid
