#!/bin/bash -e
#
#
# Set SSL certificates for Apache
#
#

elasticsearchConfFolder=/opt/datafari/elk/elasticsearch/config
elasticsearchConfFile=$elasticsearchConfFolder/elasticsearch.yml
kibanaConfFolder=/opt/datafari/elk/kibana/config
kibanaConfFile=$kibanaConfFolder/kibana.yml
defaultCertificateName=datafari-cert.pem
defaultCertificateKey=datafari-key.pem



if [ "$(whoami)" != "datafari" ]; then
        echo "Script must be run as user: datafari"
        echo "Script will exit"
        exit 1
fi

# replace into ES config
sed -i "s|\(opendistro_security.ssl.transport.pemcert_filepath: *\).*|\1${defaultCertificateName}|" ${elasticsearchConfFile}
sed -i "s|\(opendistro_security.ssl.transport.pemkey_filepath: *\).*|\1${defaultCertificateKey}|" ${elasticsearchConfFile}
sed -i "s|\(opendistro_security.ssl.transport.pemtrustedcas_filepath: *\).*|\1${defaultCertificateName}|" ${elasticsearchConfFile}
sed -i "s|\(opendistro_security.ssl.http.pemcert_filepath: *\).*|\1${defaultCertificateName}|" ${elasticsearchConfFile}
sed -i "s|\(opendistro_security.ssl.http.pemkey_filepath: *\).*|\1${defaultCertificateKey}|" ${elasticsearchConfFile}
sed -i "s|\(opendistro_security.ssl.http.pemtrustedcas_filepath: *\).*|\1${defaultCertificateName}|" ${elasticsearchConfFile}

# replace into Kibana config
sed -i "s|\(server.ssl.certificate: *\).*|\1${defaultCertificateName}|" ${kibanaConfFile}
sed -i "s|\(server.ssl.key: *\).*|\1${defaultCertificateKey}|" ${kibanaConfFile}

cd /opt/datafari/elk/scripts
bash elk-manager.sh stop
sleep 5
bash elk-manager.sh start
