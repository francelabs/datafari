#!/bin/bash -e
export DATAFARI_HOME=$(pwd)/..
export JAVA_HOME=${DATAFARI_HOME}/jvm
export LD_LIBRARY_PATH=${DATAFARI_HOME}/pgsql/lib
cd ${DATAFARI_HOME}/tomcat/bin
sh "shutdown.sh"
cd ${DATAFARI_HOME}/mcf/mcf_home
sh "stop-agents.sh"
su postgres -c "${DATAFARI_HOME}/pgsql/bin/pg_ctl -D ${DATAFARI_HOME}/pgsql/data -l ${DATAFARI_HOME}/logs/pgsql.log stop"
