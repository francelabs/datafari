#!/bin/bash -e
#
#
# Download config from ZK
#
#

"$1solr/server/scripts/cloud-scripts/zkcli.sh" -zkhost $2 -cmd downconfig -confname $3 -confdir $1solr/solrcloud/tmp/

