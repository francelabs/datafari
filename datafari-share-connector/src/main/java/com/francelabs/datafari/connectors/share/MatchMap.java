/* $Id: MatchMap.java 988245 2010-08-23 18:39:35Z kwright $ */

/**
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
*/
package com.francelabs.datafari.connectors.share;

import org.apache.manifoldcf.core.interfaces.*;
import java.util.*;
import java.util.regex.*;

/** An instance of this class describes a "match map", which describes a translation of an input
* string using regexp technology.
* A match map consists of multiple clauses, which are fired in sequence.  Each clause is a regexp
* search and replace, where the replace string can include references to the groups present in the
* search regexp.
* MatchMaps can be converted to strings in two different ways.  The first way is to build a single
* string of the form "match1=replace1&match2=replace2...".  Strings of this kind must escape & and =
* characters in the match and replace strings, where found.  The second way is to generate an array
* of match strings and a corresponding array of replace strings.  This method requires no escaping
* of the string contents.
*/
public class MatchMap
{
  public static final String _rcsid = "@(#)$Id: MatchMap.java 988245 2010-08-23 18:39:35Z kwright $";

  /** This is the set of match regexp strings */
  protected ArrayList matchStrings;
  /** This is the set of Pattern objects corresponding to the match regexp strings.
  * It's null if the patterns have not been built yet. */
  protected Pattern[] matchPatterns = null;
  /** This is the set of replace strings */
  protected ArrayList replaceStrings;

  /** Constructor.  Build an empty matchmap. */
  public MatchMap()
  {
    matchStrings = new ArrayList();
    replaceStrings = new ArrayList();
  }

  /** Constructor.  Build a matchmap from a single string. */
  public MatchMap(String stringForm)
  {
    matchStrings = new ArrayList();
    replaceStrings = new ArrayList();
    StringBuilder matchString = new StringBuilder();
    StringBuilder replaceString = new StringBuilder();
    int i = 0;
    while (i < stringForm.length())
    {
      matchString.setLength(0);
      replaceString.setLength(0);
      while (i < stringForm.length())
      {
        char x = stringForm.charAt(i);
        if (x == '&' || x == '=')
          break;
        i++;
        if (x == '\\' && i < stringForm.length())
          x = stringForm.charAt(i++);
        matchString.append(x);
      }

      if (i < stringForm.length())
      {
        char x = stringForm.charAt(i);
        if (x == '=')
        {
          i++;
          // Pick up the second string
          while (i < stringForm.length())
          {
            x = stringForm.charAt(i);
            if (x == '&')
              break;
            i++;
            if (x == '\\' && i < stringForm.length())
              x = stringForm.charAt(i++);
            replaceString.append(x);
          }
        }
      }

      matchStrings.add(matchString.toString());
      replaceStrings.add(replaceString.toString());

      if (i < stringForm.length())
      {
        char x = stringForm.charAt(i);
        if (x == '&')
          i++;
      }
    }
  }

  /** Constructor.  Build a matchmap from two arraylists representing match and replace strings */
  public MatchMap(ArrayList matchStrings, ArrayList replaceStrings)
  {
    this.matchStrings = (ArrayList)matchStrings.clone();
    this.replaceStrings = (ArrayList)replaceStrings.clone();
  }

  /** Get the number of match/replace strings */
  public int getMatchCount()
  {
    return matchStrings.size();
  }

  /** Get a specific match string */
  public String getMatchString(int index)
  {
    return (String)matchStrings.get(index);
  }

  /** Get a specific replace string */
  public String getReplaceString(int index)
  {
    return (String)replaceStrings.get(index);
  }

  /** Delete a specified match/replace string pair */
  public void deleteMatchPair(int index)
  {
    matchStrings.remove(index);
    replaceStrings.remove(index);
    matchPatterns = null;
  }

  /** Insert a match/replace string pair */
  public void insertMatchPair(int index, String match, String replace)
  {
    matchStrings.add(index,match);
    replaceStrings.add(index,replace);
    matchPatterns = null;
  }

  /** Append a match/replace string pair */
  public void appendMatchPair(String match, String replace)
  {
    matchStrings.add(match);
    replaceStrings.add(replace);
    matchPatterns = null;
  }

