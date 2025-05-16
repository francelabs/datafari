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
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraAuthorityConnector.JiraPortMustBeAnInteger'))");
    editconnection.jiraport.focus();
    return false;
  }

  if (editconnection.jirahost.value != "" && editconnection.jirahost.value.indexOf("/") != -1)
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraAuthorityConnector.JiraHostMustNotIncludeSlash'))");
    editconnection.jirahost.focus();
    return false;
  }

  if (editconnection.jirapath.value != "" && !(editconnection.jirapath.value.indexOf("/") == 0))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraAuthorityConnector.JiraPathMustBeginWithASlash'))");
    editconnection.jirapath.focus();
    return false;
  }

  if (editconnection.jiraproxyport.value != "" && !isInteger(editconnection.jiraproxyport.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraAuthorityConnector.JiraProxyPortMustBeAnInteger'))");
    editconnection.jiraproxyport.focus();
    return false;
  }

  if (editconnection.jiraproxyhost.value != "" && editconnection.jiraproxyhost.value.indexOf("/") != -1)
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraAuthorityConnector.JiraProxyHostMustNotIncludeSlash'))");
    editconnection.jiraproxyhost.focus();
    return false;
  }

  return true;
}
 
function checkConfigForSave()
{
    
  if (editconnection.jirahost.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraAuthorityConnector.JiraHostMustNotBeNull'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraAuthorityConnector.Server'))");
    editconnection.jirahost.focus();
    return false;
  }
  
  if (editconnection.jirahost.value != "" && editconnection.jirahost.value.indexOf("/") != -1)
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraAuthorityConnector.JiraHostMustNotIncludeSlash'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraAuthorityConnector.Server'))");
    editconnection.jirahost.focus();
    return false;
  }

  if (editconnection.jiraport.value != "" && !isInteger(editconnection.jiraport.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraAuthorityConnector.JiraPortMustBeAnInteger'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraAuthorityConnector.Server'))");
    editconnection.jiraport.focus();
    return false;
  }

  if (editconnection.jirapath.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraAuthorityConnector.JiraPathMustNotBeNull'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraAuthorityConnector.Server'))");
    editconnection.jirapath.focus();
    return false;
  }
  
  if (editconnection.jirapath.value != "" && !(editconnection.jirapath.value.indexOf("/") == 0))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraAuthorityConnector.JiraPathMustBeginWithASlash'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraAuthorityConnector.Server'))");
    editconnection.jirapath.focus();
    return false;
  }

  if (editconnection.jiraproxyhost.value != "" && editconnection.jiraproxyhost.value.indexOf("/") != -1)
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraAuthorityConnector.JiraProxyHostMustNotIncludeSlash'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraAuthorityConnector.Proxy'))");
    editconnection.jirahost.focus();
    return false;
  }

  if (editconnection.jiraproxyport.value != "" && !isInteger(editconnection.jiraproxyport.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraAuthorityConnector.JiraProxyPortMustBeAnInteger'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('JiraAuthorityConnector.Proxy'))");
    editconnection.jiraport.focus();
    return false;
  }

  return true;
}
//-->
</script>
