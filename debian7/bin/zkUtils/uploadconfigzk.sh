#!/bin/bash -e
#
#
# Upload ZK configuration to Solr
#
#

tmpFolder=`date +%s`_zk_config;
mkdir -p $1/.tmp/$tmpfolder;
cp -R $1/solr/solrcloud/$3/conf/ $1/.tmp/$tmpFolder;
rm $1/.tmp/$tmpFolder/managed-schema;
"$1/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost $2 -confdir $1/.tmp/$tmpFolder -confname $3;
rm -R $1/.tmp/$tmpFolder;
