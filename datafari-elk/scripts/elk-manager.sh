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
  sed -i "/path.logs/c\path.logs: ${ELK_LOGS}" $LOGSTASH_HOME/config/logstash.yml
  echo "Logs paths correctly set"
}

init_jvm_options()
{
  sed -i -e "s~@ELK_LOGS@~$ELK_LOGS~g" $LOGSTASH_HOME/config/jvm.options
}

init_logstash()
{
  echo "Initialize Logstash..."
  # Install logstash-output-solr plugin
  bash $LOGSTASH_HOME/bin/logstash-plugin install file://$LOGSTASH_HOME/logstash-offline-solr-plugin.zip
  # patch logstash-output-solr plugin
  sed -i "/@solr.add(documents)/c\    @solr.add\(documents, :add_attributes => \{:commitWithin=>10000\}, :params => \{:tr => @tr\}\)" $LOGSTASH_HOME/vendor/bundle/jruby/2.5.0/gems/logstash-output-solr_http-3.0.5/lib/logstash/outputs/solr_http.rb
  sed -i "/document\[\"@timestamp\"\]\.iso8601/c\ " $LOGSTASH_HOME/vendor/bundle/jruby/2.5.0/gems/logstash-output-solr_http-3.0.5/lib/logstash/outputs/solr_http.rb
  # Replace the default conf with the correct paths before starting logstash
  sed -i "/francelabs\/datafari-stats.log/c\    path => \"${DATAFARI_HOME}/logs/datafari-stats.log*\"" $LOGSTASH_HOME/logstash-datafari.conf
  sed -i "/francelabs\/datafari-monitoring.log/c\    path => \"${DATAFARI_HOME}/logs/datafari-monitoring.log*\"" $LOGSTASH_HOME/logstash-datafari.conf
  sed -i "/francelabs\/localhost_access_log_datafari.txt/c\    path => \"${DATAFARI_HOME}/logs/localhost_access_log_datafari*\"" $LOGSTASH_HOME/logstash-datafari.conf
  @ADDITIONAL_LOGSTASH_INIT@
  echo "Logstash initialized !"
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

start_zeppelin()
{
  if is_running $ZEPPELIN_PID_DIR/zeppelin.pid; then
    echo "Error : Zeppelin seems to be already running with PID $(cat $ZEPPELIN_PID_DIR/zeppelin.pid)"
  else
    cd $ZEPPELIN_HOME
    echo "Starting Zeppelin..."
    bash bin/zeppelin-daemon.sh start >/dev/null 2>&1 &
    echo $! > $ZEPPELIN_PID_DIR/zeppelin.pid
    echo "Zeppelin started !"
    cd $DIR
  fi
}

stop_zeppelin()
{
  cd $ZEPPELIN_HOME
  echo "Stopping Zeppelin..."
  bash bin/zeppelin-daemon.sh stop >/dev/null 2>&1 &
  echo "Zeppelin stopped !"
  cd $DIR
  forceStopIfNecessary $ZEPPELIN_PID_DIR/zeppelin.pid Zeppelin
}

cmd_start() {
  start_logstash;
  start_zeppelin;
}

cmd_stop() {
  stop_logstash;
  stop_zeppelin;
}

cmd_status() {
  
  if is_running $LOGSTASH_PID_FILE; then
      echo "Logstash is running:"
      ps -o pid,cmd --width 5000 -p $(cat $LOGSTASH_PID_FILE)
  else
      echo "Logstash is not running."
  fi
  
  if is_running $ZEPPELIN_PID_DIR/zeppelin.pid; then
      echo "Zeppelin is running:"
      ps -o pid,cmd --width 5000 -p $(cat $ZEPPELIN_PID_DIR/zeppelin.pid)
  else
      echo "Zeppelin is not running."
  fi
}

init_elk()
{
  echo "Initializing ELK..."
  set_logs_paths;
  init_jvm_options;
  init_logstash;
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
  start_logstash)
    start_logstash
    ;;
  stop_logstash)
    stop_logstash
    ;;
  start_zeppelin)
    start_zeppelin
    ;;
  stop_zeppelin)
    stop_zeppelin
    ;;
  init_elk)
    init_elk
    ;;
  @ADDITIONAL_COMMANDS@
esac




