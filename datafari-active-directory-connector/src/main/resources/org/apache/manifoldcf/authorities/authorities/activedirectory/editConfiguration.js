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
  var i = 0;
  var count = editconnection.dcrecord_count.value;
  while (i < count)
  {
    var username = eval("editconnection.dcrecord_username_"+i+".value");
    if (username == "")
    {
      alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ActiveDirectoryAuthority.AdministrativeUserNameCannotBeNull'))");
      eval("editconnection.dcrecord_username_"+i+".focus()");
      return false;
    }
    var authentication = eval("editconnection.dcrecord_authentication_"+i+".value");
    if (authentication == "")
    {
      alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ActiveDirectoryAuthority.AuthenticationCannotBeNull'))");
      eval("editconnection.dcrecord_authentication_"+i+".focus()");
      return false;
    }
    i += 1;
  }
  return true;
}

function checkConfigForSave()
{
  if (editconnection.cachelifetime.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ActiveDirectoryAuthority.CacheLifetimeCannotBeNull'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ActiveDirectoryAuthority.Cache'))");
    editconnection.cachelifetime.focus();
    return false;
  }
  if (editconnection.cachelifetime.value != "" && !isInteger(editconnection.cachelifetime.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ActiveDirectoryAuthority.CacheLifetimeMustBeAnInteger'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ActiveDirectoryAuthority.Cache'))");
    editconnection.cachelifetime.focus();
    return false;
  }
  if (editconnection.cachelrusize.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ActiveDirectoryAuthority.CacheLRUSizeCannotBeNull'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ActiveDirectoryAuthority.Cache'))");
    editconnection.cachelrusize.focus();
    return false;
  }
  if (editconnection.cachelrusize.value != "" && !isInteger(editconnection.cachelrusize.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ActiveDirectoryAuthority.CacheLRUSizeMustBeAnInteger'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ActiveDirectoryAuthority.Cache'))");
    editconnection.cachelrusize.focus();
    return false;
  }
  if (editconnection.ldapconnectiontimeout.value != "" && !isInteger(editconnection.ldapconnectiontimeout.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ActiveDirectoryAuthority.ConnectionTimeoutMustBeAnInteger'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ActiveDirectoryAuthority.DomainController'))");
    editconnection.ldapconnectiontimeout.focus();
    return false;
  }
  return true;
}

function deleteDC(i)
{
  eval("editconnection.dcrecord_op_"+i+".value=\"Delete\"");
  postFormSetAnchor("dcrecord");
}

function insertDC(i)
{
  if (editconnection.dcrecord_domaincontrollername.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ActiveDirectoryAuthority.EnterADomainControllerServerName'))");
    editconnection.dcrecord_domaincontrollername.focus();
    return;
  }
  if (editconnection.dcrecord_username.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ActiveDirectoryAuthority.AdministrativeUserNameCannotBeNull'))");
    editconnection.dcrecord_username.focus();
    return;
  }
  if (editconnection.dcrecord_authentication.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ActiveDirectoryAuthority.AuthenticationCannotBeNull'))");
    editconnection.dcrecord_authentication.focus();
    return;
  }
  eval("editconnection.dcrecord_op_"+i+".value=\"Insert\"");
  postFormSetAnchor("dcrecord_"+i);
}

function addDC()
{
  if (editconnection.dcrecord_domaincontrollername.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ActiveDirectoryAuthority.EnterADomainControllerServerName'))");
    editconnection.dcrecord_domaincontrollername.focus();
    return;
  }
  if (editconnection.dcrecord_username.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ActiveDirectoryAuthority.AdministrativeUserNameCannotBeNull'))");
    editconnection.dcrecord_username.focus();
    return;
  }
  if (editconnection.dcrecord_authentication.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ActiveDirectoryAuthority.AuthenticationCannotBeNull'))");
    editconnection.dcrecord_authentication.focus();
    return;
  }
  editconnection.dcrecord_op.value="Add";
  postFormSetAnchor("dcrecord");
}

//-->
</script>

