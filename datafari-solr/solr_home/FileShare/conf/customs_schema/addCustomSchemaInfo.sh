#!/bin/bash
server=http://localhost:8983/solr/FileShare/schema

sendCommand() {
	# test if command is empty
	if [[ -z "${1// }" ]]; then
		return;
	fi
	echo $1
	# call API with an ADD command, if answer is "already exist", use REPLACE command instead
	resp=$(echo '{ "add-'$2'": '$1' }' | curl -s -X POST -H 'Content-type:application/json' --data-binary @- $server)
        if [[ $resp == *"already"* ]]
           then
              resp=$(echo '{ "replace-'$2'": '$1' }' | curl -s -X POST -H 'Content-type:application/json' --data-binary @- $server)
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
		resp=$(echo '{ "delete-'$2'": '$delete_item' }' | curl -s -X POST -H 'Content-type:application/json' --data-binary @- $server)
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


processFile custom_fieldTypes.incl sendCommand field-type
processFile custom_fields.incl sendCommand field
processFile custom_dynamicFields.incl sendCommand dynamic-field
#Try to delete the copy fields to avoid duplicates
processFile custom_copyFields.incl deleteCopyFields copy-field
processFile custom_copyFields.incl sendCommand copy-field
echo done