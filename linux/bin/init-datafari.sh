#!/bin/bash

# Init Datafari
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
installerLog="/opt/datafari/logs/installer.log"
source "${DIR}/set-datafari-env.sh"
source "${DIR}/utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE


@VERSION-METHODS@


question_ip_node() {
    echo "Specify the IP of the current host: "
    read node_host
    set_property "NODEHOST" $node_host $CONFIG_FILE
}

question_solr_collection() {
    echo "What is the name of the main Solr Collection ? "
    read solr_main_collection
    set_property "SOLRMAINCOLLECTION" $solr_main_collection $CONFIG_FILE
}

question_solr_shards_number() {
    echo "enter the number of shards you want for your index:"
    read solr_shards_number
    set_property "SOLRNUMSHARDS" $solr_shards_number $CONFIG_FILE
}

question_datafari_password() {
    echo "enter the Datafari password:"
    read datafari_password
    set_property "TEMPADMINPASSWORD" $datafari_password $CONFIG_FILE
}

question_postgresql_password() {
    echo "enter the Postgresql password:"
    read postgresql_password
    set_property "TEMPPGSQLPASSWORD" $postgresql_password $CONFIG_FILE
}

question_start_datafari() {
	echo "Do you want Datafari to be started ? true/false"
	read start_datafari
}	
    
## Installer functions

getProperty() {
    awk -F'=' -v k="$1" '$1==k&&sub(/^[^=]*=/,"")' $2
}

delete_certificates() {
	# Delete dev SSL certificates

	chmod -R 775 /opt/datafari/ssl-keystore
	rm -f /opt/datafari/ssl-keystore/datafari-keystore.p12
	rm -f /opt/datafari/ssl-keystore/datafari-key.pem
	rm -f /opt/datafari/ssl-keystore/datafari-cert.pem
	rm -f /opt/datafari/ssl-keystore/datafari-cert.csr

	# Delete Apache certificates
	rm -f /opt/datafari/ssl-keystore/apache/datafari.csr
	rm -f /opt/datafari/ssl-keystore/apache/datafari.crt
	rm -f /opt/datafari/ssl-keystore/apache/datafari.key
}

init_war() {
	
	mkdir -p /opt/datafari/tomcat/webapps/Datafari
	mv /opt/datafari/tomcat/webapps/Datafari.war /opt/datafari/tomcat/webapps/Datafari
	unzip -qq /opt/datafari/tomcat/webapps/Datafari/Datafari.war -d /opt/datafari/tomcat/webapps/Datafari
	
	@WAR_VERSION_INIT@
}

init_war_mcf() {
	mv /opt/datafari/tomcat/webapps/Datafari /opt/datafari/tomcat/webapps/adminmcfdistant$1
}

init_git() {

	# Get Git commit id and version 

	file="/opt/datafari/tomcat/conf/git.properties"

	if [ -f "$file" ]; then
		while IFS='=' read -r key value; do
			if [[ $key == git.commit.id.abbrev ]]; then

				commit=$value
				echo $commit
			fi

			if [[ $key == git.build.version ]]; then

				version=$value
				echo $version
			fi
		done <"$file"
	fi
	sed -i -e "s/@VERSION@/$version/g" /opt/datafari/tomcat/webapps/Datafari/footer.jsp >>$installerLog 2>&1
	sed -i -e "s/@VERSION@/$version/g" /opt/datafari/tomcat/webapps/Datafari/admin/admin-footer.jsp >>$installerLog 2>&1
	sed -i -e "s/@COMMIT@/$commit/g" /opt/datafari/tomcat/webapps/Datafari/admin/admin-footer.jsp >>$installerLog 2>&1

}

init_elk() {
	#change elk address
	sed -i -e "s/localhost/${1}/g" /opt/datafari/tomcat/conf/elk.properties >>$installerLog 2>&1
	sed -i "/server.host:/c\server.host: 0.0.0.0" /opt/datafari/elk/kibana/config/kibana.yml >>$installerLog 2>&1
	
	# Init MetricBeat
	if [ "$2" = "mono" ]; then
		sed -i -e "s~@METRICBEAT-CONFIGURATION@~mono~g" /opt/datafari/elk/scripts/elk-manager.sh >>$installerLog 2>&1
	else
		sed -i -e "s~@METRICBEAT-CONFIGURATION@~multi~g" /opt/datafari/elk/scripts/elk-manager.sh >ƒ>$installerLog 2>&1
	fi

}

init_logstash() {
	sed -i -e "s/@ES_HOST@/$1:9200/g" /opt/datafari/elk/logstash/logstash-datafari.conf >>$installerLog 2>&1
}

init_elk_apache() {
	if [ -d /etc/apache2 ]; then
		cp /opt/datafari/elk/proxy/elk.conf /etc/apache2/sites-available/
		ln -s /etc/apache2/sites-available/elk.conf /etc/apache2/sites-enabled/elk.conf
	fi
}

