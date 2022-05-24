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

#set( $SEQPREFIX = "s" + $SEQNUM + "_" )
<script type="text/javascript">
/* editSpecification.js.vm (Web Connector) */
function ${SEQPREFIX}SpecOp(n, opValue, anchorvalue)
{
  eval("editjob."+n+".value = \""+opValue+"\"");
  postFormSetAnchor(anchorvalue);
}

function ${SEQPREFIX}AddRegexp(anchorvalue)
{
  if (editjob.${SEQPREFIX}rssmatch.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.MatchMustHaveARegexpValue'))");
    editjob.${SEQPREFIX}rssmatch.focus();
    return;
  }

  ${SEQPREFIX}SpecOp("${SEQPREFIX}rssop","Add",anchorvalue);
}

function ${SEQPREFIX}RemoveRegexp(index, anchorvalue)
{
  editjob.${SEQPREFIX}rssindex.value = index;
  ${SEQPREFIX}SpecOp("${SEQPREFIX}rssop","Delete",anchorvalue);
}

function ${SEQPREFIX}URLRegexpDelete(index, anchorvalue)
{
  editjob.${SEQPREFIX}urlregexpnumber.value = index;
  ${SEQPREFIX}SpecOp("${SEQPREFIX}urlregexpop","Delete",anchorvalue);
}

function ${SEQPREFIX}URLRegexpAdd(anchorvalue)
{
  ${SEQPREFIX}SpecOp("${SEQPREFIX}urlregexpop","Add",anchorvalue);
}

function ${SEQPREFIX}checkSpecification()
{
  if (${SEQPREFIX}check_expressions("inclusions",editjob.${SEQPREFIX}inclusions.value) == false)
  {
    editjob.${SEQPREFIX}inclusions.focus();
    return false;
  }
  if (${SEQPREFIX}check_expressions("exclusions",editjob.${SEQPREFIX}exclusions.value) == false)
  {
    editjob.${SEQPREFIX}exclusions.focus();
    return false;
  }
  if (${SEQPREFIX}check_seedsList() == false)
  {
    editjob.${SEQPREFIX}seeds.focus();
    return false;
  }
  return true;
}

function ${SEQPREFIX}check_expressions(thecontext,theexpressionlist)
{
  var rval = true;
  var theArray = theexpressionlist.split("\n");
  var i = 0;
  while (i < theArray.length)
  {
    // For legality check, we must cut out anything useful that is java-only
    var theexp = theArray[i];
    var trimmed = theexp.replace(/^\s+/,"");
    i = i + 1;
    if (trimmed.length == 0 || (trimmed.length >= 1 && trimmed.substring(0,1) == "#"))
      continue;
    try
    {
      var foo = "teststring";
      foo.search(theexp.replace(/\(\?i\)/,""));
    }
    catch (e)
    {
      alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.FoundAnIllegalRegularExpressionIn'))"+thecontext+": '"+theexp+"'$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.ErrorWas'))"+e);
      rval = false;
    }
  }
  return rval;
}

function ${SEQPREFIX}check_seedsList()
{
  var regexp = /http(s)?:\/\/([a-z0-9+!*(),;?&=\$_.-]+(\:[a-z0-9+!*(),;?&=\$_.-]+)?@)?[a-z0-9+\$_-]+(\.[a-z0-9+\$_-]+)*(\:[0-9]{2,5})?(\/([a-z0-9+\$_-]\.?)+)*\/?(\?[a-z+&\$_.-][a-z0-9;:@\/&%=+\$_.-]*)?(#[a-z_.-][a-z0-9+\$_.-]*)?/;
  var lines = editjob.${SEQPREFIX}seeds.value.split("\n");
  var trimmedUrlList = "";
  var invalidUrlList = "";
  var i = 0;
  while (i < lines.length)
  {
    var line = lines[i].replace(/^\s*/, "").replace(/\s*$/, "");
    if (line.length > 0)
    {
      if (!regexp.test(line))
        invalidUrlList = invalidUrlList + line + "\n";
      trimmedUrlList = trimmedUrlList + line + "\n";
    }
    i = i + 1;
  }
  editjob.${SEQPREFIX}seeds.value = trimmedUrlList;
  if (invalidUrlList.length > 0)
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.InvalidUrlsInSeedsList'))\n" + invalidUrlList);
    return false;
  }
  return true;
}

function ${SEQPREFIX}SpecAddToken(anchorvalue)
{
  if (editjob.${SEQPREFIX}spectoken.value == "")
  {
    alert("$Encoder.bodyJavascriptEscape($ResourceBundle.getString('WebcrawlerConnector.TypeInAnAccessToken'))");
    editjob.${SEQPREFIX}spectoken.focus();
    return;
  }
  ${SEQPREFIX}SpecOp("${SEQPREFIX}accessop","Add",anchorvalue);
}
</script>
