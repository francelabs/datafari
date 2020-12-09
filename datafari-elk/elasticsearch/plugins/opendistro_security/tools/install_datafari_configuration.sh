#!/bin/bash
#install_datafari_configuration.sh [-y]

SCRIPT_PATH="${BASH_SOURCE[0]}"

ELK_CERTIFICATE_PATH=$DATAFARI_HOME/ssl-keystore/elk
ELK_CERTIFICATE_CRT=datafari-cert.pem
ELK_CERTIFICATE_KEY=datafari-key.pem

if ! [ -x "$(command -v realpath)" ]; then
    if [ -L "$SCRIPT_PATH" ]; then

        [ -x "$(command -v readlink)" ] || { echo "Not able to resolve symlink. Install realpath or readlink.";exit 1; }

        # try readlink (-f not needed because we know its a symlink)
        DIR="$( cd "$( dirname $(readlink "$SCRIPT_PATH") )" && pwd -P)"
    else
        DIR="$( cd "$( dirname "$SCRIPT_PATH" )" && pwd -P)"
    fi
else
    DIR="$( cd "$( dirname "$(realpath "$SCRIPT_PATH")" )" && pwd -P)"
fi

echo "OpenDistro for Elasticsearch Security Datafari Installer"

OPTIND=1
assumeyes=0
initsecurity=0
cluster_mode=0
skip_updates=-1


function show_help() {
    echo "install_datafari_configuration.sh [-y] [-i] [-c]"
    echo "  -h show help"
    echo "  -y confirm all installation dialogues automatically"
    echo "  -i initialize Security plugin with default configuration (default is to ask if -y is not given)"
    echo "  -c enable cluster mode by binding to all network interfaces (default is to ask if -y is not given)"
    echo "  -s skip updates if config is already applied to elasticsearch.yml"
}

while getopts "h?yics" opt; do
    case "$opt" in
    h|\?)
        show_help
        exit 0
        ;;
    y)  assumeyes=1
        ;;
    i)  initsecurity=1
        ;;
    c)  cluster_mode=1
        ;;
    s)  skip_updates=0
    esac
done

shift $((OPTIND-1))

[ "$1" = "--" ] && shift

if [ "$assumeyes" == 0 ]; then
	read -r -p "Install demo certificates? [y/N] " response
	case "$response" in
	    [yY][eE][sS]|[yY]) 
	        ;;
	    *)
	        exit 0
	        ;;
	esac
fi

if [ "$initsecurity" == 0 ] && [ "$assumeyes" == 0 ]; then
	read -r -p "Initialize Security Modules? [y/N] " response
	case "$response" in
	    [yY][eE][sS]|[yY]) 
	        initsecurity=1
	        ;;
	    *)
	        initsecurity=0
	        ;;
	esac
fi

if [ "$cluster_mode" == 0 ] && [ "$assumeyes" == 0 ]; then
    echo "Cluster mode requires maybe additional setup of:"
    echo "  - Virtual memory (vm.max_map_count)"
    echo ""
	read -r -p "Enable cluster mode? [y/N] " response
	case "$response" in
	    [yY][eE][sS]|[yY]) 
	        cluster_mode=1
	        ;;
	    *)
	        cluster_mode=0
	        ;;
	esac
fi


set -e
BASE_DIR="$DIR/../../.."
if [ -d "$BASE_DIR" ]; then
	CUR="$(pwd)"
	cd "$BASE_DIR"
	BASE_DIR="$(pwd)"
	cd "$CUR"
	echo "Basedir: $BASE_DIR"
else
    echo "DEBUG: basedir does not exist"
fi
ES_CONF_FILE="$BASE_DIR/config/elasticsearch.yml"
ES_BIN_DIR="$BASE_DIR/bin"
ES_PLUGINS_DIR="$BASE_DIR/plugins"
ES_MODULES_DIR="$BASE_DIR/modules"
ES_LIB_PATH="$BASE_DIR/lib"
SUDO_CMD=""
ES_INSTALL_TYPE=".tar.gz"

#Check if its a rpm/deb install
if [ "/usr/share/elasticsearch" -ef "$BASE_DIR" ]; then
    ES_CONF_FILE="/usr/share/elasticsearch/config/elasticsearch.yml"

    if [ ! -f "$ES_CONF_FILE" ]; then
        ES_CONF_FILE="/etc/elasticsearch/elasticsearch.yml"
    fi

    if [ -x "$(command -v sudo)" ]; then
        SUDO_CMD="sudo"
        echo "This script maybe require your root password for 'sudo' privileges"
    fi

    ES_INSTALL_TYPE="rpm/deb"
fi

