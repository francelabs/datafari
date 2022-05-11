/* $Id: WebcrawlerConfig.java 995042 2010-09-08 13:10:06Z kwright $ */

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


/** Constants for the Webcrawler connector configuration.
*/
public class WebcrawlerConfig
{
  public static final String _rcsid = "@(#)$Id: WebcrawlerConfig.java 995042 2010-09-08 13:10:06Z kwright $";


  // Constants describing the configuration structure.  This structure describes the "how" of
  // fetching page data - e.g. bandwidth and fetch constraints, adherance to robots conventions,
  // etc.
  // For the throttling part of the connector, the scheduler handles average fetch-rate limits.
  // The per-connection configuration describes the maximum number of connections per some user-defined criteria, as
  // well as bandwidth maximums and fetch rate absolute maximums.
  //
  // In detail:
  //
  // 1) The robots conventions;
  // 2) Bandwidth limits in KB/sec, based on regular expressions done on the bins;
  // 3) Email address (so people can whine to somebody about our crawler);
  // 4) Maximum number of connections per host, based on a regular expression done on the bins a document belongs to.
  // 5) Authentication information (NTLM and basic auth only), based on regexp of a document's URL.
  // 6) SSL trust store certificates, trusted on the basis of a regexp of a document's URL.

  /** Robots usage (a parameter) */
  public static final String PARAMETER_ROBOTSUSAGE = "Robots usage";
  /** Meta robots tags usage (a parameter) */
  public static final String PARAMETER_META_ROBOTS_TAGS_USAGE = "Meta robots tags usage";
  /** Email (a parameter) */
  public static final String PARAMETER_EMAIL = "Email address";
  /** Proxy host name (parameter) */
  public static final String PARAMETER_PROXYHOST = "Proxy host";
  /** Proxy port (parameter) */
  public static final String PARAMETER_PROXYPORT = "Proxy port";
  /** Proxy auth domain (parameter) */
  public static final String PARAMETER_PROXYAUTHDOMAIN = "Proxy authentication domain";
  /** Proxy auth username (parameter) */
  public static final String PARAMETER_PROXYAUTHUSERNAME = "Proxy authentication user name";
  /** Proxy auth password (parameter) */
  public static final String PARAMETER_PROXYAUTHPASSWORD = "Proxy authentication password";
  /** The bin description node */
  public static final String NODE_BINDESC = "bindesc";
  /** The bin regular expression */
  public static final String ATTR_BINREGEXP = "binregexp";
  /** Whether the match is case insensitive */
  public static final String ATTR_INSENSITIVE = "caseinsensitive";
  /** The max connections node */
  public static final String NODE_MAXCONNECTIONS = "maxconnections";
  /** The bandwidth node */
  public static final String NODE_MAXKBPERSECOND = "maxkbpersecond";
  /** The max fetch rate node */
  public static final String NODE_MAXFETCHESPERMINUTE = "maxfetchesperminute";
  /** The value attribute (used for maxconnections and maxkbpersecond) */
  public static final String ATTR_VALUE = "value";
  /** Access control description node */
  public static final String NODE_ACCESSCREDENTIAL = "accesscredential";
  /** Regexp for access control node */
  public static final String ATTR_URLREGEXP = "urlregexp";
  /** Type of security  */
  public static final String ATTR_TYPE = "type";
  /** Type value for basic authentication */
  public static final String ATTRVALUE_BASIC = "basic";
  /** Type value for NTLM authentication */
  public static final String ATTRVALUE_NTLM = "ntlm";
  /** Type value for session-based authentication */
  public static final String ATTRVALUE_SESSION = "session";
  /** Domain/realm part of credentials (if any) */
  public static final String ATTR_DOMAIN = "domain";
  /** Username part of credentials */
  public static final String ATTR_USERNAME = "username";
  /** Password part of credentials */
  public static final String ATTR_PASSWORD = "password";
  /** Authentication page description node */
  public static final String NODE_AUTHPAGE = "authpage";
  /** Authentication page type: Form */
  public static final String ATTRVALUE_FORM = "form";
  /** Authentication page type: Link */
  public static final String ATTRVALUE_LINK = "link";
  /** Authentication page type: Redirection */
  public static final String ATTRVALUE_REDIRECTION = "redirection";
  /** Authentication page type: Access */
  public static final String ATTRVALUE_CONTENT = "content";
  /** Form name or link target regexp for authentication page */
  public static final String ATTR_MATCHREGEXP = "match";
  /** URL to fetch next in a sequence (an override) */
  public static final String ATTR_OVERRIDETARGETURL = "overridetargeturl";
  /** Authentication parameter node */
  public static final String NODE_AUTHPARAMETER = "authparameter";
  /** Authentication parameter name regexp */
  public static final String ATTR_NAMEREGEXP = "name";
  /** Trust store description node */
  public static final String NODE_TRUST = "trust";
  /** Trust store section of authentication record */
  public static final String ATTR_TRUSTSTORE = "truststore";
  /** "Trust everything" attribute - replacing truststore if set to 'true' */
  public static final String ATTR_TRUSTEVERYTHING = "trusteverything";

