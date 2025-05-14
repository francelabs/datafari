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
function checkConfig()
{
  if (editconnection.jiraport.value != "" && !isInteger(editconnection.jiraport.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.JiraPortMustBeAnInteger'))");
    editconnection.jiraport.focus();
    return false;
  }

  if (editconnection.jirahost.value != "" && editconnection.jirahost.value.indexOf("/") != -1)
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.JiraHostMustNotIncludeSlash'))");
    editconnection.jirahost.focus();
    return false;
  }

  if (editconnection.jirapath.value != "" && !(editconnection.jirapath.value.indexOf("/") == 0))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.JiraPathMustBeginWithASlash'))");
    editconnection.jirapath.focus();
    return false;
  }

  if (editconnection.jiraproxyport.value != "" && !isInteger(editconnection.jiraproxyport.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.JiraProxyPortMustBeAnInteger'))");
    editconnection.jiraproxyport.focus();
    return false;
  }

  if (editconnection.jiraproxyhost.value != "" && editconnection.jiraproxyhost.value.indexOf("/") != -1)
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.JiraProxyHostMustNotIncludeSlash'))");
    editconnection.jiraproxyhost.focus();
    return false;
  }

  return true;
}
 
function checkConfigForSave()
{
    
  if (editconnection.jirahost.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.JiraHostMustNotBeNull'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.Server'))");
    editconnection.jirahost.focus();
    return false;
  }
  
  if (editconnection.jirahost.value != "" && editconnection.jirahost.value.indexOf("/") != -1)
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.JiraHostMustNotIncludeSlash'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.Server'))");
    editconnection.jirahost.focus();
    return false;
  }

  if (editconnection.jiraport.value != "" && !isInteger(editconnection.jiraport.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.JiraPortMustBeAnInteger'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.Server'))");
    editconnection.jiraport.focus();
    return false;
  }

  if (editconnection.jirapath.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.JiraPathMustNotBeNull'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.Server'))");
    editconnection.jirapath.focus();
    return false;
  }
  
  if (editconnection.jirapath.value != "" && !(editconnection.jirapath.value.indexOf("/") == 0))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.JiraPathMustBeginWithASlash'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.Server'))");
    editconnection.jirapath.focus();
    return false;
  }
  
  if (editconnection.jiraproxyhost.value != "" && editconnection.jiraproxyhost.value.indexOf("/") != -1)
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.JiraProxyHostMustNotIncludeSlash'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.Proxy'))");
    editconnection.jirahost.focus();
    return false;
  }

  if (editconnection.jiraproxyport.value != "" && !isInteger(editconnection.jiraproxyport.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.JiraProxyPortMustBeAnInteger'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraRepositoryConnector.Proxy'))");
    editconnection.jiraport.focus();
    return false;
  }

  return true;
}
//-->
</script>