generate_certificates() {
	# Generate SSL certificate for datafari
	$JAVA_HOME/bin/keytool -genkey -alias tomcat -keyalg RSA -keystore /opt/datafari/ssl-keystore/datafari-keystore.p12 -validity 9999 -storepass DataFariAdmin -keypass DataFariAdmin -dname "cn=${1}, ou=francelabs, o=francelabs, l=nice, st=paca, c=pa" -ext "SAN:c=DNS:localhost,IP:127.0.0.1,IP:${1}"
	$JAVA_HOME/bin/keytool -export -keystore /opt/datafari/ssl-keystore/datafari-keystore.p12 -storetype PKCS12 -alias tomcat -storepass DataFariAdmin -file /opt/datafari/ssl-keystore/datafari-cert.csr
	$JAVA_HOME/bin/keytool -import -keystore /opt/datafari/ssl-keystore/datafari-truststore.p12 -storetype PKCS12 -storepass DataFariAdmin -alias tomcat -noprompt -file /opt/datafari/ssl-keystore/datafari-cert.csr
}

generate_certificates_apache() {

	# Generate SSL certificate for Apache
	sed -i -e "s/@NODEHOST@/${1}/g" /opt/datafari/ssl-keystore/apache/config/datafari-config.csr >>$installerLog 2>&1
	sed -i -e "s/@NODEHOST@/${1}/g" /opt/datafari/ssl-keystore/apache/config/tomcat.conf >>$installerLog 2>&1
	sed -i -e "s/@NODEHOST@/${1}/g" /opt/datafari/ssl-keystore/apache/config/datafari-services.conf >>$installerLog 2>&1
	openssl req -config /opt/datafari/ssl-keystore/apache/config/datafari-config.csr -new -newkey rsa:2048 -nodes -keyout /opt/datafari/ssl-keystore/apache/datafari.key -out /opt/datafari/ssl-keystore/apache/datafari.csr
	openssl x509 -req -days 365 -in /opt/datafari/ssl-keystore/apache/datafari.csr -signkey /opt/datafari/ssl-keystore/apache/datafari.key -out /opt/datafari/ssl-keystore/apache/datafari.crt

}

generate_certificates_elk() {

	# Generate SSL certificate for Apache
	sed -i -e "s/@NODEHOST@/${1}/g" /opt/datafari/ssl-keystore/elk/config/datafari-config.csr >>$installerLog 2>&1
	openssl req -config /opt/datafari/ssl-keystore/elk/config/datafari-config.csr -new -newkey rsa:2048 -nodes -keyout /opt/datafari/ssl-keystore/elk/datafari-key.pem -out /opt/datafari/ssl-keystore/elk/datafari.csr
	openssl x509 -req -days 365 -in /opt/datafari/ssl-keystore/elk/datafari.csr -signkey /opt/datafari/ssl-keystore/elk/datafari-key.pem -out /opt/datafari/ssl-keystore/elk/datafari-cert.pem
	mv /opt/datafari/ssl-keystore/elk/datafari-key.pem /opt/datafari/elk/elasticsearch/config/
	mv /opt/datafari/ssl-keystore/elk/datafari-cert.pem /opt/datafari/elk/elasticsearch/config/

}

init_collection_name() {
	sed -i -e "s~@MAINCOLLECTION@~${1}~g" /opt/datafari/solr/solr_home/FileShare/core.properties >>$installerLog 2>&1
	sed -i -e "s~@MAINCOLLECTION@~${1}~g" /opt/datafari/solr/solr_home/FileShare/conf/customs_schema/addCustomSchemaInfo.sh >>$installerLog 2>&1
	sed -i -e "s~@MAINCOLLECTION@~${1}~g" /opt/datafari/bin/datafari-manager.sh >>$installerLog 2>&1
	sed -i -e "s~@MAINCOLLECTION@~${1}~g" /opt/datafari/tomcat/conf/datafari.properties >>$installerLog 2>&1
	sed -i -e "s~@MAINCOLLECTION@~${1}~g" /opt/datafari/bin/common/config/manifoldcf/init/outputconnections/DatafariSolr.json >>$installerLog 2>&1
	sed -i -e "s~@MAINCOLLECTION@~${1}~g" /opt/datafari/bin/common/config/manifoldcf/init/outputconnections/DatafariSolrNoTika.json >>$installerLog 2>&1
	sed -i -e "s~@MAINCOLLECTION@~${1}~g" /opt/datafari/tomcat/webapps/Datafari/resources/js/admin/ajax/alerts.js >>$installerLog 2>&1
	sed -i -e "s~@MAINCOLLECTION@~${1}~g" /opt/datafari/tomcat/webapps/Datafari/resources/js/parameters.js >>$installerLog 2>&1
	sed -i -e "s~@MAINCOLLECTION@~${1}~g" /opt/datafari/tomcat/webapps/Datafari/resources/js/admin/ajax/queryElevator.js >>$installerLog 2>&1
	sed -i -e "s~@MAINCOLLECTION@~${1}~g" /opt/datafari/tomcat/webapps/Datafari/resources/js/AjaxFranceLabs/modules/QueryElevator.module.js >>$installerLog 2>&1
	sed -i -e "s~@MAINCOLLECTION@~${1}~g" /opt/datafari/tomcat/webapps/Datafari/admin/ajax/SchemaAdmin.jsp >>$installerLog 2>&1
	sed -i -e "s~@MAINCOLLECTION@~${1}~g" /opt/datafari/tomcat/webapps/Datafari/admin/admin-sidebar.jsp >>$installerLog 2>&1

}

