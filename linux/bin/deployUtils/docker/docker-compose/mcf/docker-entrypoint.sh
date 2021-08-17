#!/bin/bash

echo "script init mcf"
DIR="/opt/datafari/mcf/mcf_home/mcf_home"
if [ -d "$DIR" ]; then
  chmod -R 777 $DIR  
  cd $DIR
bash setglobalproperties.sh  
bash initialize.sh
bash start-agents.sh &
echo "init MCF done"
fi
$CATALINA_HOME/bin/catalina.sh run
