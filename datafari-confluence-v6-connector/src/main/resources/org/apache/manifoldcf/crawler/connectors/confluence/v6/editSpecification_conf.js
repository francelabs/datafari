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
function checkSpecificationForSave()
{
  return true;
}
 
function s${SeqNum}_SpecOp(n, opValue, anchorvalue)
{
  eval("editjob."+n+".value = \""+opValue+"\"");
  postFormSetAnchor(anchorvalue);
}

function s${SeqNum}_SpecDeleteSpace(i)
{
	s${SeqNum}_SpecOp("s${SeqNum}_spaceop_"+i,"Delete","space_"+i);
}

function s${SeqNum}_SpecAddSpace(i)
{
  var x = i-1;
  if (editjob["s${SeqNum}_space"].value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('ConfluenceRepositoryConnector.TypeInASpace'))");
    editjob.s${SeqNum}_space.focus();
    return;
  }
  s${SeqNum}_SpecOp("s${SeqNum}_spaceop","Add","space_"+i);
}

//-->
</script>