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
function s${SEQNUM}_checkSpecification()
{
  return true;
}

function s${SEQNUM}_addIncludeFilter()
{
  if (editjob.s${SEQNUM}_includefilter_regex.value == "")
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('DocFilter.NoRegexSpecified'))");
    editjob.s${SEQNUM}_includefilter_regex.focus();
    return;
  }
  editjob.s${SEQNUM}_includefilter_op.value="Add";
  postFormSetAnchor("s${SEQNUM}_includefilter");
}

function s${SEQNUM}_addExcludeFilter()
{
  if (editjob.s${SEQNUM}_excludefilter_regex.value == "")
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('DocFilter.NoRegexSpecified'))");
    editjob.s${SEQNUM}_excludefilter_regex.focus();
    return;
  }
  editjob.s${SEQNUM}_excludefilter_op.value="Add";
  postFormSetAnchor("s${SEQNUM}_excludefilter");
}

function s${SEQNUM}_deleteIncludeFilter(i)
{
  // Set the operation
  eval("editjob.s${SEQNUM}_includefilter_op_"+i+".value=\"Delete\"");
  // Submit
  if (editjob.s${SEQNUM}_includefilter_count.value==i)
    postFormSetAnchor("s${SEQNUM}_includefilter");
  else
    postFormSetAnchor("s${SEQNUM}_includefilter_"+i)
  // Undo, so we won't get two deletes next time
  eval("editjob.s${SEQNUM}_includefilter_op_"+i+".value=\"Continue\"");
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

//-->
</script>
