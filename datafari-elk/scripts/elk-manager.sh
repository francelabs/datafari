#!/bin/bash
#
# ELK Manager 
#

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/set-elk-env.sh"
source "${DIR}/elk-utils.sh"

set_logs_paths()
{
  echo "Set logs paths..."
  sed -i "/path.logs/c\path.logs: ${ELK_LOGS}" $ELASTICSEARCH_HOME/config/elasticsearch.yml
  sed -i "/path.logs/c\path.logs: ${ELK_LOGS}" $LOGSTASH_HOME/config/logstash.yml
  sed -i "/logging.dest/c\logging.dest: ${ELK_LOGS}/kibana.log" $KIBANA_HOME/config/kibana.yml
  echo "Logs paths correctly set"
}

init_logstash()
{
  echo "Initialize Logstash..."
  # Replace the default conf with the correct paths before starting logstash
  sed -i "/francelabs\/datafari-crawl.log/c\  path => \"${DATAFARI_HOME}/logs/datafari-crawl.log\"" $LOGSTASH_HOME/logstash-datafari.conf
  sed -i "/francelabs\/datafari-stats.log/c\  path => \"${DATAFARI_HOME}/logs/datafari-stats.log\"" $LOGSTASH_HOME/logstash-datafari.conf
  sed -i "/francelabs\/datafari-monitoring.log/c\ path => \"${DATAFARI_HOME}/logs/datafari-monitoring.log\"" $LOGSTASH_HOME/logstash-datafari.conf
  sed -i "/francelabs\/datafari.log/c\  path => \"${DATAFARI_HOME}/logs/datafari.log\"" $LOGSTASH_HOME/logstash-datafari.conf
  sed -i "/francelabs\/manifoldcf-webservices.log/c\ path => \"${DATAFARI_HOME}/logs/manifoldcf-webservices.log\"" $LOGSTASH_HOME/logstash-datafari.conf
  sed -i "/francelabs\/tomcat.log/c\  path => \"${DATAFARI_HOME}/logs/tomcat.log*\"" $LOGSTASH_HOME/logstash-datafari.conf
  sed -i "/francelabs\/tomcat-mcf.log/c\  path => \"${DATAFARI_HOME}/logs/tomcat-mcf.log*\"" $LOGSTASH_HOME/logstash-datafari.conf
  sed -i "/francelabs\/manifoldcf.log/c\  path => \"${DATAFARI_HOME}/logs/manifoldcf.log*\"" $LOGSTASH_HOME/logstash-datafari.conf
  sed -i "/francelabs\/elasticsearch.log/c\ path => \"${DATAFARI_HOME}/elk/elasticsearch/logs/elasticsearch.log*\"" $LOGSTASH_HOME/logstash-datafari.conf
  sed -i "/francelabs\/solr.log/c\  path => \"${DATAFARI_HOME}/logs/solr.log*\"" $LOGSTASH_HOME/logstash-datafari.conf
  sed -i "/francelabs\/zookeeper.log/c\ path => \"${DATAFARI_HOME}/logs/zookeeper.log*\"" $LOGSTASH_HOME/logstash-datafari.conf
  echo "Logstash initialized !"
}

init_kibana()
{
  echo "Initialize Kibana..."
  # Configure the right path for Kibana PID file
  sed -i "/pid\.file/c\pid.file: ${KIBANA_PID_FILE}" $KIBANA_HOME/config/kibana.yml
  echo "Kibana initialized !"
}

