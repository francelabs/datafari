#!/bin/bash -e
#
#
# Reload main collection
#
#

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/../set-datafari-env.sh"
source "${DIR}/../utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE

#curl --insecure -XGET "https://@SOLRNODEIP@:8983/solr/admin/collections?action=RELOAD&name=${mainCollection}"
curl -XGET "http://@SOLRNODEIP@:8983/solr/admin/collections?action=RELOAD&name=${mainCollection}"