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

ip_solr="@SOLRNODEIP@:8983"
maxShardsPerNode=50
lib_path="${SOLR_INSTALL_DIR}/solrcloud/FileShare/"
lib_path_duplicates="${SOLR_INSTALL_DIR}/solrcloud/Duplicates/"
mcf_ip="@NODEHOST@"
mcf_port=""
mcf_path="datafari-mcf-authority-service"
cdcr_class_cdcr="solr.CdcrUpdateLog"
cdcr_class="solr.update.UpdateLog"
cdcr_processor_cdcr="cdcr-processor-chain"
cdcr_processor=""
baseConfigSet="Init"
url_protocol=""


create_collection() {
	curl -XGET --insecure "$url_protocol://${ip_solr}/solr/admin/collections?action=CREATE&name=$1&collection.configName=$2&numShards=$3&maxShardsPerNode=${maxShardsPerNode}&replicationFactor=$4&property.lib.path=${lib_path}&property.mcf.ip=$url_protocol://${mcf_ip}:${mcf_port}/${mcf_path}&property.cdcr.class=$5&property.cdcr.processor=$6"
	curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"autocomplete.threshold": "0.005"}}' $url_protocol://${ip_solr}/solr/$1/config
  	curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"entity.extract": "false"}}' $url_protocol://${ip_solr}/solr/$1/config
  	curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"entity.name": "false"}}' $url_protocol://${ip_solr}/solr/$1/config
  	curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"entity.phone": "false"}}' $url_protocol://${ip_solr}/solr/$1/config
  	curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"entity.special": "false"}}' $url_protocol://${ip_solr}/solr/$1/config
  	curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"texttagger.enabled": "false"}}' $url_protocol://${ip_solr}/solr/$1/config
    curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"texttagger.host": "localhost:2181"}}' $url_protocol://${ip_solr}/solr/$1/config
  	curl -XPOST --insecure $url_protocol://${ip_solr}/solr/$1/config/params -H 'Content-type:application/json'  -d '{"set":{"mySearch":{"qf":"title_fr^50 title_en^50 title_de^50 title_es^50 content_fr^10 content_en^10 content_de^50 content_es^50 source^20 id^3 url_search^3","pf":"title_en^500 title_fr^500 title_de^500 title_es^500 content_fr^100 content_en^100 content_de^100 content_es^100 url_search^30","hl.maxAnalyzedChars":51200}}}'
  	curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"duplicates.enabled": "false"}}' $url_protocol://${ip_solr}/solr/$1/config
  	curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"duplicates.solr.host": "localhost:2181"}}' $url_protocol://${ip_solr}/solr/$1/config
  	curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"duplicates.collection": "Duplicates"}}' $url_protocol://${ip_solr}/solr/$1/config
  	curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"duplicates.hash.fields": "content"}}' $url_protocol://${ip_solr}/solr/Duplicates/config
  	curl -XPOST --insecure -H 'Content-type:application/json' -d '{"set-user-property": {"duplicates.quant.rate": "0.1"}}' $url_protocol://${ip_solr}/solr/Duplicates/config
  
  
}



init_cdcr_target() {
	curl -X POST --insecure -H 'Content-type:application/json' -d '{ "add-requesthandler": { "name": "/cdcr", "class": "solr.CdcrRequestHandler", "buffer": { "defaultstate": "disabled"  }}}' $url_protocol://${ip_solr}/api/collections/$1/config

}

init_cdcr_source() {
	curl -X POST --insecure -H 'Content-type:application/json' -d '{ "add-requesthandler": { "name": "/cdcr", "class": "solr.CdcrRequestHandler", "replica":[ { "zkHost": "'"$2"'", "source" : "'"$1"'", "target" : "'"$3"'"},{ "zkHost": "'"$4"'", "source" : "'"$1"'", "target" : "'"$5"'"}],"replicator": { "threadPoolSize" : 8, "schedule" : 1000, "batchSize" : 128 }, "updateLogSynchronizer" : { "schedule": 1000 }  }}}' $url_protocol://${ip_solr}/api/collections/$1/config
}

update_cdcr_source() {
	curl -X POST --insecure -H 'Content-type:application/json' -d '{ "update-requesthandler": { "name": "/cdcr", "class": "solr.CdcrRequestHandler", "replica":[ { "zkHost": "'"$2"'", "source" : "'"$1"'", "target" : "'"$3"'"},{ "zkHost": "'"$4"'", "source" : "'"$1"'", "target" : "'"$5"'"}],"replicator": { "threadPoolSize" : 8, "schedule" : 1000, "batchSize" : 128 }, "updateLogSynchronizer" : { "schedule": 1000 }  }}}' $url_protocol://${ip_solr}/api/collections/$1/config
}