if [ $SUDO_CMD ]; then
    if ! [ -x "$(command -v $SUDO_CMD)" ]; then
        echo "Unable to locate 'sudo' command. Quit."
        exit 1
    fi
fi

if $SUDO_CMD test -f "$ES_CONF_FILE"; then
    :
else
    echo "Unable to determine Elasticsearch config directory. Quit."
    exit -1
fi

if [ ! -d "$ES_BIN_DIR" ]; then
	echo "Unable to determine Elasticsearch bin directory. Quit."
	exit -1
fi

if [ ! -d "$ES_PLUGINS_DIR" ]; then
	echo "Unable to determine Elasticsearch plugins directory. Quit."
	exit -1
fi

if [ ! -d "$ES_MODULES_DIR" ]; then
	echo "Unable to determine Elasticsearch modules directory. Quit."
	#exit -1
fi

if [ ! -d "$ES_LIB_PATH" ]; then
	echo "Unable to determine Elasticsearch lib directory. Quit."
	exit -1
fi

ES_CONF_DIR=$(dirname "${ES_CONF_FILE}")
ES_CONF_DIR=`cd "$ES_CONF_DIR" ; pwd`

if [ ! -d "$ES_PLUGINS_DIR/opendistro_security" ]; then
  echo "Open Distro Security plugin not installed. Quit."
  exit -1
fi

ES_VERSION=("$ES_LIB_PATH/elasticsearch-*.jar")
ES_VERSION=$(echo $ES_VERSION | sed 's/.*elasticsearch-\(.*\)\.jar/\1/')

SECURITY_VERSION=("$ES_PLUGINS_DIR/opendistro_security/opendistro_security-*.jar")
SECURITY_VERSION=$(echo $SECURITY_VERSION | sed 's/.*opendistro_security-\(.*\)\.jar/\1/')

