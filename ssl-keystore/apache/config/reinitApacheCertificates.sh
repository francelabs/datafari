#!/bin/bash -e
#
#
# Reinit SSL certificates for Apache
#
#
defaultpathCertificates=/opt/datafari/ssl-keystore/apache
apacheConfFile=/opt/datafari/apache/sites-available/tomcat.conf
defaultcertificatefile=${defaultpathCertificates}/datafari.crt
defaultcertificatekeyfile=${defaultpathCertificates}/datafari.key

sed -i "s|\(SSLCertificateFile *\).*|\1${defaultcertificatefile}|" ${apacheConfFile}
sed -i "s|\(SSLCertificateKeyFile *\).*|\1${defaultcertificatekeyfile}|" ${apacheConfFile}

if [ -d /etc/apache2 ]; then
	/etc/init.d/apache2 stop
    /etc/init.d/apache2 start
elif [ -d /etc/httpd ]; then
	apachectl stop
    apachectl start
fi
