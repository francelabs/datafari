#!/bin/bash -e
#
#
# Upload current config in Solr and reload cores
#
#

declare -a cores=("FileShare" "Statistics" "Promolinks")
chmod -R 755 "./target/dist/solr/server"
for core in "${cores[@]}"
do
echo "Update Solr config for core : $core"
"./target/dist/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost localhost -confdir "./solr_home/FileShare/conf/" -confname FileShare
sleep 1
curl "http://localhost:8983/solr/admin/cores?action=RELOAD&core=FileShare_shard1_replica1"
done