  /** Append old-style match/replace pair.
  * This method translates old-style regexp and group output form to the
  * current style before adding to the map.
  */
  public void appendOldstyleMatchPair(String oldstyleMatch, String oldstyleReplace)
  {
    String newStyleMatch = "^" + oldstyleMatch + "$";

    // Need to build a new-style replace string from the old one.  To do that, use the
    // original parser (which basically will guarantee that we get it right)

    EvaluatorTokenStream et = new EvaluatorTokenStream(oldstyleReplace);
    StringBuilder newStyleReplace = new StringBuilder();

    while (true)
    {
      EvaluatorToken t = et.peek();
      if (t == null)
        break;
      switch (t.getType())
      {
      case EvaluatorToken.TYPE_COMMA:
        et.advance();
        break;
      case EvaluatorToken.TYPE_GROUP:
        et.advance();
        int groupNumber = t.getGroupNumber();
        switch (t.getGroupStyle())
        {
        case EvaluatorToken.GROUPSTYLE_NONE:
          newStyleReplace.append("$(").append(Integer.toString(groupNumber)).append(")");
          break;
        case EvaluatorToken.GROUPSTYLE_LOWER:
          newStyleReplace.append("$(").append(Integer.toString(groupNumber)).append("l)");
          break;
        case EvaluatorToken.GROUPSTYLE_UPPER:
          newStyleReplace.append("$(").append(Integer.toString(groupNumber)).append("u)");
          break;
        case EvaluatorToken.GROUPSTYLE_MIXED:
          newStyleReplace.append("$(").append(Integer.toString(groupNumber)).append("m)");
          break;
        default:
          break;
        }
        break;
      case EvaluatorToken.TYPE_TEXT:
        et.advance();
        escape(newStyleReplace,t.getTextValue());
        break;
      default:
        break;
      }
    }

    appendMatchPair(newStyleMatch,newStyleReplace.toString());
  }

  /** Escape a string so it is verbatim */
  protected static void escape(StringBuilder output, String input)
  {
    int i = 0;
    while (i < input.length())
    {
      char x = input.charAt(i++);
      if (x == '$')
        output.append(x);
      output.append(x);
    }
  }

  /** Convert the matchmap to string form. */
  public String toString()
  {
    int i = 0;
    StringBuilder rval = new StringBuilder();
    while (i < matchStrings.size())
    {
      String matchString = (String)matchStrings.get(i);
      String replaceString = (String)replaceStrings.get(i);
      if (i > 0)
        rval.append('&');
      stuff(rval,matchString);
      rval.append('=');
      stuff(rval,replaceString);
      i++;
    }
    return rval.toString();
  }

  /** Stuff characters */
  protected static void stuff(StringBuilder sb, String value)
  {
    int i = 0;
    while (i < value.length())
    {
      char x = value.charAt(i++);
      if (x == '\\' || x == '&' || x == '=')
        sb.append('\\');
      sb.append(x);
    }
  }

  /** Perform a translation.
  */
  public String translate(String input)
    throws ManifoldCFException
  {
    // Build pattern vector if not already there
    if (matchPatterns == null)
    {
      matchPatterns = new Pattern[matchStrings.size()];
      int i = 0;
      while (i < matchPatterns.length)
      {
        String regexp = (String)matchStrings.get(i);
        try
        {
          matchPatterns[i] = Pattern.compile(regexp);
        }
        catch (java.util.regex.PatternSyntaxException e)
        {
          matchPatterns = null;
          throw new ManifoldCFException("For match expression '"+regexp+"', found pattern syntax error: "+e.getMessage(),e);
        }
        i++;
      }
    }

    int j = 0;
    while (j < matchPatterns.length)
    {
      Pattern p = matchPatterns[j];
      // Construct a matcher
      Matcher m = p.matcher(input);
      // Grab the output description
      String outputDescription = (String)replaceStrings.get(j);
      j++;
      // Create a copy buffer
      StringBuilder outputBuffer = new StringBuilder();
      // Keep track of the index in the original string we have done up to
      int currentIndex = 0;
      // Scan the string using find, and for each one found, do a translation
      while (true)
      {
        boolean foundOne = m.find();
        if (foundOne == false)
        {
          // No subsequent match found.
          // Copy everything from currentIndex until the end of input
          outputBuffer.append(input.substring(currentIndex));
          break;
        }

        // Do a translation.  This involves copying everything in the input
        // string up until the start of the match, then doing a replace for
        // the match itself, and finally setting the currentIndex to the end
        // of the match.

        int matchStart = m.start(0);
        int matchEnd = m.end(0);
        if (matchStart == -1)
        {
          // The expression was degenerate; treat this as the end.
          outputBuffer.append(input.substring(currentIndex));
          break;
        }
        outputBuffer.append(input.substring(currentIndex,matchStart));

        // Process translation description!
        int i = 0;
        while (i < outputDescription.length())
        {
          char x = outputDescription.charAt(i++);
          if (x == '$' && i < outputDescription.length())
          {
            x = outputDescription.charAt(i++);
            if (x == '(')
            {
              // Process evaluation expression
              StringBuilder numberBuf = new StringBuilder();
              boolean upper = false;
              boolean lower = false;
              boolean mixed = false;
              while (i < outputDescription.length())
              {
                char y = outputDescription.charAt(i++);
                if (y == ')')
                  break;
                else if (y >= '0' && y <= '9')
                  numberBuf.append(y);
                else if (y == 'u' || y == 'U')
                  upper = true;
                else if (y == 'l' || y == 'L')
                  lower = true;
                else if (y == 'm' || y == 'M')
                  mixed = true;
              }
              String number = numberBuf.toString();
              try
              {
                int groupnum = Integer.parseInt(number);
                String groupValue = m.group(groupnum);
                if (upper)
                  outputBuffer.append(groupValue.toUpperCase(Locale.ROOT));
                else if (lower)
                  outputBuffer.append(groupValue.toLowerCase(Locale.ROOT));
                else if (mixed && groupValue.length() > 0)
                  outputBuffer.append(groupValue.substring(0,1).toUpperCase(Locale.ROOT)).append(groupValue.substring(1).toLowerCase(Locale.ROOT));
                else
                  outputBuffer.append(groupValue);

              }
              catch (NumberFormatException e)
              {
                // Silently skip, because it's an illegal group number, so nothing
                // gets added.
              }

              // Go back around, so we don't add the $ in
              continue;
            }
          }
          outputBuffer.append(x);
        }

        currentIndex = matchEnd;
      }

      input = outputBuffer.toString();
    }

    return input;
  }


