#!/bin/bash

echo "ZK Solr init script"

sleep 2

  "/opt/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost $ZK:2181 -confdir "/var/solr/solrcloud/FileShare/conf" -confname Init
  "/opt/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost $ZK:2181 -confdir "/var/solr/solrcloud/Statistics/conf" -confname Statistics
  "/opt/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost $ZK:2181 -confdir "/var/solr/solrcloud/Promolink/conf" -confname Promolink

  "/opt/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost $ZK:2181 -confdir "/var/solr/solrcloud/Duplicates/conf" -confname Duplicates
"/opt/solr/server/scripts/cloud-scripts/zkcli.sh" -cmd upconfig -zkhost $ZK:2181 -confdir "/var/solr/solrcloud/Entities/conf" -confname Entities
echo "Solr part"
sleep 5
 curl -XGET "http://$SOLR:8983/solr/admin/configs?action=CREATE&name=FileShare&baseConfigSet=Init&configSetProp.immutable=false"
 curl -XGET "http://$SOLR:8983/solr/admin/collections?action=CREATE&name=FileShare&collection.configName=FileShare&numShards=2&maxShardsPerNode=2&replicationFactor=1&&property.mcf.ip=http://$MCF:9080/datafari-mcf-authority-service&property.lib.path=/var/solr/solrcloud/FileShare/"
  curl -XGET "http://$SOLR:8983/solr/admin/collections?action=CREATE&name=Statistics&collection.configName=Statistics&numShards=1&maxShardsPerNode=1&replicationFactor=1"
  curl -XGET "http://$SOLR:8983/solr/admin/collections?action=CREATE&name=Promolink&collection.configName=Promolink&numShards=1&maxShardsPerNode=1&replicationFactor=1"
  curl -XPOST http://$SOLR:8983/solr/FileShare/config/params -H 'Content-type:application/json'  -d '{"set":{"mySearch":{"qf":"title_fr^50 title_en^50 title_de^50 title_es^50 content_fr^10 content_en^10 content_de^10 content_es^10 source^20 id^3 url_search^3","pf":"title_en^500 title_fr^500 title_de^500 title_es^500 content_fr^100 content_en^100 content_de^100 content_es^100 url_search^30","hl.maxAnalyzedChars":51200}}}'
  curl -XPOST -H 'Content-type:application/json' -d '{"set-user-property": {"autocomplete.threshold": "0.005"}}' http://$SOLR:8983/solr/FileShare/config
  curl -XPOST -H 'Content-type:application/json' -d '{"set-user-property": {"clustering.enabled": "false"}}' http://$SOLR:8983/solr/FileShare/config