# Warning, Elasticsearch and Kibana must be running before using this method 
init_kibana_index()
{
  echo "Initialize Kibana index into Elasticsearch..."
  kibana_config=$(curl -s http://localhost:9200/.kibana/config/_search | jq -r '.hits.hits | .[0] | ._id')
  curl -H 'Content-Type: application/json' -XPUT -d @${LOGSTASH_HOME}/templates/datafari-monitoring-template.json http://localhost:9200/_template/datafari-monitoring
  curl -H 'Content-Type: application/json' -XPUT -d @${LOGSTASH_HOME}/templates/datafari-statistic-template.json http://localhost:9200/_template/datafari-statistics
  curl -H 'Content-Type: application/json' -XPUT -d @${ELK_HOME}/save/index-pattern-kibana-monitoring.json http://localhost:9200/.kibana/index-pattern/monitoring
  curl -H 'Content-Type: application/json' -XPUT -d @${ELK_HOME}/save/index-pattern-kibana-statistics.json http://localhost:9200/.kibana/index-pattern/statistics
  curl -H 'Content-Type: application/json' -XPOST -d '{"doc":{"defaultIndex": "monitoring"}}' http://localhost:9200/.kibana/config/${kibana_config}/_update
  curl -s -XPOST localhost:9200/_bulk --data-binary "@${ELK_HOME}/save/datafari-bulk-kibana.json"
  echo "Kibana index initialized !"
}

start_es()
{
  if is_running $ELASTICSEARCH_PID_FILE; then
     echo "Elasticsearch seems to be already running with PID $(cat $ELASTICSEARCH_PID_FILE)"
  else
    cd $ELASTICSEARCH_HOME/bin
    echo "Starting Elasticsearch..."
    bash elasticsearch -p $ELASTICSEARCH_PID_FILE &
    echo "Elasticsearch started !"
    cd $DIR
  fi
}

stop_es()
{
  echo "Stopping Elasticsearch..."
  forceStopIfNecessary $ELASTICSEARCH_PID_FILE Elasticsearch
}

start_logstash()
{
  if is_running $LOGSTASH_PID_FILE; then
    echo "Error : Logstash seems to be already running with PID $(cat $LOGSTASH_PID_FILE)"
  else
    cd $LOGSTASH_HOME
    echo "Starting Logstash..."
    bash bin/logstash -f $LOGSTASH_HOME/logstash-datafari.conf &
    echo $! > $LOGSTASH_PID_FILE
    echo "Logstash started !"
    cd $DIR
  fi
}

stop_logstash()
{
  echo "Stopping Logstash..."
  forceStopIfNecessary $LOGSTASH_PID_FILE Logstash
}

start_kibana()
{
  if is_running $KIBANA_PID_FILE; then
    echo "Error : Kibana seems to be already running with PID $(cat $KIBANA_PID_FILE)"
  else
    cd $KIBANA_HOME/bin
    echo "Starting Kibana..." 
    bash kibana &
    echo "Kibana started !"
    cd $DIR
  fi
}

stop_kibana()
{
  echo "Stopping Kibana..."
  forceStopIfNecessary $KIBANA_PID_FILE Kibana
}

cmd_start() {
  start_es;
  waitElasticsearch;
  start_kibana;
  waitKibana;
  start_logstash;
}

cmd_stop() {
  stop_kibana;
  stop_logstash;
  stop_es;
}

cmd_status() {
  if is_running $ELASTICSEARCH_PID_FILE; then
      echo "Elasticsearch is running:"
      ps -o pid,cmd --width 5000 -p $(cat $ELASTICSEARCH_PID_FILE)
  else
      echo "Elasticsearch is not running."
  fi
  
  if is_running $LOGSTASH_PID_FILE; then
      echo "Logstash is running:"
      ps -o pid,cmd --width 5000 -p $(cat $LOGSTASH_PID_FILE)
  else
      echo "Logstash is not running."
  fi
  
  if is_running $KIBANA_PID_FILE; then
      echo "Kibana is running:"
      ps -o pid,cmd --width 5000 -p $(cat $KIBANA_PID_FILE)
  else
      echo "Kibana is not running."
  fi
}

init_elk()
{
  echo "Initializing ELK..."
  set_logs_paths;
  init_logstash;
  init_kibana;
  start_es;
  waitElasticsearch;
  # Sleep few seconds until Elaticsearch is fully operational
  sleep 5
  start_kibana;
  waitKibana;
  # Sleep few seconds until Kibana is fully operational
  sleep 5
  init_kibana_index;
  #Wait for low systems to complete Kibana config
  sleep 10
  stop_kibana;
  stop_es;
  echo "ELK initialized !"
}


COMMAND=$1

case $COMMAND in
    start)
        cmd_start
        ;;
    stop)
        cmd_stop
        ;;
    status)
        cmd_status
        ;;
    set_logs_paths)
        set_logs_paths
        ;;
    init_logstash)
        init_logstash
        ;;
    init_kibana)
        init_kibana
        ;;
    init_kibana_index)
        init_kibana_index
        ;;
    start_es)
        start_es
        ;;
    stop_es)
        stop_es
        ;;
    start_logstash)
        start_logstash
        ;;
    stop_logstash)
        stop_logstash
        ;;
    start_kibana)
        start_kibana
        ;;
    stop_kibana)
        stop_kibana
        ;;
    init_elk)
        init_elk
        ;;
esac




