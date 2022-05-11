/* $Id: CredentialsDescription.java 988245 2010-08-23 18:39:35Z kwright $ */

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
package com.francelabs.datafari.connectors.webcrawler;

import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.crawler.system.ManifoldCF;
import java.util.*;
import java.util.regex.*;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;

/** This class describes credential information pulled from a configuration.
* The data contained is organized by regular expression performed on a url.  What we store
* for each regular expression is a Pattern, for efficiency.
*
* This structure deals with credentials as applied to a matching set of urls.  It handles sequence-based
* credentials as well as page-based credentials - that is, session-type authentication descriptions as well
* as well as basic/ntlm authentication.  (The two are in fact not mutually exclusive!!)
*
* For page-based credentials, a method is provided that locates the proper credential to use based on the page's url.
*
* For sequence-based credentials, a different method is provided.  This reflects the fact that the underlying functionality
* of sequence-based credentials differs enormously from that of page-based.
*
* Generally it is a good thing to limit the number of regexps that need to be evaluated against
* any given url value as much as possible.  For that reason I've organized this structure
* accordingly.
*/
public class CredentialsDescription
{
  public static final String _rcsid = "@(#)$Id: CredentialsDescription.java 988245 2010-08-23 18:39:35Z kwright $";

  /** This is the hash that contains everything.  It's keyed by the regexp string itself.
  * Values are CredentialsItem objects. */
  protected HashMap patternHash = new HashMap();

