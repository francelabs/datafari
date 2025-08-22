#!/bin/bash -e
#
#
# Initialize Solr Collections
#
#

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

source "${DIR}/../set-datafari-env.sh"
source "${DIR}/../utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE


secondary_collections=''

ip_solr="@SOLRNODEIP@:443"
ip_zk="@SOLRNODEIP@:2181"
maxShardsPerNode=50
lib_path="${SOLR_INSTALL_DIR}/solrcloud/FileShare/"
lib_path_duplicates="${SOLR_INSTALL_DIR}/solrcloud/Duplicates/"
lib_path_vectormain="${SOLR_INSTALL_DIR}/solrcloud/VectorMain/"
lib_path_access="${SOLR_INSTALL_DIR}/solrcloud/Access/"
lib_path_vectormain="${SOLR_INSTALL_DIR}/solrcloud/VectorMain/"
mcf_ip="@NODEHOST@"
mcf_port=""
mcf_path="datafari-mcf-authority-service"
baseConfigSet="Init"
url_protocol=""


create_collection() {
	curl -XGET --insecure "$url_protocol://${ip_solr}/solr/admin/collections?action=CREATE&name=$1&collection.configName=$2&numShards=$3&maxShardsPerNode=${maxShardsPerNode}&replicationFactor=$4&property.lib.path=${lib_path}&property.mcf.ip=$url_protocol://${mcf_ip}:${mcf_port}/${mcf_path}"
	curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"autocomplete.threshold": "0.005"}}' $url_protocol://${ip_solr}/solr/$1/config
	curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"clustering.enabled": "false"}}' $url_protocol://${ip_solr}/solr/$1/config
  	curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"texttagger.enabled": "false"}}' $url_protocol://${ip_solr}/solr/$1/config
    curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"texttagger.host": "localhost:2181"}}' $url_protocol://${ip_solr}/solr/$1/config
  	curl -XPOST --insecure $url_protocol://${ip_solr}/solr/$1/config/params -H 'Content-type:application/json'  -d '{"set":{"mySearch":{"qf":"exactContent^500 embedded_content^500 exactTitle^500 title_fr^50 title_en^50 title_de^50 title_es^50 content_fr^10 content_en^10 content_de^50 content_es^50 source^20 id^3 url_search^3","pf":"exactContent^500 embedded_content^500 exactTitle^500 title_en^500 title_fr^500 title_de^500 title_es^500 content_fr^100 content_en^100 content_de^100 content_es^100 url_search^30","hl.maxAnalyzedChars":51200}}}'

    cd ${SOLR_INSTALL_DIR}/solrcloud/FileShare/conf/customs_schema && bash addCustomSchemaInfo.sh

    echo "Creation of Solr Collections"
echo "--- NOTE --- Please be sure that all your Solr servers are up !!!"


echo "Creation of Solr Collections Statistics, Promolinks and Entities"
curl -XGET --insecure "$url_protocol://${ip_solr}/solr/admin/collections?action=CREATE&name=Duplicates&collection.configName=Duplicates&numShards=1&replicationFactor=1&maxShardsPerNode=1&property.lib.path=${lib_path_duplicates}"
curl -XGET --insecure "$url_protocol://${ip_solr}/solr/admin/collections?action=CREATE&name=VectorMain&collection.configName=VectorMain&numShards=1&replicationFactor=1&maxShardsPerNode=1&property.lib.path=${lib_path_vectormain}&property.mcf.ip=$url_protocol://${mcf_ip}:${mcf_port}/${mcf_path}"
curl -XGET --insecure "$url_protocol://${ip_solr}/solr/admin/collections?action=CREATE&name=Statistics&collection.configName=Statistics&numShards=1&replicationFactor=1"
curl -XGET --insecure "$url_protocol://${ip_solr}/solr/admin/collections?action=CREATE&name=Promolink&collection.configName=Promolink&numShards=1&replicationFactor=1"
curl -XGET --insecure "$url_protocol://${ip_solr}/solr/admin/collections?action=CREATE&name=Entities&collection.configName=Entities&numShards=1&replicationFactor=1"
curl -XGET --insecure "$url_protocol://${ip_solr}/solr/admin/collections?action=CREATE&name=Access&collection.configName=Access&numShards=1&maxShardsPerNode=1&replicationFactor=1&property.lib.path=${lib_path_access}"
curl -XGET --insecure "$url_protocol://${ip_solr}/solr/admin/collections?action=CREATE&name=Monitoring&collection.configName=Monitoring&numShards=1&maxShardsPerNode=1&replicationFactor=1"
curl -XGET --insecure "$url_protocol://${ip_solr}/solr/admin/collections?action=CREATE&name=Crawl&collection.configName=Crawl&numShards=1&maxShardsPerNode=1&replicationFactor=1"
curl -XGET --insecure "$url_protocol://${ip_solr}/solr/admin/collections?action=CREATE&name=Logs&collection.configName=Logs&numShards=1&maxShardsPerNode=1&replicationFactor=1"
curl -XGET --insecure "$url_protocol://${ip_solr}/solr/admin/collections?action=CREATE&name=VectorMain&collection.configName=VectorMain&numShards=1&maxShardsPerNode=1&replicationFactor=1&property.lib.path=${lib_path_vectormain}"

curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"duplicates.hash.fields": "content"}}' $url_protocol://${ip_solr}/solr/Duplicates/config
curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"duplicates.quant.rate": "0.1"}}' $url_protocol://${ip_solr}/solr/Duplicates/config

  curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"vector.collection": "VectorMain"}}' $url_protocol://${ip_solr}/solr/FileShare/config
  curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"vector.host": "'"${ip_zk}"'"}}' $url_protocol://${ip_solr}/solr/FileShare/config
  curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"vector.chunksize": "300"}}' $url_protocol://${ip_solr}/solr/FileShare/config
  curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"vector.maxoverlap": "0"}}' $url_protocol://${ip_solr}/solr/FileShare/config

  curl -XPOST --insecure $url_protocol://${ip_solr}/solr/VectorMain/config/params -H 'Content-type:application/json'  -d '{"set":{"mySearch":{"qf":"exactContent^500 embedded_content^500 exactTitle^500 title_fr^50 title_en^50 title_de^50 title_es^50 content_fr^10 content_en^10 content_de^50 content_es^50 source^20 id^3 url_search^3","pf":"exactContent^500 embedded_content^500 exactTitle^500 title_en^500 title_fr^500 title_de^500 title_es^500 content_fr^100 content_en^100 content_de^100 content_es^100 url_search^30","hl.maxAnalyzedChars":51200}}}'


  curl -XGET --insecure "$url_protocol://${ip_solr}/solr/admin/collections?action=CREATE&name=OCR&collection.configName=GenericAnnotator&numShards=1&maxShardsPerNode=1&replicationFactor=1&property.lib.path=${SOLR_INSTALL_DIR}/solrcloud/GenericAnnotator/"
  curl -XGET --insecure "$url_protocol://${ip_solr}/solr/admin/collections?action=CREATE&name=Spacy&collection.configName=GenericAnnotator&numShards=1&maxShardsPerNode=1&replicationFactor=1&property.lib.path=${SOLR_INSTALL_DIR}/solrcloud/GenericAnnotator/"
  curl -XGET --insecure "$url_protocol://${ip_solr}/solr/admin/collections?action=CREATE&name=GenericAnnotator&collection.configName=GenericAnnotator&numShards=1&maxShardsPerNode=1&replicationFactor=1&property.lib.path=${SOLR_INSTALL_DIR}/solrcloud/GenericAnnotator/"
  
  collections_autocommit=("Access" "Crawl" "Duplicates" "Entities" "$1" "GenericAnnotator" "Logs" "Monitoring" "OCR" "Promolink" "Spacy" "Statistics" "VectorMain")
  for index in "${collections_autocommit[@]}"
  do
    curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-property": {"updateHandler.autoCommit.maxTime": "60000"}}' $url_protocol://${ip_solr}/solr/${index}/config
  done

  

}

init_configset() {
	curl -X POST --insecure -H 'Content-type: application/json' -d '{  "create":{ "name": "'"$1"'",  "baseConfigSet": "'"$baseConfigSet"'" }}' $url_protocol://${ip_solr}/api/cluster/configs?omitHeader=true
}




url_protocol="https"
mcf_port=443






number_collections=$numCollections



if (($number_collections > 0)); then
	for i in $(seq 1 $number_collections); do
		echo "Collection$i"
		name_collection=$SOLRMAINCOLLECTION
		name_configset=${name_collection}
		echo $name_collection

		creation_configset=$configset
		echo $creation_configset

		if [[ $creation_configset == yes ]]; then
			init_configset $name_collection
		fi

		fileshare_shards=$SOLRNUMSHARDS
		echo $fileshare_shards

		fileshare_replication=$replicationFactor
		echo $fileshare_replication

		create_collection $name_collection  $name_configset $fileshare_shards $fileshare_replication

		if (($i == 2)); then
			secondary_collections=$name_collection
		fi
		if  (($i > 2)); then
			secondary_collections="$secondary_collections,$name_collection"
fi

	done
if [[ $secondary_collections != '' ]]; then
	echo "secondary collections :" $secondary_collections
	setProperty "secondaryCollections" $secondary_collections $CONFIG_FILE
	fi
else

	echo "end of script"
	exit 0
fi