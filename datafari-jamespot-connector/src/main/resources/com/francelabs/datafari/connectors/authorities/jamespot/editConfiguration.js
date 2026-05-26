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

function checkConfigForSave()
{
  if (editconnection.newgenericAddress.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('NewGeneric.NoValueNewGenericAddress'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('NewGeneric.NewGenericTabName'))");
    eval("editconnection.newgenericAddress.focus()");
    return false;
  }
  if (editconnection.connectionTimeout.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('NewGeneric.NoValueConnectionTimeOut'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('NewGeneric.NewGenericTabName'))");
    eval("editconnection.connectionTimeout.focus()");
    return false;
  }
  if (editconnection.socketTimeout.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('NewGeneric.NoValueSocketTimeOut'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('NewGeneric.NewGenericTabName'))");
    eval("editconnection.socketTimeout.focus()");
    return false;
  }
  if (editconnection.responselifetime.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('NewGeneric.NoValueResponseLifeTime'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('NewGeneric.NewGenericTabName'))");
    eval("editconnection.responselifetime.focus()");
    return false;
  }
  return true;
}

//-->
</script>