  /** Constructor.  Build the description from the ConfigParams. */
  public CredentialsDescription(ConfigParams configData)
    throws ManifoldCFException
  {
    // Scan, looking for bin description nodes
    int i = 0;
    while (i < configData.getChildCount())
    {
      ConfigNode node = configData.getChild(i++);
      if (node.getType().equals(WebcrawlerConfig.NODE_ACCESSCREDENTIAL))
      {
        // Get the url regexp
        String urlDescription = node.getAttributeValue(WebcrawlerConfig.ATTR_URLREGEXP);
        try
        {
          Pattern p;
          try
          {
            p = Pattern.compile(urlDescription,Pattern.UNICODE_CASE);
          }
          catch (java.util.regex.PatternSyntaxException e)
          {
            throw new ManifoldCFException("Access credential regular expression '"+urlDescription+"' is illegal: "+e.getMessage(),e);
          }
          CredentialsItem ti = new CredentialsItem(p);

          String type = node.getAttributeValue(WebcrawlerConfig.ATTR_TYPE);

          // These get used in two of the three types; no harm in fetching them up front.
          String userName = node.getAttributeValue(WebcrawlerConfig.ATTR_USERNAME);
          String password = node.getAttributeValue(WebcrawlerConfig.ATTR_PASSWORD);
          if (password != null)
            password = ManifoldCF.deobfuscate(password);

          if (type.equals(WebcrawlerConfig.ATTRVALUE_BASIC))
            ti.setCredential(new BasicCredential(userName,password));
          else if (type.equals(WebcrawlerConfig.ATTRVALUE_NTLM))
          {
            String domain = node.getAttributeValue(WebcrawlerConfig.ATTR_DOMAIN);
            ti.setCredential(new NTLMCredential(domain,userName,password));
          }
          else if (type.equals(WebcrawlerConfig.ATTRVALUE_SESSION))
          {
            // This is a complex credential type that cannot be easily set up with just a constructor.
            // Use the url regexp as the sequence key; this works as well as anything, although I haven't thought through all the implications if it gets changed.
            SessionCredential sc = new SessionCredential(urlDescription);
            // Loop through child nodes; they describe the pages that belong to the login sequence.
            int j = 0;
            while (j < node.getChildCount())
            {
              ConfigNode child = node.getChild(j++);
              if (child.getType().equals(WebcrawlerConfig.NODE_AUTHPAGE))
              {
                String authPageRegexp = child.getAttributeValue(WebcrawlerConfig.ATTR_URLREGEXP);
                String pageType = child.getAttributeValue(WebcrawlerConfig.ATTR_TYPE);
                String matchRegexp = child.getAttributeValue(WebcrawlerConfig.ATTR_MATCHREGEXP);
		String overrideTargetURL = child.getAttributeValue(WebcrawlerConfig.ATTR_OVERRIDETARGETURL);
		if (overrideTargetURL != null && overrideTargetURL.length() == 0)
		  overrideTargetURL = null;
                Pattern authPattern;
                try
                {
                  authPattern = Pattern.compile(authPageRegexp,Pattern.UNICODE_CASE);
                }
                catch (java.util.regex.PatternSyntaxException e)
                {
                  throw new ManifoldCFException("Authentication page regular expression '"+authPageRegexp+"' is illegal: "+e.getMessage(),e);
                }
                Pattern matchPattern;
                try
                {
                  matchPattern = Pattern.compile(matchRegexp,Pattern.UNICODE_CASE);
                }
                catch (java.util.regex.PatternSyntaxException e)
                {
                  throw new ManifoldCFException("Match regular expression '"+matchRegexp+"' is illegal: "+e.getMessage(),e);
                }
                int authPageIndex = -1;
                if (pageType.equals(WebcrawlerConfig.ATTRVALUE_FORM))
                {
                  authPageIndex = sc.addAuthPage(authPageRegexp,authPattern,overrideTargetURL,null,null,matchRegexp,matchPattern,null,null,null,null);
                }
                else if (pageType.equals(WebcrawlerConfig.ATTRVALUE_LINK))
                {
                  authPageIndex = sc.addAuthPage(authPageRegexp,authPattern,overrideTargetURL,matchRegexp,matchPattern,null,null,null,null,null,null);
                }
                else if (pageType.equals(WebcrawlerConfig.ATTRVALUE_REDIRECTION))
                {
                  authPageIndex = sc.addAuthPage(authPageRegexp,authPattern,overrideTargetURL,null,null,null,null,matchRegexp,matchPattern,null,null);
                }
                else if (pageType.equals(WebcrawlerConfig.ATTRVALUE_CONTENT))
                {
                  authPageIndex = sc.addAuthPage(authPageRegexp,authPattern,overrideTargetURL,null,null,null,null,null,null,matchRegexp,matchPattern);
                }
                else
                  throw new ManifoldCFException("Invalid page type: "+pageType);

                // Finally, walk through any specified parameters
                int k = 0;
                while (k < child.getChildCount())
                {
                  ConfigNode paramNode = child.getChild(k++);
                  if (paramNode.getType().equals(WebcrawlerConfig.NODE_AUTHPARAMETER))
                  {
                    String paramName = paramNode.getAttributeValue(WebcrawlerConfig.ATTR_NAMEREGEXP);
                    Pattern paramNamePattern;
                    try
                    {
                      paramNamePattern = Pattern.compile(paramName,Pattern.UNICODE_CASE);
                    }
                    catch (java.util.regex.PatternSyntaxException e)
                    {
                      throw new ManifoldCFException("Parameter name regular expression '"+paramName+"' is illegal: "+e.getMessage(),e);
                    }
                    String passwordValue = paramNode.getAttributeValue(WebcrawlerConfig.ATTR_PASSWORD);
                    String paramValue = paramNode.getAttributeValue(WebcrawlerConfig.ATTR_VALUE);
                    if (passwordValue != null)
                      paramValue = ManifoldCF.deobfuscate(passwordValue);
                    sc.addPageParameter(authPageIndex,authPageRegexp,paramName,paramNamePattern,paramValue);
                  }
                }
              }
            }
            ti.setCredential(sc);
          }
          else
            throw new ManifoldCFException("Illegal credential type: "+type);
          patternHash.put(urlDescription,ti);
        }
        catch (PatternSyntaxException e)
        {
          throw new ManifoldCFException("Bad pattern syntax in '"+urlDescription+"'",e);
        }
      }
    }
  }

  /** Given a URL, find the right PageCredentials object to use.  If more than one match is found,
  * use NEITHER object.
  */
  public PageCredentials getPageCredential(String url)
  {
    PageCredentials c = null;
    Iterator iter = patternHash.keySet().iterator();
    while (iter.hasNext())
    {
      String urlDescription = (String)iter.next();
      CredentialsItem ti = (CredentialsItem)patternHash.get(urlDescription);
      Pattern p = ti.getPattern();
      AuthenticationCredentials ac = ti.getCredential();
      if (ac instanceof PageCredentials)
      {
        Matcher m = p.matcher(url);
        if (m.find())
        {
          if (c != null)
            return null;
          c = (PageCredentials)ac;
        }
      }
    }
    return c;
  }

  /** Given a URL, find the right SequenceCredentials object to use.  If more than one match is found,
  * use NEITHER object.
  */
  public SequenceCredentials getSequenceCredential(String url)
  {
    SequenceCredentials c = null;
    Iterator iter = patternHash.keySet().iterator();
    while (iter.hasNext())
    {
      String urlDescription = (String)iter.next();
      CredentialsItem ti = (CredentialsItem)patternHash.get(urlDescription);
      Pattern p = ti.getPattern();
      AuthenticationCredentials ac = ti.getCredential();
      if (ac instanceof SequenceCredentials)
      {
        Matcher m = p.matcher(url);
        if (m.find())
        {
          if (c != null)
            return null;
          c = (SequenceCredentials)ac;
        }
      }
    }
    return c;
  }

