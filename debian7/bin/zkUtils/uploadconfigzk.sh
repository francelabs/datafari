#!/bin/bash -e
#
#
# Upload ZK configuration to Solr
#
#



"$1/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost $2 -confdir $1/solr/solrcloud/tmp/ -confname $3



