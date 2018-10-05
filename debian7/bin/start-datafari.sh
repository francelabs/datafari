#!/bin/bash -e
#
#
# Startup script for Datafari
#
#

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/set-datafari-env.sh"
source "${DIR}/utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE


if run_as ${DATAFARI_USER} "bash datafari-manager.sh is_running $CATALINA_PID"; then
    PID=$(run_as ${DATAFARI_USER} "cat $CATALINA_PID");
    echo "Error: Tomcat seems to be already running with PID $PID"
    exit 1
fi

if run_as ${DATAFARI_USER} "bash datafari-manager.sh is_running $MCF_PID_FILE"; then
    PID=$(run_as ${DATAFARI_USER} "cat $MCF_PID_FILE");
    echo "Error: MCF Agent seems to be already running with PID $PID"
    exit 1
fi

if run_as ${DATAFARI_USER} "bash datafari-manager.sh is_running $SOLR_PID_FILE"; then
   PID=$(run_as ${DATAFARI_USER} "cat $SOLR_PID_FILE");
   echo "Error : Solr seems to be already running with PID $PID"
   exit 1
fi

if run_as ${DATAFARI_USER} "bash datafari-manager.sh is_running $CASSANDRA_PID_FILE"; then
   PID=$(run_as ${DATAFARI_USER} "cat $CASSANDRA_PID_FILE");
   echo "Error : Cassandra seems to be already running with PID $PID"
   exit 1
fi

if run_as ${DATAFARI_USER} "bash datafari-manager.sh is_running $ZK_PID_FILE"; then
   PID=$(run_as ${DATAFARI_USER} "cat $ZK_PID_FILE");
   echo "Error : Zookeeper seems to be already running with PID $PID"
   exit 1
fi

check_java;
check_python;


if  [[ "$STATE" = *installed* ]];
then
	cd $ELK_HOME/scripts
  run_as ${DATAFARI_USER} "bash elk-manager.sh init_elk";
  cd $DIR

	echo "Start postgres and cassandra and add ManifoldCF database"
	run_as ${POSTGRES_USER} "bash datafari-manager.sh init_postgres_repertories";
  run_as ${DATAFARI_USER} "bash datafari-manager.sh init_cassandra_repertories";
  run_as ${DATAFARI_USER} "bash datafari-manager.sh init_zookeeper_repertory";
  
  
	run_as ${DATAFARI_USER} "bash datafari-manager.sh start_cassandra";
  waitCassandra;
  run_as ${DATAFARI_USER} "bash datafari-manager.sh init_cassandra";

  run_as ${POSTGRES_USER} "bash datafari-manager.sh init_postgres";
  run_as ${POSTGRES_USER} "bash datafari-manager.sh start_postgres";
fi


run_as ${DATAFARI_USER} "bash datafari-manager.sh start_zookeeper";

if  [[ "$STATE" = *installed* ]];
then
  run_as ${DATAFARI_USER} "bash datafari-manager.sh init_mcf_crawler_agent";
	run_as ${DATAFARI_USER} "bash datafari-manager.sh init_zk_mcf";
	run_as ${DATAFARI_USER} "bash datafari-manager.sh init_zk";
else
	run_as ${POSTGRES_USER} "bash datafari-manager.sh start_postgres";
  run_as ${DATAFARI_USER} "bash datafari-manager.sh start_cassandra";
  waitCassandra;
fi

run_as ${DATAFARI_USER} "bash datafari-manager.sh start_mcf_crawler_agent";
run_as ${DATAFARI_USER} "bash datafari-manager.sh start_solr";

echo "Wait 5s that Solr registers to Zookeeper..."
sleep 5


if  [[ "$STATE" = *installed* ]];
then
	run_as ${DATAFARI_USER} "bash datafari-manager.sh init_solr";
fi

run_as ${DATAFARI_USER} "bash datafari-manager.sh start_tomcat";
waitTomcat;
  
if  [[ "$STATE" = *installed* ]];
then
  run_as ${DATAFARI_USER} "bash datafari-manager.sh init_mcf";   
  #act end initialization
  run_as ${DATAFARI_USER} "sed -i 's/\(STATE *= *\).*/\1initialized/' $INIT_STATE_FILE"
fi



