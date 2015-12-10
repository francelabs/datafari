#!/bin/bash -e
#
#
# Startup script for Datafari
#
#

if (( EUID != 0 )); then
   echo "You need to be root to run this script." 1>&2
   exit 100
fi

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/set-datafari-env.sh"
source "${DIR}/utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE


if is_running $CATALINA_PID; then
    echo "Error: Tomcat seems to be already running with PID $(cat $TOMCAT_PID_FILE)"
    exit 1
fi

if is_running $MCF_PID_FILE; then
    echo "Error: MCF Agent seems to be already running with PID $(cat $MCF_PID_FILE)"
    exit 1
fi

if is_running $SOLR_PID_FILE; then
   echo "Error : Solr seems to be already running with PID $(cat $SOLR_PID_FILE)"
   exit 1
fi

if is_running $CASSANDRA_PID_FILE; then
   echo "Error : Cassandra seems to be already running with PID $(cat $CASSANDRA_PID_FILE)"
   exit 1
fi

if  [[ "$STATE" = *installed* ]];
then
	echo "Start postgres and cassandra and add ManifoldCF database"
	rm -rf "${DATAFARI_HOME}/pgsql/data"
	mkdir "${DATAFARI_HOME}/pgsql/data"
	rm -rf "${DATAFARI_HOME}/cassandra/data"
	mkdir "${DATAFARI_HOME}/cassandra/data"
	CASSANDRA_INCLUDE=$CASSANDRA_ENV $CASSANDRA_HOME/bin/cassandra -p $CASSANDRA_PID_FILE 1>/dev/null
	id -u postgres &>/dev/null || useradd postgres
	chown -R postgres "${DATAFARI_HOME}/pgsql"
	chmod -R 777 "${DATAFARI_HOME}/logs"
	chmod -R 777 "${DATAFARI_HOME}/pid"
	su postgres -c "${DATAFARI_HOME}/pgsql/bin/initdb -U postgres -A password --pwfile=${DATAFARI_HOME}/pgsql/pwd.conf -E utf8 -D ${DATAFARI_HOME}/pgsql/data"
	cp "${DATAFARI_HOME}/pgsql/postgresql.conf.save" "${DATAFARI_HOME}/pgsql/data/postgresql.conf"
	su postgres -c "${DATAFARI_HOME}/pgsql/bin/pg_ctl -D ${DATAFARI_HOME}/pgsql/data -l ${DATAFARI_HOME}/logs/pgsql.log start"
	cd "${DATAFARI_HOME}/mcf/mcf_home"
	bash "initialize.sh"
	$CASSANDRA_HOME/bin/cqlsh -f ${DATAFARI_HOME}/bin/common/config/cassandra/tables 
fi

cd $TOMCAT_HOME/bin
bash startup.sh

if  [[ "$STATE" = *installed* ]];
then
	cd "${DATAFARI_HOME}/bin/common"
	"${JAVA_HOME}/bin/java" -cp DatafariScripts.jar com.francelabs.datafari.script.BackupManifoldCFConnectorsScript RESTORE config/manifoldcf/monoinstance
	sed -i "s/\(STATE *= *\).*/\1initialized/" $INIT_STATE_FILE

else
	su postgres -c "${DATAFARI_HOME}/pgsql/bin/pg_ctl -D ${DATAFARI_HOME}/pgsql/data -l ${DATAFARI_HOME}/logs/pgsql.log start"
	CASSANDRA_INCLUDE=$CASSANDRA_ENV $CASSANDRA_HOME/bin/cassandra -p $CASSANDRA_PID_FILE 1>/dev/null
fi



cd $MCF_HOME/../bin
bash mcf_crawler_agent.sh start

SOLR_INCLUDE=$SOLR_ENV $SOLR_INSTALL_DIR/bin/solr start