  // Protected classes

  // These classes are used to process the old token-based replacement strings

  /** Evaluator token.
  */
  protected static class EvaluatorToken
  {
    public final static int TYPE_GROUP = 0;
    public final static int TYPE_TEXT = 1;
    public final static int TYPE_COMMA = 2;

    public final static int GROUPSTYLE_NONE = 0;
    public final static int GROUPSTYLE_LOWER = 1;
    public final static int GROUPSTYLE_UPPER = 2;
    public final static int GROUPSTYLE_MIXED = 3;

    protected int type;
    protected int groupNumber = -1;
    protected int groupStyle = GROUPSTYLE_NONE;
    protected String textValue = null;

    public EvaluatorToken()
    {
      type = TYPE_COMMA;
    }

    public EvaluatorToken(int groupNumber, int groupStyle)
    {
      type = TYPE_GROUP;
      this.groupNumber = groupNumber;
      this.groupStyle = groupStyle;
    }

    public EvaluatorToken(String text)
    {
      type = TYPE_TEXT;
      this.textValue = text;
    }

    public int getType()
    {
      return type;
    }

    public int getGroupNumber()
    {
      return groupNumber;
    }

    public int getGroupStyle()
    {
      return groupStyle;
    }

    public String getTextValue()
    {
      return textValue;
    }

  }


  /** Token stream.
  */
  protected static class EvaluatorTokenStream
  {
    protected String text;
    protected int pos;
    protected EvaluatorToken token = null;

    /** Constructor.
    */
    public EvaluatorTokenStream(String text)
    {
      this.text = text;
      this.pos = 0;
    }

    /** Get current token.
    */
    public EvaluatorToken peek()
    {
      if (token == null)
      {
        token = nextToken();
      }
      return token;
    }

    /** Go on to next token.
    */
    public void advance()
    {
      token = null;
    }

    protected EvaluatorToken nextToken()
    {
      char x;
      // Fetch the next token
      while (true)
      {
        if (pos == text.length())
          return null;
        x = text.charAt(pos);
        if (x > ' ')
          break;
        pos++;
      }

      StringBuilder sb;

      if (x == '"')
      {
        // Parse text
        pos++;
        sb = new StringBuilder();
        while (true)
        {
          if (pos == text.length())
            break;
          x = text.charAt(pos);
          pos++;
          if (x == '"')
          {
            break;
          }
          if (x == '\\')
          {
            if (pos == text.length())
              break;
            x = text.charAt(pos++);
          }
          sb.append(x);
        }

        return new EvaluatorToken(sb.toString());
      }

      if (x == ',')
      {
        pos++;
        return new EvaluatorToken();
      }

      // Eat number at beginning
      sb = new StringBuilder();
      while (true)
      {
        if (pos == text.length())
          break;
        x = text.charAt(pos);
        if (x >= '0' && x <= '9')
        {
          sb.append(x);
          pos++;
          continue;
        }
        break;
      }
      String numberValue = sb.toString();
      int groupNumber = 0;
      if (numberValue.length() > 0)
        groupNumber = new Integer(numberValue).intValue();
      // Save the next char position
      int modifierPos = pos;
      // Go to the end of the word
      while (true)
      {
        if (pos == text.length())
          break;
        x = text.charAt(pos);
        if (x == ',' || x >= '0' && x <= '9' || x <= ' ' && x >= 0)
          break;
        pos++;
      }

      int style = EvaluatorToken.GROUPSTYLE_NONE;
      if (modifierPos != pos)
      {
        String modifier = text.substring(modifierPos,pos);
        if (modifier.startsWith("u"))
          style = EvaluatorToken.GROUPSTYLE_UPPER;
        else if (modifier.startsWith("l"))
          style = EvaluatorToken.GROUPSTYLE_LOWER;
        else if (modifier.startsWith("m"))
          style = EvaluatorToken.GROUPSTYLE_MIXED;
      }
      return new EvaluatorToken(groupNumber,style);
    }
  }

}
