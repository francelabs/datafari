#**
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*#

<script type="text/javascript">
/* editConfiguration.js.vm (Web Connector) */
function checkConfig()
{
  if (editconnection.email.value != "" && editconnection.email.value.indexOf("@") == -1)
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.NeedAValidEmailAddress'))");
    editconnection.email.focus();
    return false;
  }

  // If the Bandwidth tab is up, check to be sure we have valid numbers and regexps everywhere.
  var i = 0;
  var count = editconnection.bandwidth_count.value;
  while (i < count)
  {
    var connections = eval("editconnection.connections_bandwidth_"+i+".value");
    if (connections != "" && !isInteger(connections))
    {
      alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.MaximumConnectionsMustBeAnInteger'))");
      eval("editconnection.connections_bandwidth_"+i+".focus()");
      return false;
    }
    var rate = eval("editconnection.rate_bandwidth_"+i+".value");
    if (rate != "" && !isInteger(rate))
    {
      alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.MaximumKbytesPerSecondMustBeAnInteger'))");
      eval("editconnection.rate_bandwidth_"+i+".focus()");
      return false;
    }
    var fetches = eval("editconnection.fetches_bandwidth_"+i+".value");
    if (fetches != "" && !isInteger(fetches))
    {
      alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.MaximumFetchesPerMinuteMustBeAnInteger'))");
      eval("editconnection.fetches_bandwidth_"+i+".focus()");
      return false;
    }

    i = i + 1;
  }
    
  // Make sure access credentials are all legal
  i = 0;
  count = editconnection.acredential_count.value;
  while (i < count)
  {
    var username = eval("editconnection.username_acredential_"+i+".value");
    if (username == "")
    {
      alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.CredentialMustHaveNonNullUserName'))");
      eval("editconnection.username_acredential_"+i+".focus()");
      return false;
    }
    i = i + 1;
  }

  // Make sure session credentials are all legal
  i = 0;
  count = editconnection.scredential_count.value;
  while (i < count)
  {
    var loginpagecount = eval("editconnection.scredential_"+i+"_loginpagecount.value");
    var j = 0;
    while (j < loginpagecount)
    {
      var matchregexp = eval("editconnection.scredential_"+i+"_"+j+"_matchregexp.value");
      if (!isRegularExpression(matchregexp))
      {
        alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.MatchExpressionMustBeAValidRegularExpression'))");
        eval("editconnection.scredential_"+i+"_"+j+"_matchregexp.focus()");
        return false;
      }
      if (eval("editconnection.scredential_"+i+"_"+j+"_type.value") == "form")
      {
        var paramcount = eval("editconnection.scredential_"+i+"_"+j+"_loginparamcount.value");
        var k = 0;
        while (k < paramcount)
        {
          var paramname = eval("editconnection.scredential_"+i+"_"+j+"_"+k+"_param.value");
          if (paramname == "")
          {
            alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.ParameterMustHaveNonEmptyName'))");
            eval("editconnection.scredential_"+i+"_"+j+"_"+k+"_param.focus()");
            return false;
          }
          var paramvalue = eval("editconnection.scredential_"+i+"_"+j+"_"+k+"_value.value");
          var parampassword = eval("editconnection.scredential_"+i+"_"+j+"_"+k+"_password.value");
          if (paramvalue != "" && parampassword != "")
          {
            alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.ParameterCanEitherBeHidden'))");
            eval("editconnection.scredential_"+i+"_"+j+"_"+k+"_value.focus()");
            return false;
          }
          k = k + 1;
        }
      }
      j = j + 1;
    }
    i = i + 1;
  }
  return true;
}

function checkConfigForSave()
{
  if (editconnection.email.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.EmailAaddressRequired'))");
    SelectTab("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.Email'))");
    editconnection.email.focus();
    return false;
  }
  return true;
}

function deleteRegexp(i)
{
  // Set the operation
  eval("editconnection.op_bandwidth_"+i+".value=\"Delete\"");
  // Submit
  if (editconnection.bandwidth_count.value==i)
    postFormSetAnchor("bandwidth");
  else
    postFormSetAnchor("bandwidth_"+i)
  // Undo, so we won't get two deletes next time
  eval("editconnection.op_bandwidth_"+i+".value=\"Continue\"");
}

