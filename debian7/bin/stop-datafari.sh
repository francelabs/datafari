#!/bin/bash -e

if (( EUID != 0 )); then
   echo "You need to be root to run this script." 1>&2
   exit 100
fi

export DATAFARI_HOME=$(pwd)/..
export JAVA_HOME=${DATAFARI_HOME}/jvm
export LD_LIBRARY_PATH=${DATAFARI_HOME}/pgsql/lib
export CONFIG_FILE=${DATAFARI_HOME}/tomcat/conf/datafari.properties
source $CONFIG_FILE
cd ${DATAFARI_HOME}/tomcat/bin
sh "shutdown.sh"
if [[ "$SOLRCLOUD" = *false* ]] || [[ "$ISMAINNODE" = *true* ]];
then
	cd ${DATAFARI_HOME}/mcf/mcf_home
	sh "stop-agents.sh"
	su postgres -c "${DATAFARI_HOME}/pgsql/bin/pg_ctl -D ${DATAFARI_HOME}/pgsql/data -l ${DATAFARI_HOME}/logs/pgsql.log stop"
	if [[ "$SOLRCLOUD" = *true* ]];
	then
		cd "${DATAFARI_HOME}/zookeeper/bin"
		bash zkServer.sh stop
	fi
fi


