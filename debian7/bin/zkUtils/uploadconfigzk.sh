#!/bin/bash -e
#
#
# Upload ZK configuration to Solr
#
#

files=${4:-"*"};
tmpFolder=`date +%s`_zk_config;
mkdir -p $1/.tmp/$tmpFolder;
cp -R $1/solr/solrcloud/$3/conf/$files $1/.tmp/$tmpFolder;
if [ -e $1/.tmp/$tmpFolder/managed-schema ]
then
  rm $1/.tmp/$tmpFolder/managed-schema;
fi
"$1/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost $2 -confdir $1/.tmp/$tmpFolder -confname $3;
rm -R $1/.tmp/$tmpFolder;
