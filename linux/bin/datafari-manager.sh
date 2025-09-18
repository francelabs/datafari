#!/bin/bash -e
#
#
# Datafari Manager script
#
#

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/set-datafari-env.sh"
source "${DIR}/utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE

NUMSHARDS="`echo ${SOLRNUMSHARDS} | tr -d '\r'`"

is_running() {
  local pidFile=$1
  if ! [ -f $pidFile ]; then
    return 1
  fi
  local pid
  pid=$(cat $pidFile)
  if ! ps -p $pid 1>/dev/null 2>&1; then
    echo "Warn: a PID file was detected, removing it."
    rm -f $pidFile
    return 1
  fi
  return 0
}

forceStopIfNecessary(){
  local pidFile=$1
  if ! [ -f $pidFile ]; then
    return 0
  fi
  local pid
  pid=$(cat $pidFile)
  kill $pid
  waitpid $pid 30 .
  if [ $? -ne 0 ]; then
    echo "Warn: failed to stop $2 in 30 seconds, sending SIGKILL"
    kill -9 $pid
    sleep 1
  fi
  echo "stopped"
  rm -f $pidFile
}

init_postgres_repertories()
{
  rm -rf ${DATAFARI_HOME}/pgsql/data
  mkdir -m 700 ${DATAFARI_HOME}/pgsql/data
}


init_zookeeper_repertory()
{
  rm -rf ${DATAFARI_HOME}/zookeeper/data
  mkdir -m 700 ${DATAFARI_HOME}/zookeeper/data
}

init_zookeeper_mcf_repertory()
{
  rm -rf ${DATAFARI_HOME}/zookeeper-mcf/data
  mkdir -m 700 ${DATAFARI_HOME}/zookeeper-mcf/data
}

init_postgres()
{
  ${DATAFARI_HOME}/pgsql/bin/initdb -U postgres -A password --pwfile=${DATAFARI_HOME}/pgsql/pwd.conf -E utf8 -D ${DATAFARI_HOME}/pgsql/data
  cp ${DATAFARI_HOME}/pgsql/postgresql.conf.save ${DATAFARI_HOME}/pgsql/data/postgresql.conf
  
  
}

init_postgres_datafariwebapp() {
  echo "wait for Postgresql server to be started"
  PGPASSWORD="$TEMPPGSQLPASSWORD" ${DATAFARI_HOME}/pgsql/bin/createdb -h $POSTGRESQL_HOSTNAME -p $POSTGRESQL_PORT -U $POSTGRESQL_USERNAME $POSTGRESQL_DATABASE_DATAFARIWEBAPP
  PGPASSWORD="$TEMPPGSQLPASSWORD"  ${DATAFARI_HOME}/pgsql/bin/psql -h $POSTGRESQL_HOSTNAME -p $POSTGRESQL_PORT -U $POSTGRESQL_USERNAME -d $POSTGRESQL_DATABASE_DATAFARIWEBAPP -f $DATAFARI_HOME/bin/common/config/datafari/tables.sql
}


start_postgres()
{
  ${DATAFARI_HOME}/pgsql/bin/pg_ctl -D ${DATAFARI_HOME}/pgsql/data -l ${DATAFARI_LOGS}/pgsql.log start
}

stop_postgres()
{
  echo "Stopping Postgres..."
  ${DATAFARI_HOME}/pgsql/bin/pg_ctl -D ${DATAFARI_HOME}/pgsql/data -l ${DATAFARI_HOME}/logs/pgsql.log stop
  forceStopIfNecessary $POSTGRES_PID_FILE Postgres
}

start_apache()
{
  if [ -d /etc/apache2 ]; then
    sudo /etc/init.d/apache2 start
    sudo /etc/init.d/apache2 reload
  elif [ -d /etc/httpd ]; then
    sudo apachectl start
  fi
    
}

stop_apache()
{
  echo "Stopping Apache..."
  if [ -d /etc/apache2 ]; then
   sudo /etc/init.d/apache2 stop
  elif [ -d /etc/httpd ]; then
    sudo apachectl stop
  fi
  forceStopIfNecessary $APACHE_PID_FILE Apache
}


start_zookeeper()
{
  echo "Start zookeeper"
  cd "${DATAFARI_HOME}/zookeeper/bin"
  bash zkServer.sh start
}

stop_zookeeper()
{
  bash $ZK_HOME/bin/zkServer.sh stop
  forceStopIfNecessary $ZK_PID_FILE Zookeeper
}

