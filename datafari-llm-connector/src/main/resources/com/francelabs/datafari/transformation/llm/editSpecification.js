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

    function s${SEQNUM}_checkSpecification()
    {
        return true;
    }

    function s${SEQNUM}_addCategory()
    {
      if (editjob.s${SEQNUM}_category_value.value == "")
      {
        alert("$Encoder.bodyEscape($ResourceBundle.getString('llm.noCategorySpecified'))");
        editjob.s${SEQNUM}_category_value.focus();
        return;
      }
      editjob.s${SEQNUM}_category_op.value="Add";
      postFormSetAnchor("s${SEQNUM}_category");
    }

    function s${SEQNUM}_deleteCategory(i)
    {
      // Set the operation
      eval("editjob.s${SEQNUM}_category_op_"+i+".value=\"Delete\"");
      // Submit
      if (editjob.s${SEQNUM}_category_count.value==i)
        postFormSetAnchor("s${SEQNUM}_category");
      else
        postFormSetAnchor("s${SEQNUM}_category_"+i)
      // Undo, so we won't get two deletes next time
      eval("editjob.s${SEQNUM}_category_op_"+i+".value=\"Continue\"");
    }


</script>
