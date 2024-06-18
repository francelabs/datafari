<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

<script type="text/javascript">
function s${SEQNUM}_checkSpecification()
{
  if (editjob.s${SEQNUM}_securityactivated.checked && editjob.s${SEQNUM}_securityfield.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('SolrIngester.NoValueSecurityField'))");
    editjob.s${SEQNUM}_securityfield.focus();
    return false;
  }
  
  if (editjob.s${SEQNUM}_collection.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('SolrIngester.NoValueCollection'))");
    editjob.s${SEQNUM}_collection.focus();
    return false;
  }
  
  if (editjob.s${SEQNUM}_fieldid.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('SolrIngester.NoValueIdField'))");
    editjob.s${SEQNUM}_fieldid.focus();
    return false;
  }
  
  if (editjob.s${SEQNUM}_fielddate.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('SolrIngester.NoValueDateField'))");
    editjob.s${SEQNUM}_fielddate.focus();
    return false;
  }
  
  
  return true;
}
//-->
</script>
