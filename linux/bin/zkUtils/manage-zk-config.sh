#!/bin/bash -e
#
# Helper script to push or download every configuration from ZK
#
#

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

source "${DIR}/../set-datafari-env.sh"
source "${DIR}/../utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE

array_analytics_collections=("Statistics" "Access" "Monitoring" "Crawl" "Logs")
solr_ip_script=localhost
solr_protocol=http
solr_port=8983
zk_ip_script=localhost:2181

echo "Helper script to load or push Solr configuration to Zookeeper"
if [ "$NODETYPE" != "monoserver" ] && [ "$NODETYPE" != "main" ];
then
  echo "This script can only be run into monoserver or if you have a Datafri cluster into the main server."
  echo "The script will now exit"
  exit 0
fi


read -p "What is the name of the Solr collection that you want to manage ? " conf_name
echo $conf_name

if [ "$NODETYPE" = "monoserver" ];
then
  zk_ip_script=$SOLRHOSTS
else

  value="\<${conf_name}\>"
  if [[ ${array_analytics_collections[@]} =~ $value ]]
  then
    echo "Collection in local Solr"
  else
    echo "Collection into Solr cluster"
    zk_ip_script=$SOLRHOSTS
    solr_ip_script=$solr1
    solr_protocol=https
    solr_port=443
  fi

fi


read -p "Do you want to push your configuration or download the configuration ? (upload/download) [upload]" conf_action
conf_action=${conf_action:-upload}
if [[ "$conf_action" != "upload" ]] && [[ "$conf_action" != "download" ]]; then
  echo "The chosen action : $conf_action is not into the choices list. The script will exit"
  exit 0
fi

if [ "$conf_action" = "upload" ];
then
  read -p "What is the location of the folder that contains your configuration to upload ?" upload_folder
  echo $upload_folder
  "$DATAFARI_HOME/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost $zk_ip_script -confdir $upload_folder -confname $conf_name

  read -p "Do you want to reload the Solr collection (yes/no) [yes] ? " reload_action
  reload_action=${reload_action:-yes}
  echo reload_action
  if [[ "$reload_action" = "yes" ]] || [[ "$reload_action" = "y" ]] || [[ "$reload_action" = "true" ]]; then
    reload_action=true
  else
    reload_action=false
  fi

  if [ "$reload_action" = "true" ];
  then
        curl -XGET --insecure "$solr_protocol://$solr_ip_script:$solr_port/solr/admin/collections?action=RELOAD&name=$conf_name" 
  fi

elif [ "$conf_action" = "download" ];
then
  read -p "What is the location of the folder that will contain the downloaded configuration ?" download_folder
  echo $download_folder
  "$DATAFARI_HOME/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd downconfig -zkhost $zk_ip_script -confdir $download_folder -confname $conf_name

fi