init_node_host() {
	sed -i -e "s/@NODEHOST@/${1}/g" /opt/datafari/solr/bin/solr.in.sh >>$installerLog 2>&1
	sed -i -e "s/@NODEHOST@/${1}/g" /opt/datafari/solr/server/etc/jetty.xml >>$installerLog 2>&1
	sed -i -e "s/@NODEHOST@/${1}/g" /opt/datafari/bin/zkUtils/reloadCollections.sh >>$installerLog 2>&1
	sed -i -e "s/@NODEHOST@/${1}/g" /opt/datafari/bin/zkUtils/init-solr-collections.sh >>$installerLog 2>&1
}

init_solr_node() {
	sed -i -e "s/@SOLRNODEIP@/${1}/g" /opt/datafari/bin/zkUtils/reloadCollections.sh >>$installerLog 2>&1
	sed -i -e "s/@SOLRNODEIP@/${1}/g" /opt/datafari/bin/zkUtils/init-solr-collections.sh >>$installerLog 2>&1
	sed -i -e "s/@SOLRNODEIP@/${1}/g" /opt/datafari/tomcat/conf/solr.properties >>$installerLog 2>&1
	sed -i -e "s/@SOLRNODEIP@/${1}/g" /opt/datafari/ssl-keystore/apache/config/tomcat.conf >>$installerLog 2>&1
	
		
}

init_solr_hosts() {
	sed -i -e "s/@SOLRHOSTS@/${1}/g" /opt/datafari/tomcat/conf/datafari.properties >>$installerLog 2>&1
	sed -i -e "s/@SOLRHOSTS@/${1}/g" /opt/datafari/solr/bin/solr.in.sh >>$installerLog 2>&1
	sed -i -e "s/@SOLRHOSTS@/${1}/g" /opt/datafari/bin/zkUtils/init-zk.sh >>$installerLog 2>&1
}

init_zk() {
	#sed -i -e "s/@NODEHOST@/${1}/g" /opt/datafari/tomcat/conf/datafari.properties >>$installerLog 2>&1
	sed -i -e "s/@ZKHOST@/${1}/g" /opt/datafari/bin/common/config/manifoldcf/init/outputconnections/DatafariSolr.json >>$installerLog 2>&1

	sed -i -e "s/@ZKHOST@/${1}/g" /opt/datafari/bin/common/config/manifoldcf/init/outputconnections/DatafariSolrNoTika.json >>$installerLog 2>&1
}

init_zk_data() {
	mkdir -p /opt/datafari/zookeeper/data
	touch /opt/datafari/zookeeper/data/myid
	echo "${1}" >>/opt/datafari/zookeeper/data/myid
}

init_zk_mcf() {
	sed -i -e "s/@ZKHOST-MCF@/localhost:2182/g" /opt/datafari/mcf/mcf_home/properties.xml >>$installerLog 2>&1
}

init_mcf() {
	sed -i -e "s/@MCFPROCESSID@/${1}/g" /opt/datafari/mcf/mcf_home/options.env.unix >>$installerLog 2>&1
}

init_shards() {
	sed -i -e "s/@NUMSHARDS@/${1}/g" /opt/datafari/tomcat/conf/datafari.properties >>$installerLog 2>&1
	sed -i -e "s/@NUMSHARDS@/${1}/g" /opt/datafari/bin/start-datafari.sh >>$installerLog 2>&1
}

init_main_node() {
	sed -i -e "s/@ISMAINNODE@/true/g" /opt/datafari/tomcat/conf/datafari.properties >>$installerLog 2>&1
	
}

init_solrcloud() {
	mkdir -p /opt/datafari/solr/solrcloud
	mv /opt/datafari/solr/solr_home/FileShare /opt/datafari/solr/solrcloud
	mv /opt/datafari/solr/solr_home/Statistics /opt/datafari/solr/solrcloud
	mv /opt/datafari/solr/solr_home/Promolink /opt/datafari/solr/solrcloud
	mv /opt/datafari/solr/solr_home/Entities /opt/datafari/solr/solrcloud
	mv /opt/datafari/solr/solr_home/Duplicates /opt/datafari/solr/solrcloud
	mkdir -p /opt/datafari/solr/solrcloud/FileShare/lib/custom/customer

}

