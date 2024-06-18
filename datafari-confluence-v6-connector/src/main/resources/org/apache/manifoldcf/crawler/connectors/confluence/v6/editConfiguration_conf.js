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
  if (editconnection.confluence_port.value == "" || !isInteger(editconnection.confluence_port.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceRepositoryConnector.PortMustBeAnInteger'))");
    editconnection.confluence_port.focus();
    return false;
  }
  
  if (editconnection.confluence_socket_timeout.value == "" || !isInteger(editconnection.confluence_socket_timeout.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceRepositoryConnector.SocketTimeoutMustBeAnInteger'))");
    editconnection.confluence_socket_timeout.focus();
    return false;
  }
  
  if (editconnection.confluence_connection_timeout.value == "" || !isInteger(editconnection.confluence_connection_timeout.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceRepositoryConnector.ConnectionTimeoutMustBeAnInteger'))");
    editconnection.confluence_connection_timeout.focus();
    return false;
  }

  if (editconnection.confluence_host.value != "" && editconnection.confluence_host.value.indexOf("/") != -1)
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceRepositoryConnector.HostMustNotIncludeSlash'))");
    editconnection.confluence_host.focus();
    return false;
  }

//  if (editconnection.confluence_path.value != "" && !(editconnection.confluence_path.value.indexOf("/") == 0))
//  {
//    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceRepositoryConnector.PathMustBeginWithASlash'))");
//    editconnection.confluence_path.focus();
//    return false;
//  }

  return true;
}
 
function checkConfigForSave()
{
    
  if (editconnection.confluence_host.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceRepositoryConnector.HostMustNotBeNull'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceRepositoryConnector.Server'))");
    editconnection.confluence_host.focus();
    return false;
  }
  
  if (editconnection.confluence_host.value != "" && editconnection.confluence_host.value.indexOf("/") != -1)
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceRepositoryConnector.HostMustNotIncludeSlash'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceRepositoryConnector.Server'))");
    editconnection.confluence_host.focus();
    return false;
  }

  if (editconnection.confluence_port.value == "" || !isInteger(editconnection.confluence_port.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceRepositoryConnector.PortMustBeAnInteger'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceRepositoryConnector.Server'))");
    editconnection.confluence_port.focus();
    return false;
  }
  
  if (editconnection.confluence_socket_timeout.value == "" || !isInteger(editconnection.confluence_socket_timeout.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceRepositoryConnector.SocketTimeoutMustBeAnInteger'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceRepositoryConnector.Server'))");
    editconnection.confluence_socket_timeout.focus();
    return false;
  }
  
  if (editconnection.confluence_connection_timeout.value == "" || !isInteger(editconnection.confluence_connection_timeout.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceRepositoryConnector.ConnectionTimeoutMustBeAnInteger'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceRepositoryConnector.Server'))");
    editconnection.confluence_connection_timeout.focus();
    return false;
  }

//  if (editconnection.confluence_path.value == "")
//  {
//    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceRepositoryConnector.PathMustNotBeNull'))");
//    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceRepositoryConnector.Server'))");
//    editconnection.confluence_path.focus();
//    return false;
//  }
//  
//  if (editconnection.confluence_path.value != "" && !(editconnection.confluence_path.value.indexOf("/") == 0))
//  {
//    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceRepositoryConnector.PathMustBeginWithASlash'))");
//    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceRepositoryConnector.Server'))");
//    editconnection.confluence_path.focus();
//    return false;
//  }
  
  return true;
}
//-->
</script>