init_zk()
{
  echo "Uploading configuration to zookeeper"
  #"${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -zkhost localhost:2181 -cmd clusterprop -name urlScheme -val https
  "${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/FileShare/conf" -confname FileShare
  "${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/Statistics/conf" -confname Statistics
  "${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/Promolink/conf" -confname Promolink
  "${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/Access/conf" -confname Access
  "${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/Monitoring/conf" -confname Monitoring
  "${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/Duplicates/conf" -confname Duplicates
  "${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/GenericAnnotator/conf" -confname GenericAnnotator
  "${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/VectorMain/conf" -confname VectorMain
  
  @ZK-INIT@
}

start_zookeeper_mcf()
{
  echo "Start zookeeper MCF"
  cd "${DATAFARI_HOME}/zookeeper-mcf/bin"
  bash zkServer.sh start
}

stop_zookeeper_mcf()
{
  echo "Stopping MCF Zookeeper"
  bash $DATAFARI_HOME/zookeeper-mcf/bin/zkServer.sh stop
  forceStopIfNecessary $ZK_MCF_PID_FILE MCF-Zookeeper
}

init_zk_mcf()
{
  cd ${MCF_HOME}
  echo "Init ZK sync for MCF"
  bash setglobalproperties.sh & sleep 3
  bash initialize.sh
}

start_mcf_crawler_agent()
{
  cd $MCF_HOME/../bin
  bash mcf_crawler_agent.sh start
}

stop_mcf_crawler_agent()
{
  cd $MCF_HOME/../bin
  bash mcf_crawler_agent.sh stop
  forceStopIfNecessary $MCF_PID_FILE McfCrawlerAgent
}

start_tomcat()
{
  echo "Start Tomcat"
  cd $TOMCAT_HOME/bin
  bash startup.sh
}

stop_tomcat()
{
  echo "Stopping Tomcat..."
  cd $TOMCAT_HOME/bin
  bash shutdown.sh 30
  forceStopIfNecessary $CATALINA_PID Tomcat
}

start_tomcat_mcf()
{
  echo "Start Tomcat-MCF"
  cd $TOMCAT_MCF_HOME/bin
  bash startup.sh
}

stop_tomcat_mcf()
{
  echo "Stopping Tomcat..."
  cd $TOMCAT_MCF_HOME/bin
  bash shutdown.sh 30
  forceStopIfNecessary $CATALINA_MCF_PID Tomcat
}

