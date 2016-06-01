#!/bin/bash -e
#
#
# Upload ZK configuration to Solr
#
#




curl -XGET "http://localhost:8983/solr/admin/collections?action=RELOAD&name=FileShare"