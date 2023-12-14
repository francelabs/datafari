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
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.PortMustBeAnInteger'))");
    editconnection.confluence_port.focus();
    return false;
  }
  
  if (editconnection.confluence_socket_timeout.value == "" || !isInteger(editconnection.confluence_socket_timeout.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.SocketTimeoutMustBeAnInteger'))");
    editconnection.confluence_socket_timeout.focus();
    return false;
  }
  
  if (editconnection.confluence_connection_timeout.value == "" || !isInteger(editconnection.confluence_connection_timeout.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.ConnectionTimeoutMustBeAnInteger'))");
    editconnection.confluence_connection_timeout.focus();
    return false;
  }

  if (editconnection.confluence_host.value != "" && editconnection.confluence_host.value.indexOf("/") != -1)
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.HostMustNotIncludeSlash'))");
    editconnection.confluence_host.focus();
    return false;
  }

  if (editconnection.confluence_path.value != "" && !(editconnection.confluence_path.value.indexOf("/") == 0))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.PathMustBeginWithASlash'))");
    editconnection.confluence_path.focus();
    return false;
  }

  return true;
}
 
function checkConfigForSave()
{
    
  if (editconnection.confluence_host.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.HostMustNotBeNull'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.Server'))");
    editconnection.confluence_host.focus();
    return false;
  }
  
  if (editconnection.confluence_host.value != "" && editconnection.confluence_host.value.indexOf("/") != -1)
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.HostMustNotIncludeSlash'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.Server'))");
    editconnection.confluence_host.focus();
    return false;
  }

  if (editconnection.confluence_port.value == "" || !isInteger(editconnection.confluence_port.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.PortMustBeAnInteger'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.Server'))");
    editconnection.confluence_port.focus();
    return false;
  }
  
  if (editconnection.confluence_socket_timeout.value == "" || !isInteger(editconnection.confluence_socket_timeout.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.SocketTimeoutMustBeAnInteger'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.Server'))");
    editconnection.confluence_socket_timeout.focus();
    return false;
  }
  
  if (editconnection.confluence_connection_timeout.value == "" || !isInteger(editconnection.confluence_connection_timeout.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.ConnectionTimeoutMustBeAnInteger'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.Server'))");
    editconnection.confluence_connection_timeout.focus();
    return false;
  }

  if (editconnection.confluence_path.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.PathMustNotBeNull'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.Server'))");
    editconnection.confluence_path.focus();
    return false;
  }
  
  if (editconnection.confluence_path.value != "" && !(editconnection.confluence_path.value.indexOf("/") == 0))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.PathMustBeginWithASlash'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.Server'))");
    editconnection.confluence_path.focus();
    return false;
  }
  
  if (editconnection.cache_lifetime.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.CacheLifetimeCannotBeNull'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.Cache'))");
    editconnection.cache_lifetime.focus();
    return false;
  }
  
  if (editconnection.cache_lifetime.value != "" && !isInteger(editconnection.cache_lifetime.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.CacheLifetimeMustBeAnInteger'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.Cache'))");
    editconnection.cache_lifetime.focus();
    return false;
  }
  
  if (editconnection.cache_lru_size.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.CacheLRUSizeCannotBeNull'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.Cache'))");
    editconnection.cache_lru_size.focus();
    return false;
  }
  
  if (editconnection.cache_lru_size.value != "" && !isInteger(editconnection.cache_lru_size.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.CacheLRUSizeMustBeAnInteger'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceAuthorityConnector.Cache'))");
    editconnection.cache_lru_size.focus();
    return false;
  }
  
  return true;
}
//-->
</script>