init_configset() {
	curl -X POST --insecure -H 'Content-type: application/json' -d '{  "create":{ "name": "'"$1"'",  "baseConfigSet": "'"$baseConfigSet"'" }}' $url_protocol://${ip_solr}/api/cluster/configs?omitHeader=true
}

update_cdcr_source() {
	curl -X POST --insecure -H 'Content-type:application/json' -d '{ "update-requesthandler": { "name": "/cdcr", "class": "solr.CdcrRequestHandler", "replica":[ { "zkHost": "'"$2"'", "source" : "'"$1"'", "target" : "'"$3"'"},{ "zkHost": "'"$4"'", "source" : "'"$1"'", "target" : "'"$5"'"}],"replicator": { "threadPoolSize" : 8, "schedule" : 1000, "batchSize" : 128 }, "updateLogSynchronizer" : { "schedule": 1000 }  }}}' $url_protocol://${ip_solr}/api/collections/$1/config
}




#For Updating CDCR target URLs
#update_cdcr_source "SOURCE_COLLECTION" "IP_ZK_DISTANT_1:2181" "TARGET_COLLECTION_1" "IP_ZK_DISTANT_2:2181" "TARGET_COLLECTION_2"

if [ "$SSL_ALL" == "true" ]; then
        url_protocol="https"
        mcf_port=9443
else
        url_protocol="http"
        mcf_port=9080
fi



echo "Creation of Solr Collections"
echo "--- NOTE --- Please be sure that all your Solr servers are up !!!"
echo "Number of Solr nodes running :"
curl --silent "$url_protocols://localhost:8983/solr/admin/zookeeper?detail=true&path=%2Flive_nodes" | ${DATAFARI_HOME}/command/jq .znode.prop.children_count

echo "Creation of Solr Collections Statistics, Promolinks and Entities"
curl -XGET --insecure "$url_protocol://${ip_solr}/solr/admin/collections?action=CREATE&name=Duplicates&collection.configName=Duplicates&numShards=1&replicationFactor=1&maxShardsPerNode=1&property.lib.path=${lib_path_duplicates}"
curl -XGET --insecure "$url_protocol://${ip_solr}/solr/admin/collections?action=CREATE&name=Statistics&collection.configName=Statistics&numShards=1&replicationFactor=1"
curl -XGET --insecure "$url_protocol://${ip_solr}/solr/admin/collections?action=CREATE&name=Promolink&collection.configName=Promolink&numShards=1&replicationFactor=1"
curl -XGET --insecure "$url_protocol://${ip_solr}/solr/admin/collections?action=CREATE&name=Entities&collection.configName=Entities&numShards=1&replicationFactor=1"

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

		cdcr_activate=$cdcr
		echo $cdcr_activate


		if [[ $cdcr_activate == no ]]; then
			create_collection $name_collection  $name_configset $fileshare_shards $fileshare_replication $cdcr_class $cdcr_processor
		else
			read -p "Are you on the Solr source (yes/no) ?: " cdcr_source
			cdcr_source=${cdcr_source:-yes}
			echo $cdcr_source

			if [[ $cdcr_source == yes ]]; then

				read -p "What are the IPs of the DISTANT Zookeeper cluster (x.y.z:2181,a.b.c:2181) ?: " cdcr_distant_zk
				cdcr_distant_zk=${cdcr_distant_zk}
				echo $cdcr_distant_zk

				read -p "What is the name of the distant collection ?: " cdcr_collection_distant
				cdcr_collection_distant=${cdcr_collection_distant}
				echo $cdcr_collection_distant
				
				read -p "What are the IPs of the 2nd DISTANT Zookeeper cluster (x.y.z:2181,a.b.c:2181) ?: " cdcr_distant_zk_2
				cdcr_distant_zk_2=${cdcr_distant_zk_2}
				echo $cdcr_distant_zk_2

				read -p "What is the name of the 2nd distant collection ?: " cdcr_collection_distant_2
				cdcr_collection_distant_2=${cdcr_collection_distant_2}
				echo $cdcr_collection_distant_2

				create_collection $name_collection  $name_configset $fileshare_shards $fileshare_replication $cdcr_class_cdcr $cdcr_processor
				
				init_cdcr_source $name_collection $cdcr_distant_zk $cdcr_collection_distant $cdcr_distant_zk_2 $cdcr_collection_distant_2

            else
				read -p "Are you on the Solr target (yes/no) ?: " cdcr_target
				cdcr_target=${cdcr_target:yes}
				echo $cdcr_target

				if [[ $cdcr_target == yes ]]; then
					create_collection $name_collection  $name_configset $fileshare_shards $fileshare_replication $cdcr_class_cdcr $cdcr_processor_cdcr
					init_cdcr_target $name_collection

				else
					exit 0
				fi
			fi

		fi

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
