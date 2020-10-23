#!/bin/bash -e
#
#
# Set SSL certificates for Apache
#
#
pathCertificates=/opt/datafari/ssl-keystore/customerCertificates
certificatefile=${pathCertificates}/certificate.crt
certificatekeyfile=${pathCertificates}/certificateKey.key
elasticsearchConfFolder=/opt/datafari/elk/elasticsearch/config
elasticsearchConfFile=$elasticsearchConfFolder/elasticsearch.yml
kibanaConfFolder=/opt/datafari/elk/kibana/config
kibanaConfFile=$kibanaConfFolder/kibana.yml
customerCertificateName=customerCertificate-cert.pem
customerCertificateKey=customerCertificate-key.pem
cp $certificatefile $elasticsearchConfFolder/$customerCertificateName
cp $certificatekeyfile $elasticsearchConfFolder/$customerCertificateKey


if [ "$(whoami)" != "datafari" ]; then
        echo "Script must be run as user: datafari"
        echo "Script will exit"
        exit 1
fi

# replace into ES config
sed -i "s|\(opendistro_security.ssl.transport.pemcert_filepath: *\).*|\1${customerCertificateName}|" ${elasticsearchConfFile}
sed -i "s|\(opendistro_security.ssl.transport.pemkey_filepath: *\).*|\1${customerCertificateKey}|" ${elasticsearchConfFile}
sed -i "s|\(opendistro_security.ssl.transport.pemtrustedcas_filepath: *\).*|\1${customerCertificateName}|" ${elasticsearchConfFile}
sed -i "s|\(opendistro_security.ssl.http.pemcert_filepath: *\).*|\1${customerCertificateName}|" ${elasticsearchConfFile}
sed -i "s|\(opendistro_security.ssl.http.pemkey_filepath: *\).*|\1${customerCertificateKey}|" ${elasticsearchConfFile}
sed -i "s|\(opendistro_security.ssl.http.pemtrustedcas_filepath: *\).*|\1${customerCertificateName}|" ${elasticsearchConfFile}

# replace into Kibana config
sed -i "s|\(server.ssl.certificate: *\).*|\1${customerCertificateName}|" ${kibanaConfFile}
sed -i "s|\(server.ssl.key: *\).*|\1${customerCertificateKey}|" ${kibanaConfFile}

cd /opt/datafari/elk/scripts
bash elk-manager.sh stop
sleep 5
bash elk-manager.sh start