  /** Class representing an individual credential item.
  */
  protected static class CredentialsItem
  {
    /** The bin-matching pattern. */
    protected Pattern pattern;
    /** The credential */
    protected AuthenticationCredentials authentication;

    /** Constructor. */
    public CredentialsItem(Pattern p)
    {
      pattern = p;
    }

    /** Get the pattern. */
    public Pattern getPattern()
    {
      return pattern;
    }

    /** Set Credentials */
    public void setCredential(AuthenticationCredentials authentication)
    {
      this.authentication = authentication;
    }

    /** Get credential type */
    public AuthenticationCredentials getCredential()
    {
      return authentication;
    }

  }

  /** Session credential parameter class */
  protected static class SessionCredentialParameter
  {
    /** Name regexp */
    protected String nameRegexp;
    /** Compiled name pattern */
    protected Pattern namePattern;
    /** Value **/
    protected String value;

    public SessionCredentialParameter(String nameRegexp, Pattern namePattern, String value)
    {
      this.nameRegexp = nameRegexp;
      this.namePattern = namePattern;
      this.value = value;
    }

    public Pattern getNamePattern()
    {
      return namePattern;
    }

    public String getValue()
    {
      return value;
    }

    public boolean equals(Object o)
    {
      if (!(o instanceof SessionCredentialParameter))
        return false;
      SessionCredentialParameter sc = (SessionCredentialParameter)o;
      return nameRegexp.equals(sc.nameRegexp) && value.equals(sc.value);
    }

    public int hashCode()
    {
      return nameRegexp.hashCode() + value.hashCode();
    }
  }

  /** Session credential helper class */
  protected static class SessionCredentialItem implements LoginParameters
  {
    /** url regexp */
    protected final String regexp;
    /** Url match pattern */
    protected final Pattern pattern;
    /** Override target URL */
    protected final String overrideTargetURL;
    /** The preferred redirection regexp */
    protected final String preferredRedirectionRegexp;
    /** The preferred redirection pattern, or null if there's no preferred redirection */
    protected final Pattern preferredRedirectionPattern;
    /** The preferred link regexp */
    protected final String preferredLinkRegexp;
    /** The preferred link pattern, or null if there's no preferred link */
    protected final Pattern preferredLinkPattern;
    /** The form name regexp */
    protected final String formNameRegexp;
    /** The form name pattern, or null if no form is expected */
    protected final Pattern formNamePattern;
    /** The content regexp */
    protected final String contentRegexp;
    /** The content pattern, or null if no content is sought for */
    protected final Pattern contentPattern;
    
    /** The list of the parameters we want to add for this pattern. */
    protected final List parameters = new ArrayList();

    /** Constructor */
    public SessionCredentialItem(String regexp, Pattern p,
      String overrideTargetURL,
      String preferredLinkRegexp, Pattern preferredLinkPattern,
      String formNameRegexp, Pattern formNamePattern,
      String preferredRedirectionRegexp, Pattern preferredRedirectionPattern,
      String contentRegexp, Pattern contentPattern)
    {
      this.regexp = regexp;
      this.pattern = p;
      this.overrideTargetURL = overrideTargetURL;
      this.preferredLinkRegexp = preferredLinkRegexp;
      this.preferredLinkPattern = preferredLinkPattern;
      this.formNameRegexp = formNameRegexp;
      this.formNamePattern = formNamePattern;
      this.preferredRedirectionRegexp = preferredRedirectionRegexp;
      this.preferredRedirectionPattern = preferredRedirectionPattern;
      this.contentRegexp = contentRegexp;
      this.contentPattern = contentPattern;
    }

    /** Add parameter */
    public void addParameter(String nameRegexp, Pattern namePattern, String value)
    {
      parameters.add(new SessionCredentialParameter(nameRegexp,namePattern,value));
    }

    /** Get the pattern */
    public Pattern getPattern()
    {
      return pattern;
    }

    /** Get the override target URL.
    */
    public String getOverrideTargetURL()
    {
      return overrideTargetURL;
    }

    /** Get the preferred redirection pattern.
    */
    public Pattern getPreferredRedirectionPattern()
    {
      return preferredRedirectionPattern;
    }

    /** Get the preferred link pattern.
    */
    public Pattern getPreferredLinkPattern()
    {
      return preferredLinkPattern;
    }

