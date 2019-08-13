#!/bin/bash -e
#
#
# Upload ZK configuration to Solr
#
#


DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/../set-datafari-env.sh"
source "${DIR}/../utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE


curl -XGET "http://localhost:8983/solr/admin/collections?action=RELOAD&name=${mainCollection}"