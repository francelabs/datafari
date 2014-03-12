#!/bin/bash -e
export DATAFARI_HOME=$(pwd)/..
export JAVA_HOME=${DATAFARI_HOME}/jvm
export LD_LIBRARY_PATH=${DATAFARI_HOME}/pgsql/lib
export CONFIG_FILE=${DATAFARI_HOME}/bin/common/init_state.properties
source $CONFIG_FILE
if  [ $STATE = "installed" ];
then 
rm -rf "${DATAFARI_HOME}/pgsql/data"
mkdir "${DATAFARI_HOME}/pgsql/data"
id -u postgres &>/dev/null || useradd postgres
chown -R postgres /opt/datafari/pgsql
chmod -R 777 /opt/datafari/logs
su postgres -c "${DATAFARI_HOME}/pgsql/bin/initdb -U postgres -A password --pwfile=${DATAFARI_HOME}/pgsql/pwd.conf -E utf8 -D ${DATAFARI_HOME}/pgsql/data"
su postgres -c "${DATAFARI_HOME}/pgsql/bin/pg_ctl -D ${DATAFARI_HOME}/pgsql/data -l ${DATAFARI_HOME}/logs/pgsql.log start"
cd "${DATAFARI_HOME}/mcf/mcf_home"
bash "initialize.sh"
else
su postgres -c "${DATAFARI_HOME}/pgsql/bin/pg_ctl -D ${DATAFARI_HOME}/pgsql/data -l ${DATAFARI_HOME}/logs/pgsql.log start"
fi
cd "${DATAFARI_HOME}/tomcat/bin"
bash "startup.sh"
if  [ $STATE = "installed" ];
then 
cd "${DATAFARI_HOME}/bin/common"
"${DATAFARI_HOME}/jvm/bin/java" -cp DatafariScripts.jar com.francelabs.datafari.script.BackupManifoldCFConnectorsScript RESTORE config/manifoldcf
sed -i "s/\(STATE *= *\).*/\1initialized/" $CONFIG_FILE
fi
cd "${DATAFARI_HOME}/mcf/mcf_home"
bash "lock-clean.sh"
bash "start-agents.sh" &
sleep 3