init_folders() {
	mkdir -p /opt/datafari/logs/elk
	mkdir -p /opt/datafari/logs/tika-server
	mkdir -p /opt/datafari/bin/backup/
	mkdir -p /opt/datafari/bin/backup/cassandra
	mkdir -p /opt/datafari/bin/backup/datafari_conf
	mkdir -p /opt/datafari/bin/backup/mcf-script
	mkdir -p /opt/datafari/bin/backup/mcf
	mkdir -p /opt/datafari/bin/backup/solr
	mkdir -p /opt/datafari/solr/solr_home/FileShare/lib/custom/customer/
	
	
}

init_password() {
	apacheAdminUser=apacheadmin
	elkAdminUser=elkadmin
	solrAdminUser=solradmin
	password=${1}
	realm=datafari
	cd /opt/datafari/mcf/mcf_home/obfuscation-utility
	chmod -R 777 /opt/datafari/mcf/mcf_home/obfuscation-utility/obfuscate.sh
	sed -i -e "s~@PASSWORD@~$(./obfuscate.sh ${1})~g" /opt/datafari/mcf/mcf_home/properties-global.xml >>$installerLog 2>&1
	sed -i -e "s/@TEMPADMINPASSWORD@/${1}/g" /opt/datafari/tomcat/conf/datafari.properties >>$installerLog 2>&1
	sed -i -e "s~@MCF_ADMIN_PASSWORD@~${1}~g" /opt/datafari/bin/purgeUtils/vacuum-mcf.sh >>$installerLog 2>&1
	digestAdminUser="$( printf "%s:%s:%s" "$apacheAdminUser" "$realm" "$password" | md5sum | awk '{print $1}' )"
	digestElkUser="$( printf "%s:%s:%s" "$elkAdminUser" "$realm" "$password" | md5sum | awk '{print $1}' )"
	digestSolrUser="$( printf "%s:%s:%s" "$solrAdminUser" "$realm" "$password" | md5sum | awk '{print $1}' )"
	printf "%s:%s:%s\n" "$apacheAdminUser" "$realm" "$digestAdminUser" >> "/opt/datafari/apache/password/htpasswd"
	printf "%s:%s:%s\n" "$elkAdminUser" "$realm" "$digestElkUser" >> "/opt/datafari/apache/password/htpasswd"
	printf "%s:%s:%s\n" "$solrAdminUser" "$realm" "$digestSolrUser" >> "/opt/datafari/apache/password/htpasswd"
}

init_password_postgresql() {
	sed -i -e "s~@POSTGRESPASSWORD@~$(./obfuscate.sh ${1})~g" /opt/datafari/mcf/mcf_home/properties-global.xml >>$installerLog 2>&1
	sed -i -e "s~@POSTGRESPASSWORD@~$(./obfuscate.sh ${1})~g" /opt/datafari/tomcat/conf/mcf-postgres.properties >>$installerLog 2>&1
	sed -i -e "s~@POSTGRESPASSWORD@~${1}~g" /opt/datafari/pgsql/pwd.conf >>$installerLog 2>&1
}

