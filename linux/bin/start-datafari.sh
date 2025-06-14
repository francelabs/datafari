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
source $AS_HOME/scripts/set-as-env.sh
source $TIKA_SERVER_HOME/bin/set-tika-env.sh


if  [[ "$STATE" = *installed* ]];
then
  echo "You need to initialize Datafari first. Please launch the script init-datafari.sh first"
  exit 0
fi
    
    

if run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh is_running $CATALINA_PID"; then
  PID=$(run_as ${DATAFARI_USER} "cat $CATALINA_PID");
  echo "Error: Tomcat seems to be already running with PID $PID"
  exit 1
fi

if run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh is_running $CATALINA_MCF_PID"; then
  PID=$(run_as ${DATAFARI_USER} "cat $CATALINA_MCF_PID");
  echo "Error: Tomcat-MCF seems to be already running with PID $PID"
  exit 1
fi

if run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh is_running $MCF_PID_FILE"; then
  PID=$(run_as ${DATAFARI_USER} "cat $MCF_PID_FILE");
  echo "Error: MCF Agent seems to be already running with PID $PID"
  exit 1
fi

if run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh is_running $SOLR_PID_FILE"; then
  PID=$(run_as ${DATAFARI_USER} "cat $SOLR_PID_FILE");
  echo "Error : Solr seems to be already running with PID $PID"
  exit 1
fi


if run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh is_running $CASSANDRA_PID_FILE"; then
  PID=$(run_as ${DATAFARI_USER} "cat $CASSANDRA_PID_FILE");
  echo "Error : Cassandra seems to be already running with PID $PID"
  exit 1
fi

if run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh is_running $ZK_PID_FILE"; then
  PID=$(run_as ${DATAFARI_USER} "cat $ZK_PID_FILE");
  echo "Error : Zookeeper seems to be already running with PID $PID"
  exit 1
fi


if run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh is_running $ZK_MCF_PID_FILE"; then
  PID=$(run_as ${DATAFARI_USER} "cat $ZK_MCF_PID_FILE");
  echo "Error : Zookeeper MCF seems to be already running with PID $PID"
  exit 1
fi

if run_as ${POSTGRES_USER} "bash ${DIR}/datafari-manager.sh is_running $POSTGRES_PID_FILE"; then
  PID=$(run_as ${POSTGRES_USER} "cat $POSTGRES_PID_FILE");
  echo "Error : Postgres seems to be already running with PID $PID"
  exit 1
fi

if run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh is_running $TIKA_SERVER_PID_FILE"; then
  PID=$(run_as ${DATAFARI_USER} "cat $TIKA_SERVER_PID_FILE");
  echo "Error : Tika Server seems to be already running with PID $PID"
  exit 1
fi

if run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh is_running $TIKA_SERVER_PID_FILE"; then
  PID=$(run_as ${DATAFARI_USER} "cat $TIKA_SERVER_PID_FILE");
  echo "Error : Tika Server seems to be already running with PID $PID"
  exit 1
fi

@START-CHECKS@


check_java;


@VERSION-START@

# Monoserver

if  [[ "$NODETYPE" = *mono* ]]; then

  

  if  [[ "$STATE" = *initialized* ]];
  then
    cd $AS_HOME/scripts
    run_as ${DATAFARI_USER} "bash as-manager.sh init_logstash";
     
    cd $DIR   
    
    echo "Start postgres and cassandra and add ManifoldCF database"
    run_as ${POSTGRES_USER} "bash ${DIR}/datafari-manager.sh init_postgres_repertories";
    run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh init_cassandra_repertories";
    run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh init_zookeeper_repertory";
    run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh init_zookeeper_mcf_repertory";
    
    run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh start_cassandra";
    waitCassandra;
    run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh init_cassandra";
  
    run_as ${POSTGRES_USER} "bash ${DIR}/datafari-manager.sh init_postgres";
    if  [[ "$POSTGRESQL_EXTERNAL" = false ]]; then
    run_as ${POSTGRES_USER} "bash ${DIR}/datafari-manager.sh start_postgres";
    fi
  fi


  run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh start_zookeeper";
  
  run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh start_zookeeper_mcf";

  if  [[ "$STATE" = *initialized* ]];
  then
    echo "Wait 10s that the Zookeepers are started..."
    sleep 10
    run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh init_zk_mcf";
    run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh init_zk";
  else
    if  [[ "$POSTGRESQL_EXTERNAL" = false ]]; then
    run_as ${POSTGRES_USER} "bash ${DIR}/datafari-manager.sh start_postgres";
    fi
    run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh start_cassandra";
    waitCassandra;
  fi


  if [ "$(whoami)" == "root" ]; then
    bash ${DIR}/datafari-manager.sh start_apache
  else
    run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh start_apache"
  fi
  
  run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh start_mcf_crawler_agent";
  
  run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh start_solr";
  
  echo "Wait 5s that Solr registers to Zookeeper..."
  sleep 5
  
  if  [[ "$STATE" = *initialized* ]];
  then
    run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh init_solr";
    run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh init_solr_annotators";
    # TODO add parameter to activate or not
    run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh init_solr_rag";
  fi

  run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh start_tomcat";
  waitTomcat;
  run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh start_tomcat_mcf";
  waitTomcatMCF;
  
  if  [[ "$STATE" = *initialized* ]];
  then
    run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh init_mcf";   
    #act end start initialization
    run_as ${DATAFARI_USER} `sed -i 's/\(STATE *= *\).*/\1active/' $INIT_STATE_FILE`
  fi

  if  [[ "$TIKASERVER" = *true* ]];
  then
    cd $TIKA_SERVER_HOME/bin
    run_as ${DATAFARI_USER} "bash tika-server.sh start";
    cd $DIR
  fi
  
  if  [[ "$TIKASERVER_ANNOTATOR" = *true* ]];
  then
    cd $TIKA_SERVER_HOME_ANNOTATOR/bin
    run_as ${DATAFARI_USER} "bash tika-server.sh start";
    cd $DIR
  fi
  
  if  [[ "$TIKASERVER_OCR" = *true* ]];
  then
    cd $TIKA_SERVER_HOME_OCR/bin
    run_as ${DATAFARI_USER} "bash tika-server.sh start";
    cd $DIR
  fi
  
  if  [[ "$AnalyticsActivation" = *true* ]]; then
    #Run Analytic Stack
    cd $AS_HOME/scripts
    run_as ${DATAFARI_USER} "bash as-manager.sh start";
    cd $DIR
  fi

fi

if [ "$MONIT_STATE" == "active" ]; then
  sudo systemctl start monit
fi

echo "Check services state, wait 5 seconds that all services are started"
sleep 5
check_services
