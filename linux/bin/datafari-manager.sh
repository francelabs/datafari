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
        echo
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

init_cassandra_repertories()
{
  rm -rf ${DATAFARI_HOME}/cassandra/data
  mkdir ${DATAFARI_HOME}/cassandra/data
  mkdir ${DATAFARI_HOME}/cassandra/tmp
  chmod -R 775 ${DATAFARI_HOME}/cassandra/tmp
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


start_cassandra()
{
  # Redirect stdout and stderr to log file to ease startup issues investigation
  $CASSANDRA_HOME/bin/cassandra -p $CASSANDRA_PID_FILE &>$DATAFARI_LOGS/cassandra-startup.log
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
}

stop_cassandra()
{
  echo "Stopping Cassandra..."
  forceStopIfNecessary $CASSANDRA_PID_FILE Cassandra
}

init_cassandra()
{
  $CASSANDRA_HOME/bin/cqlsh -f $DATAFARI_HOME/bin/common/config/cassandra/tables
  $CASSANDRA_HOME/bin/cqlsh -f $DATAFARI_HOME/bin/common/config/cassandra/custom-tables
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
  "${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/FileShare/conf" -confname Init
  "${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/Statistics/conf" -confname Statistics
  "${DATAFARI_HOME}/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost:2181 -confdir "${DATAFARI_HOME}/solr/solrcloud/Promolink/conf" -confname Promolink
  
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

init_mcf_crawler_agent()
{
  cd $MCF_HOME
  echo "Init MCF crawler agent libs"
  LIBS=$(echo lib/*.jar | tr ' ' ':')
  #Remove log4j-1 lib
  LOG4J1=$(echo lib/log4j-1.2.*.jar):
  LIBS=$(echo "${LIBS/$LOG4J1/}")
  sed -i -e "s#@LIBS@#$LIBS#g" options.env.unix
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
  nohup "${JAVA_HOME}/bin/java" -Dlog4j.configurationFile=$DATAFARI_HOME/bin/mcf.scripts.logging.xml -Djavax.net.ssl.trustStore=${DATAFARI_HOME}/ssl-keystore/datafari-truststore.p12 -Djavax.net.ssl.trustStorePassword=DataFariAdmin -Dorg.apache.manifoldcf.configfile=${MCF_HOME}/properties.xml -cp ./*:${MCF_HOME}/lib/mcf-core.jar:${MCF_HOME}/lib/* com.francelabs.manifoldcf.configuration.script.BackupManifoldCFConnectorsScript RESTORE config/manifoldcf/init 2>/dev/null &
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
  
  #curl --insecure -XGET "https://localhost:8983/solr/admin/configs?action=CREATE&name=@MAINCOLLECTION@&baseConfigSet=Init&configSetProp.immutable=false"
  #curl --insecure -XGET "https://localhost:8983/solr/admin/collections?action=CREATE&name=@MAINCOLLECTION@&collection.configName=@MAINCOLLECTION@&numShards=${NUMSHARDS}&maxShardsPerNode=${NUMSHARDS}&replicationFactor=1&property.lib.path=${SOLR_INSTALL_DIR}/solrcloud/FileShare/"
  #curl --insecure -XGET "https://localhost:8983/solr/admin/collections?action=CREATE&name=Statistics&collection.configName=Statistics&numShards=1&maxShardsPerNode=1&replicationFactor=1"
  #curl --insecure -XGET "https://localhost:8983/solr/admin/collections?action=CREATE&name=Promolink&collection.configName=Promolink&numShards=1&maxShardsPerNode=1&replicationFactor=1"
  #curl --insecure -XPOST https://localhost:8983/solr/@MAINCOLLECTION@/config/params -H 'Content-type:application/json'  -d '{"set":{"mySearch":{"qf":"title_fr^50 title_en^50 title_de^50 title_es^50 content_fr^10 content_en^10 content_de^10 content_es^10 source^20 id^3 url_search^3","pf":"title_en^500 title_fr^500 title_de^500 title_es^500 content_fr^100 content_en^100 content_de^500 content_es^500 url_search^30","hl.maxAnalyzedChars":51200}}}'
  #curl --insecure -XPOST -H 'Content-type:application/json' -d '{"set-user-property": {"autocomplete.threshold": "0.005"}}' https://localhost:8983/solr/@MAINCOLLECTION@/config
  
  
  curl -XGET "http://localhost:8983/solr/admin/configs?action=CREATE&name=@MAINCOLLECTION@&baseConfigSet=Init&configSetProp.immutable=false"
  curl -XGET "http://localhost:8983/solr/admin/collections?action=CREATE&name=@MAINCOLLECTION@&collection.configName=@MAINCOLLECTION@&numShards=${NUMSHARDS}&maxShardsPerNode=${NUMSHARDS}&replicationFactor=1&property.lib.path=${SOLR_INSTALL_DIR}/solrcloud/FileShare/"
  curl -XGET "http://localhost:8983/solr/admin/collections?action=CREATE&name=Statistics&collection.configName=Statistics&numShards=1&maxShardsPerNode=1&replicationFactor=1"
  curl -XGET "http://localhost:8983/solr/admin/collections?action=CREATE&name=Promolink&collection.configName=Promolink&numShards=1&maxShardsPerNode=1&replicationFactor=1"
  curl -XPOST http://localhost:8983/solr/@MAINCOLLECTION@/config/params -H 'Content-type:application/json'  -d '{"set":{"mySearch":{"qf":"title_fr^50 title_en^50 title_de^50 title_es^50 content_fr^10 content_en^10 content_de^10 content_es^10 source^20 id^3 url_search^3","pf":"title_en^500 title_fr^500 title_de^500 title_es^500 content_fr^100 content_en^100 content_de^100 content_es^100 url_search^30","hl.maxAnalyzedChars":51200}}}'
  curl -XPOST -H 'Content-type:application/json' -d '{"set-user-property": {"autocomplete.threshold": "0.005"}}' http://localhost:8983/solr/@MAINCOLLECTION@/config
  curl -XPOST -H 'Content-type:application/json' -d '{"set-user-property": {"clustering.enabled": "false"}}' http://localhost:8983/solr/@MAINCOLLECTION@/config
  
  @SOLR-INIT@
}

stop_solr()
{
  $SOLR_INSTALL_DIR/bin/solr stop
  forceStopIfNecessary $SOLR_PID_FILE Solr
}

@VERSION-MANAGER@

COMMAND=$1

case $COMMAND in
    init_postgres_repertories)
        init_postgres_repertories
        ;;
    init_cassandra_repertories)
        init_cassandra_repertories
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
    start_postgres)
        start_postgres
        ;;
    stop_postgres)
        stop_postgres
        ;;
    start_cassandra)
        start_cassandra
        ;;
    stop_cassandra)
        stop_cassandra
        ;;
    init_cassandra)
        init_cassandra
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
    init_mcf_crawler_agent)
        init_mcf_crawler_agent
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
