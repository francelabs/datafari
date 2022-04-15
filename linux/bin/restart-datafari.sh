#!/bin/bash -e
#
#
# Restart script for Datafari
#
#

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source "${DIR}/set-datafari-env.sh"
source "${DIR}/utils.sh"
source $CONFIG_FILE

echo "Restart of Datafari"
errors=""
# TODO add variable for check if Monit is installed


#monoserver case
if [ "$NODETYPE" != "main" ]; then
  echo "Check status of MCF jobs"
  cd $DIR/monitorUtils/
  bash check_jobs_mcf.sh
  if [[ $? -eq 1 ]] && [[ $1 != "force" ]] ; then
    echo "MCF jobs still active, please stop or pause them before restarting Datafari. Or you can force restart with force option at the execution of the script"
    echo "END OF SCRIPT"
    exit 1
  fi
  sleep 10
  cd $DIR
  echo "1/2 Stop Datafari"
  bash stop-datafari.sh
  echo "2/2 Start Datafari"
  sleep 2
  bash start-datafari.sh  
fi
if [ -z "$errors" ]
then
  echo $errors
fi
echo "Please wait 10 seconds before the end of the restart"
sleep 10
echo "END OF SCRIPT"
