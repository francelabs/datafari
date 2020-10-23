# Turn off the brp-python-bytecompile script
%global __os_install_post %(echo '%{__os_install_post}' | sed -e 's!/usr/lib[^[:space:]]*/brp-python-bytecompile[[:space:]].*$!!g')

# Turn off the brp-python-bytecompile script
%global __os_install_post %(echo '%{__os_install_post}' | sed -e 's!/usr/lib[^[:space:]]*/brp-java-repack-jars[[:space:]].*$!!g')

Summary: Datafari search solution
Name: datafari
Version: 5.0dev
Release: 1
Group: Applications/File
License: Apache
AutoReqProv: 0
Source: datafari.zip
%description
Datafari search solution

%prep
cd $RPM_BUILD_DIR
rm -rf datafari
unzip -qq $RPM_SOURCE_DIR/datafari.zip

%build

%install
mkdir -p $RPM_BUILD_ROOT/opt/datafari
cp -R datafari/* $RPM_BUILD_ROOT/opt/datafari

%clean
rm -rf $RPM_BUILD_ROOT

%pre
# Add user/group here if needed

%post
useradd datafari -m -s /bin/bash
chmod -R 775 /opt/datafari

%preun
if [ -d /opt/datafari ]; then
	tmp_dir="/var/tmp/datafari"
	if [ ! -d "$tmp_dir" ];
	then
		mkdir "$tmp_dir"
	else
		rm -rf "$tmp_dir"/*
	fi
	tmp_common="$tmp_dir/bin/common"
	mkdir -p "$tmp_common"
	cp /opt/datafari/bin/common/init_state.properties "$tmp_common"
	tmp_solr_conf="$tmp_dir/solr/solrcloud/FileShare/conf"
	mkdir -p "$tmp_solr_conf"
	cp -R /opt/datafari/solr/solrcloud/FileShare/conf/customs* "$tmp_solr_conf"
	cp /opt/datafari/solr/solrcloud/FileShare/conf/stopwords* "$tmp_solr_conf"
	cp /opt/datafari/solr/solrcloud/FileShare/conf/synonyms* "$tmp_solr_conf"
	cp /opt/datafari/solr/solrcloud/FileShare/conf/protwords* "$tmp_solr_conf"
	
	if [ -L /etc/apache2/sites-enabled/elk.conf ]; then
		rm /etc/apache2/sites-enabled/elk.conf
	fi
fi

%postun
if [ -d /opt/datafari ]; then
	tmp_dir="/var/tmp/datafari"
	if [ -d "$tmp_dir" ];
	then
		cp -R "$tmp_dir"/* /opt/datafari/
	fi
fi

%files
%config(noreplace) /opt/datafari/solr/solr_home/FileShare/conf/schema.xml
%config(noreplace) /opt/datafari/solr/solr_home/FileShare/conf/solrconfig.xml
%config(noreplace) /opt/datafari/mcf/mcf_home/properties.xml
%config(noreplace) /opt/datafari/mcf/mcf_home/connectors.xml
%doc /opt/datafari/CHANGES.txt
%doc /opt/datafari/LICENSE.txt
%doc /opt/datafari/README.txt
/opt/datafari/batch/
/opt/datafari/zookeeper/
/opt/datafari/zookeeper-mcf/
/opt/datafari/tomcat/
/opt/datafari/tomcat-mcf/
/opt/datafari/mcf/
/opt/datafari/elk/
/opt/datafari/bin/
/opt/datafari/solr/
/opt/datafari/cassandra/
/opt/datafari/ocr/
/opt/datafari/pgsql/
/opt/datafari/ssl-keystore/
/opt/datafari/tika-server/
/opt/datafari/command/
/opt/datafari/apache
%dir /opt/datafari/logs/
%dir /opt/datafari/pid/
