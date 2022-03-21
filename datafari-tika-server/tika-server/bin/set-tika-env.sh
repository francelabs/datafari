#!/bin/bash
#
# Tika env
#

export DATAFARI_HOME=$( cd "$( dirname "${BASH_SOURCE[0]}" )/../.." && pwd )
export TMP_DIR=/tmp
export TIKA_MEM="-Xmx@TIKASERVERMEMORY@"
export TIKA_SERVER_HOME=$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )
export PID_DIR=$DATAFARI_HOME/pid
export TIKA_SERVER_PID_FILE=$PID_DIR/tika.pid
export TIKA_LOGS_DIR=$DATAFARI_HOME/logs/tika-server