init_mcf()
{
  echo "Uploading MCF configuration"
  cd "${DATAFARI_HOME}/bin/common"
  nohup "${JAVA_HOME}/bin/java" -Dlog4j.configurationFile=$DATAFARI_HOME/bin/mcf.scripts.logging.xml -Dlogback.configurationFile=${DATAFARI_HOME}/zookeeper-mcf/conf/logback.xml -Djavax.net.ssl.trustStore=${DATAFARI_HOME}/ssl-keystore/datafari-truststore.p12 -Djavax.net.ssl.trustStorePassword=DataFariAdmin -Dorg.apache.manifoldcf.configfile=${MCF_HOME}/properties.xml -cp ./*:${MCF_HOME}/lib/mcf-core.jar:${MCF_HOME}/lib/* com.francelabs.manifoldcf.configuration.script.BackupManifoldCFConnectorsScript RESTORE config/manifoldcf/init 2>/dev/null &
  pid_mcf_upload=$!
  spin='-\|/'
  i=0
  while kill -0 $pid_mcf_upload 2>/dev/null
  do
    i=$(( (i+1) %4 ))
    printf "\r${spin:$i:1}"
    sleep .2
  done
  sleep 2
  echo "end uploading MCF conf"
}

start_solr()
{
  $SOLR_INSTALL_DIR/bin/solr start
}

init_solr()
{
  find $SOLR_INSTALL_DIR/solr_home -maxdepth 1 -mindepth 1 -type d -exec rm -rf '{}' \;
 
  
  curl -XGET "http://localhost:8983/solr/admin/collections?action=CREATE&name=@MAINCOLLECTION@&collection.configName=@MAINCOLLECTION@&numShards=${NUMSHARDS}&maxShardsPerNode=${NUMSHARDS}&replicationFactor=1&property.lib.path=${SOLR_INSTALL_DIR}/solrcloud/FileShare/"
  curl -XGET "http://localhost:8983/solr/admin/collections?action=CREATE&name=Statistics&collection.configName=Statistics&numShards=1&maxShardsPerNode=1&replicationFactor=1"
  curl -XGET "http://localhost:8983/solr/admin/collections?action=CREATE&name=Promolink&collection.configName=Promolink&numShards=1&maxShardsPerNode=1&replicationFactor=1"
  curl -XGET "http://localhost:8983/solr/admin/collections?action=CREATE&name=Access&collection.configName=Access&numShards=1&maxShardsPerNode=1&replicationFactor=1&property.lib.path=${SOLR_INSTALL_DIR}/solrcloud/Access/"
  curl -XGET "http://localhost:8983/solr/admin/collections?action=CREATE&name=Monitoring&collection.configName=Monitoring&numShards=1&maxShardsPerNode=1&replicationFactor=1"
  curl -XGET "http://localhost:8983/solr/admin/collections?action=CREATE&name=Duplicates&collection.configName=Duplicates&numShards=1&maxShardsPerNode=1&replicationFactor=1&property.lib.path=${SOLR_INSTALL_DIR}/solrcloud/Duplicates/"
  curl -XGET "http://localhost:8983/solr/admin/collections?action=CREATE&name=VectorMain&collection.configName=VectorMain&numShards=1&maxShardsPerNode=1&replicationFactor=1&property.lib.path=${SOLR_INSTALL_DIR}/solrcloud/VectorMain/"

  curl -XPOST http://localhost:8983/solr/@MAINCOLLECTION@/config/params -H 'Content-type:application/json'  -d '{"set":{"mySearch":{"qf":"exactContent^500 embedded_content^500 exactTitle^500 title_fr^50 title_en^50 title_de^50 title_es^50 content_fr^10 content_en^10 content_de^10 content_es^10 source^20 id^3 url_search^3","pf":"exactContent^5000 embedded_content^5000 exactTitle^5000 title_en^500 title_fr^500 title_de^500 title_es^500 content_fr^100 content_en^100 content_de^100 content_es^100 url_search^30","hl.maxAnalyzedChars":51200}}}'
  curl -XPOST http://localhost:8983/solr/VectorMain/config/params -H 'Content-type:application/json'  -d '{"set":{"mySearch":{"qf":"exactContent^500 embedded_content^500 exactTitle^500 title_fr^50 title_en^50 title_de^50 title_es^50 content_fr^10 content_en^10 content_de^10 content_es^10 source^20 id^3 url_search^3","pf":"exactContent^5000 embedded_content^5000 exactTitle^5000 title_en^500 title_fr^500 title_de^500 title_es^500 content_fr^100 content_en^100 content_de^100 content_es^100 url_search^30","hl.maxAnalyzedChars":51200}}}'
  curl -XPOST -H 'Content-type:application/json' -d '{"set-user-property": {"autocomplete.threshold": "0.005"}}' http://localhost:8983/solr/@MAINCOLLECTION@/config
  curl -XPOST -H 'Content-type:application/json' -d '{"set-user-property": {"clustering.enabled": "false"}}' http://localhost:8983/solr/@MAINCOLLECTION@/config
  
  curl -XPOST -H 'Content-type:application/json' -d '{"set-user-property": {"duplicates.hash.fields": "content"}}' http://localhost:8983/solr/Duplicates/config
  curl -XPOST -H 'Content-type:application/json' -d '{"set-user-property": {"duplicates.quant.rate": "0.1"}}' http://localhost:8983/solr/Duplicates/config 

  curl -XPOST -H 'Content-type:application/json' -d '{"set-user-property": {"vector.collection": "VectorMain"}}' http://localhost:8983/solr/FileShare/config
  curl -XPOST -H 'Content-type:application/json' -d '{"set-user-property": {"vector.host": "localhost:2181"}}' http://localhost:8983/solr/FileShare/config
  curl -XPOST -H 'Content-type:application/json' -d '{"set-user-property": {"vector.chunksize": "300"}}' http://localhost:8983/solr/FileShare/config
  curl -XPOST -H 'Content-type:application/json' -d '{"set-user-property": {"vector.maxoverlap": "0"}}' http://localhost:8983/solr/FileShare/config

  collections_autocommit=("Access" "Duplicates" "@MAINCOLLECTION@" "Monitoring" "Promolink" "Statistics" "VectorMain")
  for index in "${collections_autocommit[@]}"
  do
    curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-property": {"updateHandler.autoCommit.maxTime": "60000"}}' http://localhost:8983/solr/${index}/config
  done
 
  cd ${SOLR_INSTALL_DIR}/solrcloud/FileShare/conf/customs_schema && bash addCustomSchemaInfo.sh
  @SOLR-INIT@
}

init_solr_annotators() {
  curl -XGET "http://localhost:8983/solr/admin/collections?action=CREATE&name=OCR&collection.configName=GenericAnnotator&numShards=1&maxShardsPerNode=1&replicationFactor=1&property.lib.path=${SOLR_INSTALL_DIR}/solrcloud/GenericAnnotator/"
  curl -XGET "http://localhost:8983/solr/admin/collections?action=CREATE&name=Spacy&collection.configName=GenericAnnotator&numShards=1&maxShardsPerNode=1&replicationFactor=1&property.lib.path=${SOLR_INSTALL_DIR}/solrcloud/GenericAnnotator/"
  curl -XGET "http://localhost:8983/solr/admin/collections?action=CREATE&name=GenericAnnotator&collection.configName=GenericAnnotator&numShards=1&maxShardsPerNode=1&replicationFactor=1&property.lib.path=${SOLR_INSTALL_DIR}/solrcloud/GenericAnnotator/"

  collections_annotators_autocommit=("GenericAnnotator" "Spacy" "OCR")
  for index in "${collections_annotators_autocommit[@]}"
  do
    curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-property": {"updateHandler.autoCommit.maxTime": "60000"}}' http://localhost:8983/solr/${index}/config
  done
  
  @SOLR-INIT-ANNOTATORS@
}

init_solr_rag() {
  curl -XPUT 'http://localhost:8983/solr/VectorMain/schema/text-to-vector-model-store' --data-binary "@./zkUtils/openai.json" -H 'Content-type:application/json'
}


init_solr_analytics() {
  curl -XGET --insecure "http://localhost:8983/solr/admin/collections?action=CREATE&name=Statistics&collection.configName=Statistics&numShards=1&replicationFactor=1"
  curl -XGET --insecure "http://localhost:8983/solr/admin/collections?action=CREATE&name=Access&collection.configName=Access&numShards=1&maxShardsPerNode=1&replicationFactor=1&property.lib.path=${SOLR_INSTALL_DIR}/solrcloud/Access/"
  curl -XGET --insecure "http://localhost:8983/solr/admin/collections?action=CREATE&name=Monitoring&collection.configName=Monitoring&numShards=1&maxShardsPerNode=1&replicationFactor=1"
  curl -XGET --insecure "http://localhost:8983/solr/admin/collections?action=CREATE&name=Crawl&collection.configName=Crawl&numShards=1&maxShardsPerNode=1&replicationFactor=1"
  curl -XGET --insecure "http://localhost:8983/solr/admin/collections?action=CREATE&name=Logs&collection.configName=Logs&numShards=1&maxShardsPerNode=1&replicationFactor=1"
  collections_autocommit=("Statistics" "Access" "Monitoring" "Crawl" "Logs")
  for index in "${collections_autocommit[@]}"
  do
    curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-property": {"updateHandler.autoCommit.maxTime": "600000"}}' http://localhost:8983/solr/${index}/config
  done
}

stop_solr()
{
  $SOLR_INSTALL_DIR/bin/solr stop
  forceStopIfNecessary $SOLR_PID_FILE Solr
}

COMMAND=$1

case $COMMAND in
  init_postgres_repertories)
    init_postgres_repertories
  ;;
  init_zookeeper_repertory)
    init_zookeeper_repertory
  ;;
  init_zookeeper_mcf_repertory)
    init_zookeeper_mcf_repertory
  ;;
  init_postgres)
    init_postgres
  ;;
  init_postgres_datafariwebapp)
    init_postgres_datafariwebapp
  ;;
  start_postgres)
    start_postgres
  ;;
  stop_postgres)
    stop_postgres
  ;;
  start_zookeeper)
    start_zookeeper
  ;;
  stop_zookeeper)
    stop_zookeeper
  ;;
  init_zk)
    init_zk
  ;;
  start_zookeeper_mcf)
    start_zookeeper_mcf
  ;;
  stop_zookeeper_mcf)
    stop_zookeeper_mcf
  ;;
  init_zk_mcf)
    init_zk_mcf
  ;;
  start_mcf_crawler_agent)
    start_mcf_crawler_agent
  ;;
  stop_mcf_crawler_agent)
    stop_mcf_crawler_agent
  ;;
  start_tomcat)
    start_tomcat
  ;;
  stop_tomcat)
    stop_tomcat
  ;;
  start_tomcat_mcf)
    start_tomcat_mcf
  ;;
  stop_tomcat_mcf)
    stop_tomcat_mcf
  ;;    
  init_mcf)
    init_mcf
  ;;
  start_solr)
    start_solr
  ;;
  init_solr)
    init_solr
  ;;
  init_solr_annotators)
    init_solr_annotators
  ;;
  init_solr_analytics)
    init_solr_analytics
  ;;
  stop_solr)
    stop_solr
  ;;
  start_apache)
    start_apache
  ;;
  stop_apache)
    stop_apache
  ;;
  is_running)
    is_running $2
  ;;
  forceStopIfNecessary)
    forceStopIfNecessary $2 $3
  ;;
esac
