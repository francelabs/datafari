#!/bin/bash -e
#
#
# Set Datafari environment variable
#
#

export DATAFARI_HOME=$( cd "$( dirname "${BASH_SOURCE[0]}" )/../.." && pwd )

#Global vars
export PID_DIR=$DATAFARI_HOME/pid
export AS_HOME=$DATAFARI_HOME/analytic-stack
export AS_LOGS=$DATAFARI_HOME/logs
export LOGSTASH_HOME=$AS_HOME/logstash
export ZEPPELIN_HOME=$AS_HOME/zeppelin
export TIKA_SERVER_HOME=$DATAFARI_HOME/tika-server
export LS_JAVA_HOME=$JAVA_HOME

# pid files 
export LOGSTASH_PID_FILE=$PID_DIR/logstash.pid

export RETRIES_NUMBER=35