init_apache_ssl() {	
	getMCF=""
	getMCFSimplified=""
	getSolrAdmin=""
		apachePresent="true"
		getMCFAdmin="\"/datafari-mcf-crawler-ui/\""
		getMCF="\"/datafari-mcf-crawler-ui/\""
		getMCFSimplified="\"/datafari-mcf-crawler-ui/index.jsp?p=showjobstatus.jsp\""
		getSolrAdmin="\"/solr/\""
		sed -i -e "s/@APACHE@/true/g" /opt/datafari/tomcat/conf/datafari.properties >>$installerLog 2>&1
		cp -r /opt/datafari/apache/html/* /var/www/html/

		if [ -d /etc/apache2 ]; then
			cp /opt/datafari/ssl-keystore/apache/config/tomcat.conf /etc/apache2/sites-available/
			cp /opt/datafari/ssl-keystore/apache/config/envvars /etc/apache2/
			ln -s /etc/apache2/* /opt/datafari/apache/
			rm -f /var/www/html/index.jsp
			mkdir /var/apache
			mkdir /var/apache/logs
			ln -s /var/apache/logs /opt/datafari/logs/apache
			a2enmod proxy
			a2enmod proxy_ajp
			a2enmod proxy_http
			a2enmod ssl
			a2enmod proxy_http
			a2enmod auth_digest
			a2enmod rewrite
			a2enmod headers
			a2dissite 000-default
			a2dissite default-ssl
			a2ensite tomcat
			/etc/init.d/apache2 start
			/etc/init.d/apache2 stop
			update-rc.d apache2 disable
			
		elif [ -d /etc/httpd ]; then
			cp /opt/datafari/ssl-keystore/apache/config/httpd.conf /etc/httpd/conf/
			mkdir /etc/httpd/sites-available /etc/httpd/sites-enabled
			mkdir /var/apache
			mkdir /var/apache/pid
			mkdir /var/apache/logs
			chmod -R 775 /var/apache
			chown -R datafari /var/apache
			cp /opt/datafari/ssl-keystore/apache/config/tomcat.conf /etc/httpd/sites-available/
			ln -s /etc/httpd/sites-available/tomcat.conf /etc/httpd/sites-enabled/tomcat.conf
			ln -s /var/apache/pid /opt/datafari/pid/apache
			ln -s /var/apache/logs /opt/datafari/logs/apache
			cp /opt/datafari/ssl-keystore/apache/config/envvars /etc/httpd/
			ln -s /etc/httpd/* /opt/datafari/apache/
			rm -f /var/www/html/index.jsp
			/usr/sbin/setsebool -P httpd_can_network_connect 1
			apachectl start
			apachectl stop
			
		fi
	sed -i -e "s~\"@GET-MCF-IP@\"~${getMCFAdmin}~g" /opt/datafari/tomcat/webapps/Datafari/admin/admin-sidebar.jsp >>$installerLog 2>&1
	sed -i -e "s~\"@GET-MCF-IP@\"~${getMCF}~g" /opt/datafari/tomcat/webapps/Datafari/resources/js/header.js >>$installerLog 2>&1
	sed -i -e "s~\"@GET-MCF-IP@\"~${getMCFSimplified}~g" /opt/datafari/tomcat/webapps/Datafari/resources/js/admin/ajax/mcfSimplified.js >>$installerLog 2>&1
	
	
	sed -i -e "s~\"@GET-SOLR-IP@\"~${getSolrAdmin}~g" /opt/datafari/tomcat/webapps/Datafari/admin/admin-sidebar.jsp >>$installerLog 2>&1
	sed -i -e "s/@APACHE-PRESENT@/${apachePresent}/g" /opt/datafari/tomcat/webapps/Datafari/admin/admin-sidebar.jsp >>$installerLog 2>&1
	
}

clean_monoserver_node() {
	rm -rf /opt/datafari/bin/start-solr.sh
	rm -rf /opt/datafari/bin/stop-solr.sh
	rm -rf /opt/datafari/bin/start-mcf.sh
	rm -rf /opt/datafari/bin/stop-mcf.sh
	rm -rf /opt/datafari/bin/start-zk.sh
	rm -rf /opt/datafari/bin/stop-zk.sh
	rm -rf /opt/datafari/bin/start-zk-mcf.sh
	rm -rf /opt/datafari/bin/stop-zk-mcf.sh
	rm -rf /opt/datafari/bin/start-visilia.sh
	rm -rf /opt/datafari/bin/cluster_init.sh
}

init_permissions() {
	echo "Init permissions of Datafari. Please wait"
	mkdir /opt/datafari/tmp
	chmod -R 775 /opt/datafari
	chown -R datafari /opt/datafari
	chown -R postgres /opt/datafari/pgsql/
	chmod -R 700 /opt/datafari/pgsql/
	chmod -R 777 /opt/datafari/pid
	chmod -R 777 /opt/datafari/logs
	chmod -R 755 /opt/datafari/elk
	if [ -d /etc/apache2 ]; then
		chown -R datafari /etc/apache2
		chmod -R 775 /etc/apache2
	elif [ -d /etc/httpd ]; then
		echo 'datafari ALL=NOPASSWD:/sbin/apachectl' >> /etc/sudoers
		chown -R datafari /etc/httpd
		chmod -R 775 /etc/httpd
	fi
	
}

init_permissions_file_datafari_properties() {
sleep 30
echo "cht permissions datafari properties"
	chmod -R 775 /opt/datafari/tomcat/conf/datafari.properties
	chown datafari /opt/datafari/tomcat/conf/datafari.properties
}

init_users() {
	id -u postgres >/dev/null 2>&1 || useradd postgres
	useradd datafari -m -s /bin/bash
	if [ -d /etc/apache2 ]; then
		usermod -aG sudo datafari
	elif [ -d /etc/httpd ]; then
		usermod -aG wheel datafari
	fi
	echo 'datafari ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers
}

secure_tomcat() { 
	iptables -A INPUT -p tcp -s 127.0.0.1 --dport 8080 -j ACCEPT
	if [ "$NODETYPE" == "main" ]; then
		for ((i=1;i<=$solrNodesNumber;i++)); do

			solrvalue=solr$i
			solrproperty=$(echo $solrvalue)
			solrServer=`getProperty $solrproperty $CONFIG_FILE`

			iptables -A INPUT -p tcp -s ${solrServer} --dport 8080 -j ACCEPT
		done
	
		if [ $mcfNodesNumber -ne 0 ]; then
			for ((i=1;i<=$mcfNodesNumber;i++)); do

				mcfvalue=mcf$i
				mcfproperty=$(echo $mcfvalue)
				mcfServer=`getProperty $mcfproperty $CONFIG_FILE`
				iptables -A INPUT -p tcp -s ${mcfServer} --dport 8080 -j ACCEPT
	
			done
		fi
	fi
	iptables -A INPUT -p tcp -s ${1} --dport 8080 -j ACCEPT
	iptables -A INPUT -p tcp --dport 8080 -j DROP
	
	iptables -A INPUT -p tcp -s 127.0.0.1 --dport 8009 -j ACCEPT
	if [ "$NODETYPE" == "main" ]; then
		for ((i=1;i<=$solrNodesNumber;i++)); do

			solrvalue=solr$i
			solrproperty=$(echo $solrvalue)
			solrServer=`getProperty $solrproperty $CONFIG_FILE`

			iptables -A INPUT -p tcp -s ${solrServer} --dport 8009 -j ACCEPT
		done
	
		if [ $mcfNodesNumber -ne 0 ]; then
			for i in "${mcfNodesNumber}"; do

				mcfvalue=mcf$i
				mcfproperty=$(echo $mcfvalue)
				mcfServer=`getProperty $mcfproperty $CONFIG_FILE`
				iptables -A INPUT -p tcp -s ${mcfServer} --dport 8009 -j ACCEPT
	
			done
		fi
	fi
	iptables -A INPUT -p tcp -s ${1} --dport 8009 -j ACCEPT
	iptables -A INPUT -p tcp --dport 8009 -j DROP
	
	iptables -A INPUT -p tcp -s 127.0.0.1 --dport 8443 -j ACCEPT
	if [ "$NODETYPE" == "main" ]; then
		for ((i=1;i<=$solrNodesNumber;i++)); do

			solrvalue=solr$i
			solrproperty=$(echo $solrvalue)
			solrServer=`getProperty $solrproperty $CONFIG_FILE`

			iptables -A INPUT -p tcp -s ${solrServer} --dport 8443 -j ACCEPT
		done
	
		if [ $mcfNodesNumber -ne 0 ]; then
			for ((i=1;i<=$mcfNodesNumber;i++)); do

				mcfvalue=mcf$i
				mcfproperty=$(echo $mcfvalue)
				mcfServer=`getProperty $mcfproperty $CONFIG_FILE`
				iptables -A INPUT -p tcp -s ${mcfServer} --dport 8443 -j ACCEPT
	
			done
		fi
	fi
	iptables -A INPUT -p tcp -s ${1} --dport 8443 -j ACCEPT
	iptables -A INPUT -p tcp --dport 8443 -j DROP
}




secure_tomcat_mcf() {
	iptables -A INPUT -p tcp -s 127.0.0.1 --dport 9080 -j ACCEPT
	if [ "$NODETYPE" == "main" ]; then
		for ((i=1;i<=$solrNodesNumber;i++)); do

			solrvalue=solr$i
			solrproperty=$(echo $solrvalue)
			solrServer=`getProperty $solrproperty $CONFIG_FILE`

			iptables -A INPUT -p tcp -s ${solrServer} --dport 9080 -j ACCEPT
		done
	
		if [ $mcfNodesNumber -ne 0 ]; then
			for ((i=1;i<=$mcfNodesNumber;i++)); do

				mcfvalue=mcf$i
				mcfproperty=$(echo $mcfvalue)
				mcfServer=`getProperty $mcfproperty $CONFIG_FILE`
				iptables -A INPUT -p tcp -s ${mcfServer} --dport 9080 -j ACCEPT
	
			done
		fi
	fi
	iptables -A INPUT -p tcp -s ${1} --dport 9080 -j ACCEPT
	iptables -A INPUT -p tcp --dport 9080 -j DROP
	
	iptables -A INPUT -p tcp -s 127.0.0.1 --dport 9009 -j ACCEPT
	if [ "$NODETYPE" == "main" ]; then
		for ((i=1;i<=$solrNodesNumber;i++)); do

			solrvalue=solr$i
			solrproperty=$(echo $solrvalue)
			solrServer=`getProperty $solrproperty $CONFIG_FILE`

			iptables -A INPUT -p tcp -s ${solrServer} --dport 9009 -j ACCEPT
		done
	
		if [ $mcfNodesNumber -ne 0 ]; then
			for ((i=1;i<=$mcfNodesNumber;i++)); do

				mcfvalue=mcf$i
				mcfproperty=$(echo $mcfvalue)
				mcfServer=`getProperty $mcfproperty $CONFIG_FILE`
				iptables -A INPUT -p tcp -s ${mcfServer} --dport 9009 -j ACCEPT
	
			done
		fi
	fi
	iptables -A INPUT -p tcp -s ${1} --dport 9009 -j ACCEPT
	iptables -A INPUT -p tcp --dport 9009 -j DROP

	iptables -A INPUT -p tcp -s 127.0.0.1 --dport 9443 -j ACCEPT
	if [ "$NODETYPE" == "main" ]; then
		for ((i=1;i<=$solrNodesNumber;i++)); do

			solrvalue=solr$i
			solrproperty=$(echo $solrvalue)
			solrServer=`getProperty $solrproperty $CONFIG_FILE`

			iptables -A INPUT -p tcp -s ${solrServer} --dport 9443 -j ACCEPT
		done
	
		if [ $mcfNodesNumber -ne 0 ]; then
			for ((i=1;i<=$mcfNodesNumber;i++)); do

				mcfvalue=mcf$i
				mcfproperty=$(echo $mcfvalue)
				mcfServer=`getProperty $mcfproperty $CONFIG_FILE`
				iptables -A INPUT -p tcp -s ${mcfServer} --dport 9443 -j ACCEPT
	
			done
		fi
	fi
	iptables -A INPUT -p tcp -s ${1} --dport 9443 -j ACCEPT
	iptables -A INPUT -p tcp --dport 9443 -j DROP
	
}

secure_zk_solr() {
iptables -A INPUT -p tcp -s 127.0.0.1 --dport 2181 -j ACCEPT
	
		
IFS=', ' read -r -a array <<<"$SOLRHOSTS"
	for index in "${!array[@]}"; do
		echo "$index ${array[index]}"
		var2=${array[index]}
		var2=${var2%:2181}
		iptables -A INPUT -p tcp -s ${var2} --dport 2181 -j ACCEPT
	done
	
		if [ $mcfNodesNumber -ne 0 ]; then
			IFS=', ' read -r -a array <<<"$MCFHOSTS"
	for index in "${!array[@]}"; do
		echo "$index ${array[index]}"
		var2=${array[index]}
		iptables -A INPUT -p tcp -s ${var2} --dport 2181 -j ACCEPT
	done
		fi
	
	iptables -A INPUT -p tcp -s $MAINNODEHOST --dport 2181 -j ACCEPT
	iptables -A INPUT -p tcp -s ${1} --dport 2181 -j ACCEPT
	iptables -A INPUT -p tcp --dport 2181 -j DROP
	
	iptables -A INPUT -p tcp -s 127.0.0.1 --dport 2888 -j ACCEPT
	
		IFS=', ' read -r -a array <<<"$SOLRHOSTS"
	for index in "${!array[@]}"; do
		echo "$index ${array[index]}"
		var2=${array[index]}
		var2=${var2%:2181}
		iptables -A INPUT -p tcp -s ${var2} --dport 2888 -j ACCEPT
	done
	
		if [ $mcfNodesNumber -ne 0 ]; then
			IFS=', ' read -r -a array <<<"$MCFHOSTS"
	for index in "${!array[@]}"; do
		echo "$index ${array[index]}"
		var2=${array[index]}
		iptables -A INPUT -p tcp -s ${var2} --dport 2888 -j ACCEPT
	done
		fi
	
	iptables -A INPUT -p tcp -s $MAINNODEHOST --dport 2888 -j ACCEPT
	iptables -A INPUT -p tcp -s ${1} --dport 2888 -j ACCEPT
	
	iptables -A INPUT -p tcp --dport 2888 -j DROP
	
	iptables -A INPUT -p tcp -s 127.0.0.1 --dport 3888 -j ACCEPT
	
		IFS=', ' read -r -a array <<<"$SOLRHOSTS"
	for index in "${!array[@]}"; do
		echo "$index ${array[index]}"
		var2=${array[index]}
		var2=${var2%:2181}
		iptables -A INPUT -p tcp -s ${var2} --dport 3888 -j ACCEPT
	done
	
		if [ $mcfNodesNumber -ne 0 ]; then
			IFS=', ' read -r -a array <<<"$MCFHOSTS"
	for index in "${!array[@]}"; do
		echo "$index ${array[index]}"
		var2=${array[index]}
		iptables -A INPUT -p tcp -s ${var2} --dport 3888 -j ACCEPT
	done
		fi
	
	iptables -A INPUT -p tcp -s $MAINNODEHOST --dport 3888 -j ACCEPT
	iptables -A INPUT -p tcp -s ${1} --dport 3888 -j ACCEPT
	
	iptables -A INPUT -p tcp --dport 3888 -j DROP
	

}

secure_elk() { 
	iptables -A INPUT -p tcp -s 127.0.0.1 --dport 9200 -j ACCEPT
	if [ "$NODETYPE" == "main" ]; then
		for ((i=1;i<=$solrNodesNumber;i++)); do

			solrvalue=solr$i
			solrproperty=$(echo $solrvalue)
			solrServer=`getProperty $solrproperty $CONFIG_FILE`

			iptables -A INPUT -p tcp -s ${solrServer} --dport 9200 -j ACCEPT
		done
	
		if [ $mcfNodesNumber -ne 0 ]; then
			for ((i=1;i<=$mcfNodesNumber;i++)); do

				mcfvalue=mcf$i
				mcfproperty=$(echo $mcfvalue)
				mcfServer=`getProperty $mcfproperty $CONFIG_FILE`
				iptables -A INPUT -p tcp -s ${mcfServer} --dport 9200 -j ACCEPT
	
			done
		fi
	fi
	iptables -A INPUT -p tcp -s ${1} --dport 9200 -j ACCEPT
	iptables -A INPUT -p tcp --dport 9200 -j DROP
	
}

stop_firewalld_start_iptables() {
	systemctl stop firewalld
	systemctl disable firewalld
	systemctl mask firewalld
	systemctl enable iptables
	systemctl start iptables
	iptables -F
}
save_iptables_rules(){ 

	if [ -d /etc/apache2 ]; then
		iptables-save > /etc/iptables/rules.v4
	elif [ -d /etc/httpd ]; then
		
		service iptables save
	fi
}


########
# initialization node types
   
initialization_monoserver() {
    echo "initialization for monoserver"
    localip=localhost
    #delete_certificates
    init_users
  init_war
  init_git
  init_folders
    init_elk localhost "mono"
    init_logstash localhost
    generate_certificates $NODEHOST
  generate_certificates_apache $NODEHOST
  generate_certificates_elk $NODEHOST
    init_collection_name $SOLRMAINCOLLECTION
    init_node_host $NODEHOST
    
  init_solr_node $localip
  init_solr_hosts $localip
  init_zk $localip
  init_zk_mcf
  init_mcf "A"
    init_shards $SOLRNUMSHARDS
    init_main_node
  init_solrcloud
    clean_monoserver_node
    init_password $TEMPADMINPASSWORD
    init_password_postgresql $TEMPPGSQLPASSWORD
    init_apache_ssl
    if [ -d /etc/httpd ]; then
      stop_firewalld_start_iptables
    fi
    secure_tomcat $NODEHOST
  secure_tomcat_mcf $NODEHOST
  #secure_elk $NODEHOST
  save_iptables_rules
  
  init_permissions
  @INIT-ANNOTATOR@
  sed -i 's/\(STATE *= *\).*/\1initialized/' $INIT_STATE_FILE
  init_permissions_file_datafari_properties

}


