#!/bin/bash -e
#
#
# Atomic Updates launcher script
#
#

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/../set-datafari-env.sh"
source "${DIR}/../utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE


echo "This script will launch atomic updates jar. Press the keys CTRL+C if you want to exit. Otherwise please wait 10 seconds"
sleep 10

nohup "${JAVA_HOME}/bin/java"  datafari-solr-atomic-update-*.jar $1 2>/dev/null &
  


echo "Atomic updates script done"
