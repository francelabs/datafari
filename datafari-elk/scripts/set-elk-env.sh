#!/bin/bash -e
#
#
# Set Datafari environment variable
#
#

export DATAFARI_HOME=$( cd "$( dirname "${BASH_SOURCE[0]}" )/../.." && pwd )

export PID_DIR=$DATAFARI_HOME/pid
export ELK_HOME=$DATAFARI_HOME/elk
export ELK_LOGS=$DATAFARI_HOME/logs/elk
export LOGSTASH_HOME=$ELK_HOME/logstash
export TIKA_SERVER_HOME=$DATAFARI_HOME/tika-server

# pid files 
export LOGSTASH_PID_FILE=$PID_DIR/logstash.pid

export RETRIES_NUMBER=35