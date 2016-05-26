#!/bin/bash -e
#
#
# Upload ZK configuration to Solr
#
#



"$1solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost $2 -confdir $1solr/solrcloud/tmp/ -confname $3

rm -rf $1solr/solrcloud/tmp/

