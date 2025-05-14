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
<!--
function s${SeqNum}_checkSpecificationForSave()
{
  if (editjob.s${SeqNum}_jiraquery.value == "") {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.SeedQueryCannotBeNull'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.JiraQuery'))");
    editjob.s${SeqNum}_jiraquery.focus();
    return false;
  }
  return true;
}
 
function s${SeqNum}_SpecOp(n, opValue, anchorvalue)
{
  eval("editjob."+n+".value = \""+opValue+"\"");
  postFormSetAnchor(anchorvalue);
}

function s${SeqNum}_SpecDeleteToken(i)
{
  s${SeqNum}_SpecOp("s${SeqNum}_accessop_"+i,"Delete","s${SeqNum}_token_"+i);
}

function s${SeqNum}_SpecAddToken(i)
{
  if (editjob.s${SeqNum}_spectoken.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.TypeInAnAccessToken'))");
    editjob.s${SeqNum}_spectoken.focus();
    return;
  }
  s${SeqNum}_SpecOp("s${SeqNum}_accessop","Add","s${SeqNum}_token_"+i);
}

//-->
</script>