    /** Get the form name pattern.
    */
    public Pattern getFormNamePattern()
    {
      return formNamePattern;
    }

    /** Get the content pattern.
    */
    public Pattern getContentPattern()
    {
      return contentPattern;
    }

    /** Get the name of the i'th parameter.
    */
    public Pattern getParameterNamePattern(int index)
    {
      return getParameter(index).getNamePattern();
    }

    /** Get the desired value of the i'th parameter.
    */
    public String getParameterValue(int index)
    {
      return getParameter(index).getValue();
    }

    /** Get the parameter count */
    public int getParameterCount()
    {
      return parameters.size();
    }

    /** Get the actual parameter */
    public SessionCredentialParameter getParameter(int index)
    {
      return (SessionCredentialParameter)parameters.get(index);
    }

    public boolean equals(Object o)
    {
      if (!(o instanceof SessionCredentialItem))
        return false;
      SessionCredentialItem sci = (SessionCredentialItem)o;
      if (!regexp.equals(sci.regexp))
        return false;

      if (preferredRedirectionRegexp == null || sci.preferredRedirectionRegexp == null)
      {
        if (preferredRedirectionRegexp != sci.preferredRedirectionRegexp)
          return false;
      }
      else if (!preferredRedirectionRegexp.equals(sci.preferredRedirectionRegexp))
        return false;

      if (preferredLinkRegexp == null || sci.preferredLinkRegexp == null)
      {
        if (preferredLinkRegexp != sci.preferredLinkRegexp)
          return false;
      }
      else if (!preferredLinkRegexp.equals(sci.preferredLinkRegexp))
        return false;

      if (formNameRegexp == null || sci.formNameRegexp == null)
      {
        if (formNameRegexp != sci.formNameRegexp)
          return false;
      }
      else if (!formNameRegexp.equals(sci.formNameRegexp))
        return false;

      if (contentRegexp == null || sci.contentRegexp == null)
      {
        if (contentRegexp != sci.contentRegexp)
          return false;
      }
      else if (!contentRegexp.equals(sci.contentRegexp))
        return false;

      if (parameters.size() != sci.parameters.size())
        return false;
      int i = 0;
      while (i < parameters.size())
      {
        if (!((SessionCredentialParameter)parameters.get(i)).equals((SessionCredentialParameter)sci.parameters.get(i)))
          return false;
        i++;
      }

      return true;
    }

    public int hashCode()
    {
      int rval = regexp.hashCode() + ((preferredRedirectionRegexp==null)?0:preferredRedirectionRegexp.hashCode()) +
        ((preferredLinkRegexp==null)?0:preferredLinkRegexp.hashCode()) +
        ((formNameRegexp==null)?0:formNameRegexp.hashCode()) +
	((contentRegexp==null)?0:contentRegexp.hashCode());
      int i = 0;
      while (i < parameters.size())
      {
        rval += parameters.get(i).hashCode();
        i++;
      }
      return rval;
    }

  }

  /** LoginParameter iterator */
  protected static class LoginParameterIterator implements Iterator
  {
    protected final List<SessionCredentialItem> sessionPages;
    protected final String documentIdentifier;
    protected int sessionCredentialIndex = -1;
    protected LoginParameters currentOne = null;

    /** Constructor */
    public LoginParameterIterator(List<SessionCredentialItem> sessionPages, String documentIdentifier)
    {
      this.sessionPages = sessionPages;
      this.documentIdentifier = documentIdentifier;
    }

    /** Find next one */
    protected void findNextOne()
    {
      if (currentOne != null)
        return;
      if (sessionCredentialIndex == -1) {
        sessionCredentialIndex = 0;
      } else {
        sessionCredentialIndex++;
      }
      while (sessionCredentialIndex < sessionPages.size())
      {
        SessionCredentialItem sci = sessionPages.get(sessionCredentialIndex);
        Matcher m = sci.getPattern().matcher(documentIdentifier);
        if (m.find())
        {
          currentOne = sci;
          return;
        }
        sessionCredentialIndex++;
      }
    }

    /** Check for next */
    @Override
    public boolean hasNext()
    {
      findNextOne();
      return (currentOne != null);
    }

    /** Get the next one */
    @Override
    public Object next()
    {
      findNextOne();
      Object rval = currentOne;
      currentOne = null;
      return rval;
    }

    @Override
    public void remove()
    {
      throw new Error("Unimplemented function");
    }
  }

  /** Session credentials */
  protected static class SessionCredential implements SequenceCredentials
  {
    protected final String sequenceKey;
    protected final List<SessionCredentialItem> sessionPages = new ArrayList<>();

