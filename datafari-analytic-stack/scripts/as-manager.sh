#!/bin/bash
#
# ELK Manager 
#

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/set-as-env.sh"
source "${DIR}/as-utils.sh"

set_logs_paths()
{
  echo "Set logs paths..."
  sed -i "/path.logs/c\path.logs: ${AS_LOGS}" $LOGSTASH_HOME/config/logstash.yml
  sed -i -e "s~@AS_LOGS@~$AS_LOGS~g" $LOGSTASH_HOME/config/jvm.options
  echo "Logs paths correctly set"
}

init_logstash()
{
  echo "Initialize Logstash..."
  set_logs_paths;
  # Install logstash-output-solr plugin
  bash $LOGSTASH_HOME/bin/logstash-plugin install file://$LOGSTASH_HOME/logstash-offline-solr-plugin.zip
  # patch logstash-output-solr plugin
  sed -i "/@solr.add(documents)/c\    @solr.add\(documents, :add_attributes => \{:commitWithin=>3600000\}, :params => \{:tr => @tr\}\)" $LOGSTASH_HOME/vendor/bundle/jruby/*/gems/logstash-output-solr_http-*/lib/logstash/outputs/solr_http.rb
  sed -i "/document\[\"@timestamp\"\]\.iso8601/c\ " $LOGSTASH_HOME/vendor/bundle/jruby/*/gems/logstash-output-solr_http-*/lib/logstash/outputs/solr_http.rb
  ssl_logstash="require 'openssl'\nOpenSSL::SSL::VERIFY_PEER = OpenSSL::SSL::VERIFY_NONE\n"
  sed -i -e "/^require \"uuidtools\"/a${ssl_logstash}" $LOGSTASH_HOME/vendor/bundle/jruby/*/gems/logstash-output-solr_http-*/lib/logstash/outputs/solr_http.rb
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

cmd_start() {
  start_logstash;
}

cmd_stop() {
  stop_logstash;
}

cmd_status() {
  
  if is_running $LOGSTASH_PID_FILE; then
      echo "Logstash is running:"
      ps -o pid,cmd --width 5000 -p $(cat $LOGSTASH_PID_FILE)
  else
      echo "Logstash is not running."
  fi
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
esac




