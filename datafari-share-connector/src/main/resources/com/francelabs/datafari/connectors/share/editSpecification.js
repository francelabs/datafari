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
//<!--

function s${SEQNUM}_checkSpecification()
{
  if (editjob.s${SEQNUM}_specmaxlength.value != "" && !isInteger(editjob.s${SEQNUM}_specmaxlength.value))
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('SharedDriveConnector.NeedAValidNumberForMaximumDocumentLength'))");
    editjob.s${SEQNUM}_specmaxlength.focus();
    return false;
  }
  return true;
}

function s${SEQNUM}_addIncludeFolderFilter()
{
  if (editjob.s${SEQNUM}_includefolderfilter_regex.value == "")
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('SharedDriveConnector.NoRegexSpecified'))");
    editjob.s${SEQNUM}_includefolderfilter_regex.focus();
    return;
  }
  editjob.s${SEQNUM}_includefolderfilter_op.value="Add";
  postFormSetAnchor("s${SEQNUM}_includefolderfilter");
}

function s${SEQNUM}_addIncludeFileFilter()
{
  if (editjob.s${SEQNUM}_includefilefilter_regex.value == "")
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('SharedDriveConnector.NoRegexSpecified'))");
    editjob.s${SEQNUM}_includefilefilter_regex.focus();
    return;
  }
  editjob.s${SEQNUM}_includefilefilter_op.value="Add";
  postFormSetAnchor("s${SEQNUM}_includefilefilter");
}

function s${SEQNUM}_addExcludeFilter()
{
  if (editjob.s${SEQNUM}_excludefilter_regex.value == "")
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('SharedDriveConnector.NoRegexSpecified'))");
    editjob.s${SEQNUM}_excludefilter_regex.focus();
    return;
  }
  editjob.s${SEQNUM}_excludefilter_op.value="Add";
  postFormSetAnchor("s${SEQNUM}_excludefilter");
}

function s${SEQNUM}_deleteIncludeFileFilter(i)
{
  // Set the operation
  eval("editjob.s${SEQNUM}_includefilefilter_op_"+i+".value=\"Delete\"");
  // Submit
  if (editjob.s${SEQNUM}_includefilefilter_count.value==i)
    postFormSetAnchor("s${SEQNUM}_includefilefilter");
  else
    postFormSetAnchor("s${SEQNUM}_includefilefilter_"+i)
  // Undo, so we won't get two deletes next time
  eval("editjob.s${SEQNUM}_includefilefilter_op_"+i+".value=\"Continue\"");
}

function s${SEQNUM}_deleteIncludeFolderFilter(i)
{
  // Set the operation
  eval("editjob.s${SEQNUM}_includefolderfilter_op_"+i+".value=\"Delete\"");
  // Submit
  if (editjob.s${SEQNUM}_includefolderfilter_count.value==i)
    postFormSetAnchor("s${SEQNUM}_includefolderfilter");
  else
    postFormSetAnchor("s${SEQNUM}_includefolderfilter_"+i)
  // Undo, so we won't get two deletes next time
  eval("editjob.s${SEQNUM}_includefolderfilter_op_"+i+".value=\"Continue\"");
}

function s${SEQNUM}_deleteExcludeFilter(i)
{
  // Set the operation
  eval("editjob.s${SEQNUM}_excludefilter_op_"+i+".value=\"Delete\"");
  // Submit
  if (editjob.s${SEQNUM}_excludefilter_count.value==i)
    postFormSetAnchor("s${SEQNUM}_excludefilter");
  else
    postFormSetAnchor("s${SEQNUM}_excludefilter_"+i)
  // Undo, so we won't get two deletes next time
  eval("editjob.s${SEQNUM}_excludefilter_op_"+i+".value=\"Continue\"");
}

function s${SEQNUM}_SpecOp(n, opValue, anchorvalue)
{
  eval("editjob."+n+".value =\""+opValue+"\"");
  postFormSetAnchor(anchorvalue);
}

