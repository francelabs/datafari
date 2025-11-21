#!/bin/bash

# Send solr custom configuration
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source "${DIR}/../set-datafari-env.sh"
source "${DIR}/../utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE

server=@PROTOCOL@://@SOLRNODEIP@:@PORT@/solr/@MAINCOLLECTION@/schema
SOLR_MAIN_COLLECTION_DIR="${DIR}/../../solr/solrcloud/FileShare/conf/customs_schema"


echo "Helper script to send custom Solr configuration to Zookeeper"
if [ "$NODETYPE" != "monoserver" ] && [ "$NODETYPE" != "main" ];
then
  echo "This script can only be run into monoserver or if you have a Datafari cluster into the main server."
  echo "The script will now exit"
  exit 0
fi




sendCommand() {
	# test if command is empty
	if [[ -z "${1// }" ]]; then
		return;
	fi
	echo $1
	# call API with an ADD command, if answer is "already exist", use REPLACE command instead
	resp=$(echo '{ "add-'$2'": '$1' }' | curl --insecure -s -X POST -H 'Content-type:application/json' --data-binary @- $server)
        if [[ $resp == *"already"* ]]
           then
              resp=$(echo '{ "replace-'$2'": '$1' }' | curl --insecure -s -X POST -H 'Content-type:application/json' --data-binary @- $server)
              echo $resp;
           else
              echo $resp;
        fi
}

deleteCopyFields() {
	# test if command is empty
	if [[ -z "${1// }" ]]; then
		return;
	fi
	source=$(echo $1 | jq '.source')
	for row in $(echo $1 | jq '.dest[]'); do
		delete_item="{ \"source\":$source, \"dest\":$row }"
		resp=$(echo '{ "delete-'$2'": '$delete_item' }' | curl --insecure -s -X POST -H 'Content-type:application/json' --data-binary @- $server)
		echo $resp
	done
}

processFile() {
value=''
while IFS='' read -r line || [[ -n "$line" ]]; do
	# read file and find && separtor
        if [ "$line" = "&&" ]; then
                $2 "${value}" $3
                value=''
        else
                value=${value}${line}
        fi
done < $1
$2 "${value}" $3
}


processFile $SOLR_MAIN_COLLECTION_DIR/custom_fieldTypes.incl sendCommand field-type
processFile $SOLR_MAIN_COLLECTION_DIR/custom_fields.incl sendCommand field
processFile $SOLR_MAIN_COLLECTION_DIR/custom_dynamicFields.incl sendCommand dynamic-field
#Try to delete the copy fields to avoid duplicates
processFile $SOLR_MAIN_COLLECTION_DIR/custom_copyFields.incl deleteCopyFields copy-field
processFile $SOLR_MAIN_COLLECTION_DIR/custom_copyFields.incl sendCommand copy-field
echo done
