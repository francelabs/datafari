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

function s${SEQNUM}_addNameCleaner()
{
  if (editjob.s${SEQNUM}_namecleaner_regex.value == "")
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('MetadataCleaner.NoRegexSpecified'))");
    editjob.s${SEQNUM}_namecleaner_regex.focus();
    return;
  }
  editjob.s${SEQNUM}_namecleaner_op.value="Add";
  postFormSetAnchor("s${SEQNUM}_namecleaner");
}

function s${SEQNUM}_deleteNameCleaner(i)
{
  // Set the operation
  eval("editjob.s${SEQNUM}_namecleaner_op_"+i+".value=\"Delete\"");
  // Submit
  if (editjob.s${SEQNUM}_namecleaner_count.value==i)
    postFormSetAnchor("s${SEQNUM}_namecleaner");
  else
    postFormSetAnchor("s${SEQNUM}_namecleaner_"+i)
  // Undo, so we won't get two deletes next time
  eval("editjob.s${SEQNUM}_namecleaner_op_"+i+".value=\"Continue\"");
}

function s${SEQNUM}_addValueCleaner()
{
  if (editjob.s${SEQNUM}_valuecleaner_regex.value == "")
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('MetadataCleaner.NoRegexSpecified'))");
    editjob.s${SEQNUM}_valuecleaner_regex.focus();
    return;
  }
  editjob.s${SEQNUM}_valuecleaner_op.value="Add";
  postFormSetAnchor("s${SEQNUM}_valuecleaner");
}

function s${SEQNUM}_deleteValueCleaner(i)
{
  // Set the operation
  eval("editjob.s${SEQNUM}_valuecleaner_op_"+i+".value=\"Delete\"");
  // Submit
  if (editjob.s${SEQNUM}_valuecleaner_count.value==i)
    postFormSetAnchor("s${SEQNUM}_valuecleaner");
  else
    postFormSetAnchor("s${SEQNUM}_valuecleaner_"+i)
  // Undo, so we won't get two deletes next time
  eval("editjob.s${SEQNUM}_valuecleaner_op_"+i+".value=\"Continue\"");
}

//-->
</script>