function addRegexp()
{
  if (editconnection.connections_bandwidth.value != "" && !isInteger(editconnection.connections_bandwidth.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.MaximumConnectionsMustBeAnInteger'))");
    editconnection.connections_bandwidth.focus();
    return;
  }
  if (editconnection.rate_bandwidth.value != "" && !isInteger(editconnection.rate_bandwidth.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.MaximumKbytesPerSecondMustBeAnInteger'))");
    editconnection.rate_bandwidth.focus();
    return;
  }
  if (editconnection.fetches_bandwidth.value != "" && !isInteger(editconnection.fetches_bandwidth.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.MaximumFetchesPerMinuteMustBeAnInteger'))");
    editconnection.fetches_bandwidth.focus();
    return;
  }
  if (!isRegularExpression(editconnection.regexp_bandwidth.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.AValidRegularExpressionIsRequired'))");
    editconnection.regexp_bandwidth.focus();
    return;
  }
  editconnection.bandwidth_op.value="Add";
  postFormSetAnchor("bandwidth");
}

function deleteARegexp(i)
{
  // Set the operation
  eval("editconnection.op_acredential_"+i+".value=\"Delete\"");
  // Submit
  if (editconnection.acredential_count.value==i)
    postFormSetAnchor("acredential");
  else
    postFormSetAnchor("acredential_"+i)
  // Undo, so we won't get two deletes next time
  eval("editconnection.op_acredential_"+i+".value=\"Continue\"");
}

function addARegexp()
{
  if (editconnection.username_acredential.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.CredentialMustIncludeANonNullUserName'))");
    editconnection.username_acredential.focus();
    return;
  }
  if (!isRegularExpression(editconnection.regexp_acredential.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.AValidRegularExpressionIsRequired'))");
    editconnection.regexp_acredential.focus();
    return;
  }
  editconnection.acredential_op.value="Add";
  postFormSetAnchor("acredential");
}

function deleteSRegexp(i)
{
  // Set the operation
  eval("editconnection.scredential_"+i+"_op.value=\"Delete\"");
  // Submit
  if (editconnection.scredential_count.value==i)
    postFormSetAnchor("scredential");
  else
    postFormSetAnchor("scredential_"+i)
  // Undo, so we won't get two deletes next time
  eval("editconnection.scredential_"+i+"_op.value=\"Continue\"");
}

function addSRegexp()
{
  if (!isRegularExpression(editconnection.scredential_regexp.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.AValidRegularExpressionIsRequired'))");
    editconnection.scredential_regexp.focus();
    return;
  }
  editconnection.scredential_op.value="Add";
  postFormSetAnchor("scredential");
}

function deleteLoginPage(credential,loginpage)
{
  // Set the operation
  eval("editconnection.scredential_"+credential+"_"+loginpage+"_op.value=\"Delete\"");
  // Submit
  if (eval("editconnection.scredential_"+credential+"_loginpagecount.value")==credential)
    postFormSetAnchor("scredential_loginpage");
  else
    postFormSetAnchor("scredential_"+credential+"_"+loginpage)
  // Undo, so we won't get two deletes next time
  eval("editconnection.scredential_"+credential+"_"+loginpage+"_op.value=\"Continue\"");

}
  
function addLoginPage(credential)
{
  if (!isRegularExpression(eval("editconnection.scredential_"+credential+"_loginpageregexp.value")))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.AValidRegularExpressionIsRequired'))");
    eval("editconnection.scredential_"+credential+"_loginpageregexp.focus()");
    return;
  }
  if (!isRegularExpression(eval("editconnection.scredential_"+credential+"_loginpagematchregexp.value")))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.AValidRegularExpressionIsRequired'))");
    eval("editconnection.scredential_"+credential+"_loginpagematchregexp.focus()");
    return;
  }
  eval("editconnection.scredential_"+credential+"_loginpageop.value=\"Add\"");
  postFormSetAnchor("scredential_"+credential);
}
  
function deleteLoginPageParameter(credential,loginpage,parameter)
{
  // Set the operation
  eval("editconnection.scredential_"+credential+"_"+loginpage+"_"+parameter+"_op.value=\"Delete\"");
  // Submit
  if (eval("editconnection.scredential_"+credential+"_"+loginpage+"_loginparamcount.value")==credential)
    postFormSetAnchor("scredential_"+credential+"_loginparam");
  else
    postFormSetAnchor("scredential_"+credential+"_"+loginpage+"_"+parameter)
  // Undo, so we won't get two deletes next time
  eval("editconnection.scredential_"+credential+"_"+loginpage+"_"+parameter+"_op.value=\"Continue\"");
}
  
function addLoginPageParameter(credential,loginpage)
{
  if (!isRegularExpression(eval("editconnection.scredential_"+credential+"_"+loginpage+"_loginparamname.value")))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.ParameterNameMustBeARegularExpression'))");
    eval("editconnection.scredential_"+credential+"_"+loginpage+"_loginparamname.focus()");
    return;
  }
  if (eval("editconnection.scredential_"+credential+"_"+loginpage+"_loginparamvalue.value") != "" && eval("editconnection.scredential_"+credential+"_"+loginpage+"_loginparampassword.value") != "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.ParameterCanEitherBeHidden'))");
    eval("editconnection.scredential_"+credential+"_"+loginpage+"_loginparamvalue.focus()");
    return;
  }
  eval("editconnection.scredential_"+credential+"_"+loginpage+"_loginparamop.value=\"Add\"");
  postFormSetAnchor("scredential_"+credential+"_"+loginpage);
}
  
function deleteTRegexp(i)
{
  // Set the operation
  eval("editconnection.op_trust_"+i+".value=\"Delete\"");
  // Submit
  if (editconnection.trust_count.value==i)
    postFormSetAnchor("trust");
  else
    postFormSetAnchor("trust_"+i);
  // Undo, so we won't get two deletes next time
  eval("editconnection.op_trust_"+i+".value=\"Continue\"");
}

function addTRegexp()
{
  if (editconnection.certificate_trust.value == "" && editconnection.all_trust.checked == false)
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.SpecifyATrustCertificateFileToUploadFirst'))");
    editconnection.certificate_trust.focus();
    return;
  }
  if (!isRegularExpression(editconnection.regexp_trust.value))
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.AValidRegularExpressionIsRequired'))");
    editconnection.regexp_trust.focus();
    return;
  }
  editconnection.trust_op.value="Add";
  postFormSetAnchor("trust");
}
</script>

