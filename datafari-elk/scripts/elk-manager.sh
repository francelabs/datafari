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

init_jvm_options()
{
  sed -i -e "s~@ELK_LOGS@~$ELK_LOGS~g" $ELASTICSEARCH_HOME/config/jvm.options
  sed -i -e "s~@ELK_LOGS@~$ELK_LOGS~g" $LOGSTASH_HOME/config/jvm.options
}

init_elasticsearch()
{
  if ! grep -q 'network.host: 0.0.0.0' $ELASTICSEARCH_HOME/config/elasticsearch.yml; then
    echo "network.host: 0.0.0.0" >> $ELASTICSEARCH_HOME/config/elasticsearch.yml
    echo "discovery.type: single-node" >> $ELASTICSEARCH_HOME/config/elasticsearch.yml
  fi
  cd $ELASTICSEARCH_HOME
  echo "Starting Elasticsearch..."
  bash opendistro-tar-install.sh -p $ELASTICSEARCH_PID_FILE &
  echo "Elasticsearch started !"
  cd $DIR
  
}

init_logstash()
{
  echo "Initialize Logstash..."
  # Replace the default conf with the correct paths before starting logstash
  sed -i "/francelabs\/datafari-stats.log/c\  path => \"${DATAFARI_HOME}/logs/datafari-stats.log*\"" $LOGSTASH_HOME/logstash-datafari.conf
  sed -i "/francelabs\/datafari-monitoring.log/c\ path => \"${DATAFARI_HOME}/logs/datafari-monitoring.log*\"" $LOGSTASH_HOME/logstash-datafari.conf
  @ADDITIONAL_LOGSTASH_INIT@
  echo "Logstash initialized !"
}

init_kibana()
{
  echo "Initialize Kibana..."
  # Configure the right path for Kibana PID file
  sed -i "/pid\.file/c\pid.file: ${KIBANA_PID_FILE}" $KIBANA_HOME/config/kibana.yml
  echo "Kibana initialized !"
}

init_kibana_index()
{
  echo "Initialize Kibana index through it's save objects API..."
  curl -k -u admin:admin -H 'Content-Type: application/json' -XPUT -d @${LOGSTASH_HOME}/templates/datafari-monitoring-template.json https://localhost:9200/_index_template/datafari-monitoring
  curl -k -u admin:admin -H 'Content-Type: application/json' -XPUT -d @${LOGSTASH_HOME}/templates/datafari-statistic-template.json https://localhost:9200/_index_template/datafari-statistics
  @ADDITIONAL_TEMPLATES@
  curl -k -u searchadmin:admin -X POST https://localhost:5601/api/saved_objects/_import -H "kbn-xsrf: true" -H "securitytenant: searchexpert_tenant" --form file=@${ELK_HOME}/save/kibana-ce.ndjson
  @ADDITIONAL_IMPORTS@
  echo "Kibana index initialized !"
}

# Warning, Elasticsearch and Kibana must be running and initialized before using this method 
kibana_first_init()
{
  cpt=0
  pct=0
  until [ $cpt -gt 150 ]
  do
   modulo=$(expr $cpt % 2)
   if [[ "$modulo" ==  0 ]] && [[ "$cpt" -gt 0 ]]; then
     let pct+=1
   fi
   if [[ "$pct" -lt 10 ]]; then
    echo -ne 'Initializing Kibana [          ] ('$pct'%)\r'
   elif [[ "$pct" -ge 10 ]] && [[ "$pct" -lt 20 ]]; then
       echo -ne 'Initializing Kibana [#         ] ('$pct'%)\r'
   elif [[ "$pct" -ge 20 ]] && [[ "$pct" -lt 30 ]]; then
       echo -ne 'Initializing Kibana [##        ] ('$pct'%)\r'
   elif [[ "$pct" -ge 30 ]] && [[ "$pct" -lt 40 ]]; then
       echo -ne 'Initializing Kibana [###       ] ('$pct'%)\r'
   elif [[ "$pct" -ge 40 ]] && [[ "$pct" -lt 50 ]]; then
       echo -ne 'Initializing Kibana [####      ] ('$pct'%)\r'
   elif [[ "$pct" -ge 50 ]] && [[ "$pct" -lt 60 ]]; then
       echo -ne 'Initializing Kibana [#####     ] ('$pct'%)\r'
   elif [[ "$pct" -ge 60 ]] && [[ "$pct" -lt 70 ]]; then
       echo -ne 'Initializing Kibana [######    ] ('$pct'%)\r'
   elif [[ "$pct" -ge 70 ]] && [[ "$pct" -lt 80 ]]; then
       echo -ne 'Initializing Kibana [#######   ] ('$pct'%)\r'
   elif [[ "$pct" -ge 80 ]] && [[ "$pct" -lt 90 ]]; then
       echo -ne 'Initializing Kibana [########  ] ('$pct'%)\r'
   elif [[ "$pct" -ge 90 ]] && [[ "$pct" -lt 100 ]]; then
       echo -ne 'Initializing Kibana [######### ] ('$pct'%)\r'
   else
      echo -ne 'Initializing Kibana [##########] ('$pct'%)\r'
   fi
   
   if [[ "$pct" == 75 ]]; then
     init_kibana_index;
     echo -ne 'Initializing Kibana [##########] ('100'%)\r'
     
   fi
      
   sleep 1
   let cpt+=1
  done
}

start_es()
{
  if is_running $ELASTICSEARCH_PID_FILE; then
     echo "Elasticsearch seems to be already running with PID $(cat $ELASTICSEARCH_PID_FILE)"
  else
    cd $ELASTICSEARCH_HOME/bin
    echo "Starting Elasticsearch..."
    bash elasticsearch -p $ELASTICSEARCH_PID_FILE &
    waitElasticsearch;
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
    bash bin/logstash -f $LOGSTASH_HOME/logstash-datafari.conf >/dev/null 2>&1 &
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
    waitKibana;
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
  start_kibana;
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
  init_jvm_options;
  init_logstash;
  init_kibana;
  init_elasticsearch;
  waitElasticsearch;
  # Sleep few seconds until Elaticsearch is fully operational
  echo "Sleep 15 seconds until Elaticsearch is fully operational"
  sleep 15
  start_kibana;
  kibana_first_init;
  stop_kibana;
  stop_es;
  echo "ELK initialized !"
}

@ADDITIONAL_FUNCTIONS@

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
    @ADDITIONAL_COMMANDS@
esac