function s${SEQNUM}_SpecAddToPath(anchorvalue)
{
  if (editjob.s${SEQNUM}_pathaddon.value == "" && editjob.s${SEQNUM}_pathtypein.value == "")
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('SharedDriveConnector.SelectAFolderOrTypeInAPathFirst'))");
    editjob.s${SEQNUM}_pathaddon.focus();
    return;
  }
  if (editjob.s${SEQNUM}_pathaddon.value != "" && editjob.s${SEQNUM}_pathtypein.value != "")
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('SharedDriveConnector.EitherSelectAFolderORTypeInAPath'))");
    editjob.s${SEQNUM}_pathaddon.focus();
    return;
  }
  s${SEQNUM}_SpecOp("s${SEQNUM}_pathop","AddToPath",anchorvalue);
}

function s${SEQNUM}_SpecAddSpec(suffix,anchorvalue)
{
  if (eval("editjob.s${SEQNUM}_specfile"+suffix+".value") == "")
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('SharedDriveConnector.EnterAFileSpecificationFirst'))");
    eval("editjob.s${SEQNUM}_specfile"+suffix+".focus()");
    return;
  }
  s${SEQNUM}_SpecOp("s${SEQNUM}_pathop"+suffix,"Add",anchorvalue);
}

function s${SEQNUM}_SpecInsertSpec(postfix,anchorvalue)
{
  if (eval("editjob.s${SEQNUM}_specfile_i"+postfix+".value") == "")
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('SharedDriveConnector.EnterAFileSpecificationFirst'))");
    eval("editjob.s${SEQNUM}_specfile_i"+postfix+".focus()");
    return;
  }
  s${SEQNUM}_SpecOp("s${SEQNUM}_specop"+postfix,"Insert Here",anchorvalue);
}

function s${SEQNUM}_SpecAddToken(anchorvalue)
{
  if (editjob.s${SEQNUM}_spectoken.value == "")
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('SharedDriveConnector.NullAccessTokensNotAllowed'))");
    editjob.s${SEQNUM}_spectoken.focus();
    return;
  }
  s${SEQNUM}_SpecOp("s${SEQNUM}_accessop","Add",anchorvalue);
}

function s${SEQNUM}_SpecAddMapping(anchorvalue)
{
  if (editjob.s${SEQNUM}_specmatch.value == "")
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('SharedDriveConnector.MatchStringCannotBeEmpty'))");
    editjob.s${SEQNUM}_specmatch.focus();
    return;
  }
  if (!isRegularExpression(editjob.s${SEQNUM}_specmatch.value))
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('SharedDriveConnector.MatchStringMustBeValidRegularExpression'))");
    editjob.s${SEQNUM}_specmatch.focus();
    return;
  }
  s${SEQNUM}_SpecOp("s${SEQNUM}_specmappingop","Add",anchorvalue);
}

function s${SEQNUM}_SpecAddFMap(anchorvalue)
{
  if (editjob.s${SEQNUM}_specfmapmatch.value == "")
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('SharedDriveConnector.MatchStringCannotBeEmpty'))");
    editjob.s${SEQNUM}_specfmapmatch.focus();
    return;
  }
  if (!isRegularExpression(editjob.s${SEQNUM}_specfmapmatch.value))
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('SharedDriveConnector.MatchStringMustBeValidRegularExpression'))");
    editjob.s${SEQNUM}_specfmapmatch.focus();
    return;
  }
  s${SEQNUM}_SpecOp("s${SEQNUM}_specfmapop","Add",anchorvalue);
}

function s${SEQNUM}_SpecAddUMap(anchorvalue)
{
  if (editjob.s${SEQNUM}_specumapmatch.value == "")
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('SharedDriveConnector.MatchStringCannotBeEmpty'))");
    editjob.s${SEQNUM}_specumapmatch.focus();
    return;
  }
  if (!isRegularExpression(editjob.s${SEQNUM}_specumapmatch.value))
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('SharedDriveConnector.MatchStringMustBeValidRegularExpression'))");
    editjob.s${SEQNUM}_specumapmatch.focus();
    return;
  }
  s${SEQNUM}_SpecOp("s${SEQNUM}_specumapop","Add",anchorvalue);
}

//-->
</script>