####


### Init Datafari main function

init_datafari() {

if [ "$(whoami)" != "root" ]; then
        echo "Script must be run as user: root"
        echo "Script will exit"
        exit 1
fi

if  [[ "$STATE" = *initialized* ]];
		then
			echo "Datafari is already initialized. You can start Datafari directly."
			echo "If you want to reinitialize Datafari, edit the file /opt/datafari/bin/common/init_state.properties and replace the content by that : STATE=installed "
			echo "The script will now exit"
			exit 0
fi		
		
check_java;
is_file_present $CONFIG_FILE
is_variable_set $INSTALLER_TYPE
if [ "$INSTALLER_TYPE" == "interactive" ]; then
    echo "Interactive installer mode. You need to answer some questions to initialize Datafari"
    interactive_questions
fi
source $CONFIG_FILE
is_variable_set $NODETYPE
if [ "$NODETYPE" == "monoserver" ]; then
    echo "Monoserver initialization"
    echo "check of the variables of the properties file are set"
    echo "nodehost check"
    is_variable_set $NODEHOST
    echo "numshards check"
    is_variable_set $SOLRNUMSHARDS
    echo "solrhosts check"
	is_variable_set $SOLRHOSTS
    echo "maincollection check"
    is_variable_set $SOLRMAINCOLLECTION
	echo "datafari password check"
    is_variable_set $TEMPADMINPASSWORD
	echo "postgresql password check"
    is_variable_set $TEMPPGSQLPASSWORD
    echo "elk activation check"
    is_variable_set $ELKactivation
    
	echo "Check complete."

    initialization_monoserver
fi

@VERSION-INIT@

#is_variable_set $NODEHOST
#is_variable_set $MAINNODEHOST
#is_variable_set $SOLRNUMSHARDS
#is_variable_set $SOLRHOSTS
#is_variable_set $SOLRMAINCOLLECTION
echo "Initialization done. You can start Datafari :"

if [ "$INSTALLER_TYPE" == "interactive" ] && [ "$NODETYPE" == "monoserver" ]; then
    question_start_datafari
    if [ "$start_datafari" == "true" ]; then
    	cd $DIR
    	bash start-datafari.sh
    fi
fi



}


init_datafari;
