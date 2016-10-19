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

processFile() {
value=''
while IFS='' read -r line || [[ -n "$line" ]]; do
	# read file and find && separtor 
        if [ "$line" = "&&" ]; then
                sendCommand "${value}" $2
                value=''
        else
                value=${value}${line}
        fi   
done < $1
sendCommand "${value}" $2
}


processFile custom_fields.incl field
processFile custom_dynamicFields.incl dynamic-field
processFile custom_copyFields.incl copy-field
processFile custom_fieldTypes.incl field-type
echo done
