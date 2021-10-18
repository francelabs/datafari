#!/bin/bash -e
#
#
# Factory Reset Datafari
#
#

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/../bin/set-datafari-env.sh"
source "${DIR}/../bin/utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE

if [ "$(whoami)" != "root" ]; then
        echo "Script must be run as user: root"
        echo "Script will exit"
        exit 1
fi

echo "This script will restore Datafato to factory setting."
echo "You will loose all your data."
echo "Datafari must be stopped"
read -p "Are you sure that you want to reinitialize COMPLETELY Datafari : all data will be lost (YES/NO) [NO]" factoryreset
factoryreset=${factoryreset:-NO}

if [ "$factoryreset" == "YES" ]; then
		cd $DIR/..
        find * -maxdepth 0 -name 'recovery_do_not_touch' -prune -o -exec rm -rf '{}' ';'
        unzip recovery_do_not_touch/datafarifactory.zip
fi
	