  // Constants used in the document specification part of the configuration structure.
  // This describes the "what" of the job.

  /** Map entry specification node.  Has two attributes: 'match' and 'map'. */
  public static final String NODE_MAP = "map";
  /** The seeds node.  The value of this node contains the seeds, as a large
  * text area. */
  public static final String NODE_SEEDS = "seeds";
  /** Include regexps node.  The value of this node contains the regexps that
  * must match the canonical URL in order for that URL to be included in the crawl.  These
  * regexps are newline separated, and # starts a comment.  */
  public static final String NODE_INCLUDES = "includes";
  /** Exclude regexps node.  The value of this node contains the regexps that
  * if any one matches, causes the URL to be excluded from the crawl.  These
  * regexps are newline separated, and # starts a comment.  */
  public static final String NODE_EXCLUDES = "excludes";
  /** Include regexps node.  The value of this node contains the regexps that
  * must match the canonical URL in order for that URL to be included for indexing.  These
  * regexps are newline separated, and # starts a comment.  */
  public static final String NODE_INCLUDESINDEX = "includesindex";
  /** Exclude regexps node.  The value of this node contains the regexps that
  * if any one matches, causes the URL to be excluded from indexing.  These
  * regexps are newline separated, and # starts a comment.  */
  public static final String NODE_EXCLUDESINDEX = "excludesindex";

  /**
   * Exclude any page containing specified regex in their body from index
   */
  public static final String NODE_EXCLUDESCONTENTINDEX = "excludescontentindex";

  /** Limit to seeds.  When value attribute is true, only seed domains will be permitted. */
  public static final String NODE_LIMITTOSEEDS = "limittoseeds";
  /** Canonicalization rule.  Attributes are regexp, description, reorder, 
  *javasessionremoval, aspsessionremoval, phpsessionremoval, bvsessionremoval */
  public static final String NODE_URLSPEC = "urlspec";
  /** Forced acl access token node.  Attribute is "token". */
  public static final String NODE_ACCESS = "access";
  /** Exclude header node.  The value of this node lists a single header (in lower case) that 
  * should be excluded from the document metadata */
  public static final String NODE_EXCLUDEHEADER = "excludeheader";
  
  /** regexp attribute */
  public static final String ATTR_REGEXP = "regexp";
  /** description attribute */
  public static final String ATTR_DESCRIPTION = "description";
  /** reorder attribute */
  public static final String ATTR_REORDER = "reorder";
  /** javasessionremoval attribute */
  public static final String ATTR_JAVASESSIONREMOVAL = "javasessionremoval";
  /** aspsessionremoval attribute */
  public static final String ATTR_ASPSESSIONREMOVAL = "aspsessionremoval";
  /** phpsessionremoval attribute */
  public static final String ATTR_PHPSESSIONREMOVAL = "phpsessionremoval";
  /** bvsessionremoval attribute */
  public static final String ATTR_BVSESSIONREMOVAL = "bvsessionremoval";
  /** map to lower case */
  public static final String ATTR_LOWERCASE = "lowercase";
  /** name attribute */
  public static final String ATTR_NAME = "name";
  /** token attribute */
  public static final String ATTR_TOKEN = "token";
  /** Value yes */
  public static final String ATTRVALUE_YES = "yes";
  /** Value no */
  public static final String ATTRVALUE_NO = "no";
  /** Value false */
  public static final String ATTRVALUE_FALSE = "false";
  /** Value true */
  public static final String ATTRVALUE_TRUE = "true";
  /** Match attribute */
  public static final String ATTR_MATCH = "match";
  /** Map attribute */
  public static final String ATTR_MAP = "map";

}


