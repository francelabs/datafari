#!/bin/bash -e

if (( EUID != 0 )); then
   echo "You need to be root to run this script." 1>&2
   exit 100
fi


export DATAFARI_HOME=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/..
export JAVA_HOME=${DATAFARI_HOME}/jvm
export LD_LIBRARY_PATH=${DATAFARI_HOME}/pgsql/lib
export INIT_STATE_FILE=${DATAFARI_HOME}/bin/common/init_state.properties
export CONFIG_FILE=${DATAFARI_HOME}/tomcat/conf/datafari.properties
source $INIT_STATE_FILE
source $CONFIG_FILE
nodes(){
        numrunningsolrnodes="`curl --silent 'http://localhost:8080/datafari-solr/zookeeper?detail=true&path=/live_nodes' | ${DATAFARI_HOME}/command/jq .znode.prop.children_count`"
}

NUMSHARDS="`echo ${NUMSHARDS} | tr -d '\r'`"
if [[ "$SOLRCLOUD" = *false* ]] || [[ "$ISMAINNODE" = *true* ]];
then
	if  [[ "$STATE" = *installed* ]];
	then
		echo "Cleaning data directories"
		rm -rf "${DATAFARI_HOME}/solr/solr_home/FileShare*"
		rm -rf "${DATAFARI_HOME}/solr/solr_home/Statistics*"
		rm -rf "${DATAFARI_HOME}/zookeeper/data"
		mkdir "${DATAFARI_HOME}/zookeeper/data"
		rm -rf "${DATAFARI_HOME}/pgsql/data"
		echo "Start postgres and add ManifoldCF database"
		mkdir "${DATAFARI_HOME}/pgsql/data"
		id -u postgres &>/dev/null || useradd postgres
		chown -R postgres "${DATAFARI_HOME}/pgsql"
		chmod -R 777 "${DATAFARI_HOME}/logs"
		su postgres -c "${DATAFARI_HOME}/pgsql/bin/initdb -U postgres -A password --pwfile=${DATAFARI_HOME}/pgsql/pwd.conf -E utf8 -D ${DATAFARI_HOME}/pgsql/data"
		su postgres -c "${DATAFARI_HOME}/pgsql/bin/pg_ctl -D ${DATAFARI_HOME}/pgsql/data -l ${DATAFARI_HOME}/logs/pgsql.log start"
		cd "${DATAFARI_HOME}/mcf/mcf_home"
		bash "initialize.sh"
	else
		su postgres -c "${DATAFARI_HOME}/pgsql/bin/pg_ctl -D ${DATAFARI_HOME}/pgsql/data -l ${DATAFARI_HOME}/logs/pgsql.log start"
	fi
fi
if  [[ "$SOLRCLOUD" = *true* ]];
then
	if [[ "$ISMAINNODE" = *true* ]];
	then
		echo "Start zookeeper"
		export SOLRCLOUDOPTION="-DzkHost=localhost:9080 "
		cd "${DATAFARI_HOME}/zookeeper/bin"
		bash zkServer.sh start
	else
		export SOLRCLOUDOPTION="-DzkHost=${MAINNODEHOST}:9080 "
	fi
fi
cd "${DATAFARI_HOME}/tomcat/bin"
bash "startup.sh"
echo "Wait until Solr is started"
until [ "`curl --silent --connect-timeout 2 -I http://localhost:8080/datafari-solr/ | grep '200 OK'`" != "" ];
do
	sleep 3
done

if  [[ "$SOLRCLOUD" = *true* ]];
then
	if [[ "$ISMAINNODE" = *true* ]];
	then
		nodes
		while (( ${numrunningsolrnodes} < ${NUMSHARDS} ));
		do
        		echo "There are only ${numrunningsolrnodes} Datafari node(s) running and the cluster is configured for ${NUMSHARDS} shards. Please run at least $((NUMSHARDS - numrunningsolrnodes)) more Datafari node(s) in secondary mode."
        		echo "Waiting for secondary node start..."
        		sleep 5
        		nodes
		done
	fi
fi

if  [[ "$STATE" = *installed* ]];
	then
	if  [[ "$SOLRCLOUD" = *true* ]];
	then
		if [[ "$ISMAINNODE" = *true* ]];
		then
			echo "Uploading configuration to zookeeper"
			"${JAVA_HOME}/bin/java" -cp "${DATAFARI_HOME}/solr/lib/*" org.apache.solr.cloud.ZkCLI -cmd upconfig -zkhost localhost:9080 -confdir "${DATAFARI_HOME}/solr/solrcloud/FileShare/conf" -confname FileShare
			"${JAVA_HOME}/bin/java" -cp "${DATAFARI_HOME}/solr/lib/*" org.apache.solr.cloud.ZkCLI -cmd upconfig -zkhost localhost:9080 -confdir "${DATAFARI_HOME}/solr/solrcloud/Statistics/conf" -confname Statistics
			"curl" "http://localhost:8080/datafari-solr/admin/collections?action=CREATE&name=FileShare&numShards=${NUMSHARDS}&replicationFactor=1"
			"curl" "http://localhost:8080/datafari-solr/admin/collections?action=CREATE&name=Statistics&numShards=1&replicationFactor=1"
			echo "Configuring ManifoldCF Connectors"
			cd "${DATAFARI_HOME}/bin/common"
			"${JAVA_HOME}/bin/java" -cp DatafariScripts.jar com.francelabs.datafari.script.BackupManifoldCFConnectorsScript RESTORE config/manifoldcf/solrcloud
		fi
	else
		echo "Configuring ManifoldCF Connectors"
		cd "${DATAFARI_HOME}/bin/common"
		"${JAVA_HOME}/bin/java" -cp DatafariScripts.jar com.francelabs.datafari.script.BackupManifoldCFConnectorsScript RESTORE config/manifoldcf/monoinstance
	fi
	sed -i "s/\(STATE *= *\).*/\1initialized/" $INIT_STATE_FILE
fi
if [[ "$SOLRCLOUD" = *false* ]] || [[ "$ISMAINNODE" = *true* ]];
then
	echo "Start ManifoldCF Crawling agent"
	cd "${DATAFARI_HOME}/mcf/mcf_home"
	bash "lock-clean.sh"
	bash "start-agents.sh" &
	sleep 4
fi

