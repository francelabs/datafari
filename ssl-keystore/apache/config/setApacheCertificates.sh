#!/bin/bash -e
#
#
# Set SSL certificates for Apache
#
#

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/../../../bin/set-datafari-env.sh"
source "${DIR}/../../../bin/utils.sh"


if [ "$(whoami)" != "$DATAFARI_USER" ]; then
        echo "Script must be run as user: $DATAFARI_USER"
        echo "Script will exit"
        exit 1
fi


pathCertificates=${DIR}/../../customerCertificates
certificatefile=${pathCertificates}/certificate.crt
certificatekeyfile=${pathCertificates}/certificateKey.key

echo "Change of Apache certificates"

cp $certificatefile ${DIR}/../datafari.crt
cp $certificatekeyfile ${DIR}/../datafari.key

echo "Certificates changed. Restart of Apache needed"
#cd /opt/datafari/bin/monitorUtils
#bash monit-stop-apache.sh
#bash monit-start-apache.sh
#echo "Restart done"