    /** Constructor */
    public SessionCredential(String sequenceKey)
    {
      this.sequenceKey = sequenceKey;
    }

    /** Add an auth page */
    public int addAuthPage(String urlregexp, Pattern urlPattern,
      String overrideTargetURL,
      String preferredLinkRegexp, Pattern preferredLinkPattern,
      String formNameRegexp, Pattern formNamePattern,
      String preferredRedirectionRegexp, Pattern preferredRedirectionPattern,
      String contentRegexp, Pattern contentPattern)
      throws ManifoldCFException
    {
      int rval = sessionPages.size();
      sessionPages.add(new SessionCredentialItem(urlregexp,urlPattern,
	overrideTargetURL,
        preferredLinkRegexp,preferredLinkPattern,
        formNameRegexp,formNamePattern,
        preferredRedirectionRegexp,preferredRedirectionPattern,
	contentRegexp,contentPattern));
      return rval;
    }

    /** Add a page parameter */
    public void addPageParameter(int authPageIndex, String urlregexp, String paramNameRegexp, Pattern paramNamePattern, String paramValue)
    {
      SessionCredentialItem sci = sessionPages.get(authPageIndex);
      sci.addParameter(paramNameRegexp,paramNamePattern,paramValue);
    }

    /** Fetch the unique key value for this particular credential.  (This is used to enforce the proper page ordering).
    */
    @Override
    public String getSequenceKey()
    {
      return sequenceKey;
    }

    /** For a given login page, specific information may need to be submitted to the server to properly log in.  This information
    * must be specified as part of the login sequence description information.
    * If null is returned, then this page has no specific login information.
    */
    @Override
    public Iterator findLoginParameters(String documentIdentifier)
      throws ManifoldCFException
    {
      return new LoginParameterIterator(sessionPages,documentIdentifier);
    }

    /** Compare against another object */
    public boolean equals(Object o)
    {
      if (!(o instanceof SessionCredential))
        return false;
      SessionCredential b = (SessionCredential)o;
      if (b.sessionPages.size() != sessionPages.size())
        return false;
      for (int i = 0; i < sessionPages.size(); i++) {
        SessionCredentialItem sci = sessionPages.get(i);
        SessionCredentialItem bsci = b.sessionPages.get(i);
        if (bsci == null)
          return false;
        if (!sci.equals(bsci))
          return false;
      }
      return true;
    }

    /** Calculate a hash function */
    public int hashCode()
    {
      int rval = 0;
      for (final SessionCredentialItem sci : sessionPages) {
        rval += sci.hashCode();
      }
      return rval;
    }

  }

  /** Basic type credentials */
  protected static class BasicCredential implements PageCredentials
  {
    protected String userName;
    protected String password;
    protected UsernamePasswordCredentials credentialsObject;

    /** Constructor */
    public BasicCredential(String userName, String password)
    {
      this.userName = userName;
      this.password = password;
      credentialsObject = new UsernamePasswordCredentials(userName,password);
    }

    /** Turn this instance into a Credentials object, given the specified target host name */
    public Credentials makeCredentialsObject(String targetHostName)
      throws ManifoldCFException
    {
      return credentialsObject;
    }

    /** Compare against another object */
    public boolean equals(Object o)
    {
      if (!(o instanceof BasicCredential))
        return false;
      BasicCredential b = (BasicCredential)o;
      return b.userName.equals(userName) && b.password.equals(password);
    }

    /** Calculate a hash function */
    public int hashCode()
    {
      return userName.hashCode() + password.hashCode();
    }
  }

  /** NTLM-style credentials */
  protected static class NTLMCredential implements PageCredentials
  {
    protected String domain;
    protected String userName;
    protected String password;
    // No Credentials object here because it depends on the hostname

    /** Constructor */
    public NTLMCredential(String domain, String userName, String password)
    {
      this.domain = domain;
      this.userName = userName;
      this.password = password;
    }

    /** Turn this instance into a Credentials object, given the specified target host name */
    public Credentials makeCredentialsObject(String targetHostName)
      throws ManifoldCFException
    {
      return new NTCredentials(userName,password,targetHostName,domain);
    }

    /** Compare against another object */
    public boolean equals(Object o)
    {
      if (!(o instanceof NTLMCredential))
        return false;
      NTLMCredential b = (NTLMCredential)o;
      return b.userName.equals(userName) && b.password.equals(password) &&
        b.domain.equals(domain);
    }

    /** Calculate a hash function */
    public int hashCode()
    {
      return userName.hashCode() + password.hashCode() + domain.hashCode();
    }
  }

}