OS=$(sb_release -ds 2>/dev/null || cat /etc/*release 2>/dev/null | head -n1 || uname -om)
echo "Elasticsearch install type: $ES_INSTALL_TYPE on $OS"
echo "Elasticsearch config dir: $ES_CONF_DIR"
echo "Elasticsearch config file: $ES_CONF_FILE"
echo "Elasticsearch bin dir: $ES_BIN_DIR"
echo "Elasticsearch plugins dir: $ES_PLUGINS_DIR"
echo "Elasticsearch lib dir: $ES_LIB_PATH"
echo "Detected Elasticsearch Version: $ES_VERSION"
echo "Detected Open Distro Security Version: $SECURITY_VERSION"

if $SUDO_CMD grep --quiet -i opendistro_security "$ES_CONF_FILE"; then
  echo "$ES_CONF_FILE seems to be already configured for Security. Quit."
  exit $skip_updates
fi

set +e

set -e

echo "" | $SUDO_CMD tee -a  "$ES_CONF_FILE"
echo "######## Start OpenDistro for Elasticsearch Security Datafari Configuration ########" | $SUDO_CMD tee -a "$ES_CONF_FILE" > /dev/null 
echo "opendistro_security.ssl.transport.pemcert_filepath: ${ELK_CERTIFICATE_CRT}" | $SUDO_CMD tee -a  "$ES_CONF_FILE" > /dev/null
echo "opendistro_security.ssl.transport.pemkey_filepath: ${ELK_CERTIFICATE_KEY}" | $SUDO_CMD tee -a  "$ES_CONF_FILE" > /dev/null
echo "opendistro_security.ssl.transport.pemtrustedcas_filepath: ${ELK_CERTIFICATE_CRT}" | $SUDO_CMD tee -a "$ES_CONF_FILE" > /dev/null
echo "opendistro_security.ssl.transport.enforce_hostname_verification: false" | $SUDO_CMD tee -a  "$ES_CONF_FILE" > /dev/null
echo "opendistro_security.ssl.http.enabled: true" | $SUDO_CMD tee -a "$ES_CONF_FILE" > /dev/null
echo "opendistro_security.ssl.http.pemcert_filepath: ${ELK_CERTIFICATE_CRT}" | $SUDO_CMD tee -a "$ES_CONF_FILE" > /dev/null
echo "opendistro_security.ssl.http.pemkey_filepath: ${ELK_CERTIFICATE_KEY}" | $SUDO_CMD tee -a  "$ES_CONF_FILE" > /dev/null
echo "opendistro_security.ssl.http.pemtrustedcas_filepath: ${ELK_CERTIFICATE_CRT}" | $SUDO_CMD tee -a "$ES_CONF_FILE" > /dev/null
echo "opendistro_security.allow_unsafe_democertificates: true" | $SUDO_CMD tee -a "$ES_CONF_FILE" > /dev/null
if [ "$initsecurity" == 1 ]; then
    echo "opendistro_security.allow_default_init_securityindex: true" | $SUDO_CMD tee -a "$ES_CONF_FILE" > /dev/null
fi
 
echo "" | $SUDO_CMD tee -a "$ES_CONF_FILE" > /dev/null 
echo "opendistro_security.audit.type: internal_elasticsearch" | $SUDO_CMD tee -a "$ES_CONF_FILE" > /dev/null
echo "opendistro_security.enable_snapshot_restore_privilege: true" | $SUDO_CMD tee -a "$ES_CONF_FILE" > /dev/null
echo "opendistro_security.check_snapshot_restore_write_privileges: true" | $SUDO_CMD tee -a "$ES_CONF_FILE" > /dev/null
echo 'opendistro_security.restapi.roles_enabled: ["all_access", "security_rest_api_access"]' | $SUDO_CMD tee -a "$ES_CONF_FILE" > /dev/null

#cluster.routing.allocation.disk.threshold_enabled
if $SUDO_CMD grep --quiet -i "^cluster.routing.allocation.disk.threshold_enabled" "$ES_CONF_FILE"; then
	: #already present
else
    echo 'cluster.routing.allocation.disk.threshold_enabled: false' | $SUDO_CMD tee -a "$ES_CONF_FILE" > /dev/null
fi

#network.host
if $SUDO_CMD grep --quiet -i "^network.host" "$ES_CONF_FILE"; then
	: #already present
else
	if [ "$cluster_mode" == 1 ]; then
        echo "network.host: 0.0.0.0" | $SUDO_CMD tee -a "$ES_CONF_FILE" > /dev/null
        echo "node.name: smoketestnode" | $SUDO_CMD tee -a "$ES_CONF_FILE" > /dev/null
        echo "cluster.initial_master_nodes: smoketestnode" | $SUDO_CMD tee -a "$ES_CONF_FILE" > /dev/null
    fi
fi

#discovery.zen.minimum_master_nodes
#if $SUDO_CMD grep --quiet -i "^discovery.zen.minimum_master_nodes" "$ES_CONF_FILE"; then
#	: #already present
#else
#    echo "discovery.zen.minimum_master_nodes: 1" | $SUDO_CMD tee -a "$ES_CONF_FILE" > /dev/null
#fi

#node.max_local_storage_nodes
if $SUDO_CMD grep --quiet -i "^node.max_local_storage_nodes" "$ES_CONF_FILE"; then
	: #already present
else
    echo 'node.max_local_storage_nodes: 3' | $SUDO_CMD tee -a "$ES_CONF_FILE" > /dev/null
fi



echo "######## End OpenDistro for Elasticsearch Security Datafari Configuration ########" | $SUDO_CMD tee -a "$ES_CONF_FILE" > /dev/null 

$SUDO_CMD chmod +x "$ES_PLUGINS_DIR/opendistro_security/tools/securityadmin.sh"

ES_PLUGINS_DIR=`cd "$ES_PLUGINS_DIR" ; pwd`

echo "### Success"
echo "### Execute this script now on all your nodes and then start all nodes"
#Generate securityadmin_datafari.sh
echo "#!/bin/bash" | $SUDO_CMD tee securityadmin_datafari.sh > /dev/null 
echo $SUDO_CMD \""$ES_PLUGINS_DIR/opendistro_security/tools/securityadmin.sh"\" -cd \""$ES_PLUGINS_DIR/opendistro_security/securityconfig"\" -icl -key \""$ES_CONF_DIR/datafari-key.pem"\" -cert \""$ES_CONF_DIR/datafari-cert.pem"\" -cacert \""$ES_CONF_DIR/datafari-cert.pem"\" -nhnv | $SUDO_CMD tee -a securityadmin_datafari.sh > /dev/null
$SUDO_CMD chmod +x securityadmin_datafari.sh

if [ "$initsecurity" == 0 ]; then
	echo "### After the whole cluster is up execute: "
	$SUDO_CMD cat securityadmin_datafari.sh | tail -1
	echo "### or run ./securityadmin_datafari.sh"
    echo "### After that you can also use the Security Plugin ConfigurationGUI"
else
    echo "### Open Distro Security will be automatically initialized."
    echo "### If you like to change the runtime configuration "
    echo "### change the files in ../securityconfig and execute: "
	$SUDO_CMD cat securityadmin_datafari.sh | tail -1
	echo "### or run ./securityadmin_datafari.sh"
	echo "### To use the Security Plugin ConfigurationGUI"
fi

echo "### To access your secured cluster open https://<hostname>:<HTTP port> and log in with admin/admin."
echo "### (Ignore the SSL certificate warning because we installed self-signed demo certificates)"
