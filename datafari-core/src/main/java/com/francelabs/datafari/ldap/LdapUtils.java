package com.francelabs.datafari.ldap;

import java.io.IOException;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LdapUtils {

  public static final String baseFilter = "(&(objectCategory=Person)(objectClass=User))";
  private static final String[] returnAttributes = { "sAMAccountName", "givenName", "userAccountControl" };
  private static final String departmentAttribute = "department";
  private static final String[] returnDepartmentAttributes = { "sAMAccountName", "givenName", "userAccountControl", departmentAttribute };
  // For paged results
  private static final int pageSize = 1000;

  private static final Logger logger = LogManager.getLogger(LdapUtils.class);

  /**
   * Gets the AD LDAP context of the provided url
   *
   * @param url      the url ("ldap://serverName:serverPort")
   * @param user     the user
   * @param password the user's password
   * @return the LDAP context
   * @throws IOException
   * @throws NamingException .
   */
  public static LdapContext getLdapContext(final String url, final String user, final String password) throws NamingException, IOException {
    // create an initial directory context
    final Hashtable<String, Object> env = new Hashtable<>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put("com.sun.jndi.ldap.connect.timeout", "1000");
    env.put(Context.PROVIDER_URL, url);
    env.put(Context.SECURITY_PRINCIPAL, user);
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
    env.put(Context.SECURITY_CREDENTIALS, password);

    // Create the initial directory context
    final LdapContext ldapCtx = new InitialLdapContext(env, null);
    ldapCtx.setRequestControls(new Control[] { new PagedResultsControl(pageSize, Control.NONCRITICAL) });
    return ldapCtx;
  }

  public static boolean testUserBase(final LdapContext ldapContext, final String searchBase, final String userFilter) {
    final int scope = SearchControls.SUBTREE_SCOPE;
    // initializing search controls
    final SearchControls searchCtls = new SearchControls();
    searchCtls.setSearchScope(scope);
    searchCtls.setReturningAttributes(returnAttributes);

    try {
      logger.debug("Testing user base " + searchBase + ". Trying to find users with filter " + userFilter);
      final NamingEnumeration<SearchResult> users = ldapContext.search(searchBase, userFilter, searchCtls);

      if (users.hasMore()) {
        logger.debug("Found users in user base " + searchBase);
        return true;
      }

      // This part was checking if there are active users in the found ones but depending on the AD type it does not work...
//      while (users.hasMore()) {
//        logger.debug("Found users in user base " + searchBase + "\r\nNow checking if there is at least one active user (userAccountControl attribute check)");
//        final Attributes attrs = users.next().getAttributes();
//
//        // Check if the user is activated before considering we found a valid one
//        final Attribute bitsAttribute = attrs.get("userAccountControl");
//        if (bitsAttribute != null) {
//          final long lng = Long.parseLong(bitsAttribute.get(0).toString());
//          final long secondBit = lng & 2; // get bit 2
//          if (secondBit == 0) { // User activated so return true as the userBase is valid
//            logger.debug("Found active users in user base " + searchBase);
//            return true;
//          }
//        } else {
//          logger.debug("Found a user without userAccountControl attribute in user base " + searchBase);
//        }
//      }
    } catch (final NamingException e) {
      logger.error("Unable to search in user base " + searchBase, e);
      return false;
    }
    logger.debug("No user found in user base " + searchBase);
    return false;
  }

  /**
   * Check in the Active directory if the given username exists
   *
   * @param username   a {@link java.lang.String} object - username value
   * @param searchBase a {@link java.lang.String} object - search base value for scope tree for eg. DC=myjeeva,DC=com
   * @return true if the username exists, false otherwise
   * @throws NamingException
   */
  public static boolean checkUser(final String username, final String searchBase, final DirContext dirContext) throws NamingException {

    final String filter = getFilter(username);

    // initializing search controls
    final SearchControls searchCtls = new SearchControls();
    searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    searchCtls.setReturningAttributes(returnAttributes);
    try {
      return dirContext.search(searchBase, filter, searchCtls).hasMore();
    } catch (final PartialResultException e) {
      // Means that the requested user does not exists
      return false;
    }
  }

  private static String getFilter(final String username) {
    final String filter = "sAMAccountName=" + username;
    return filter;
  }

  public static String getUserDepartment(final String username, final String searchBase, final DirContext dirContext) {
    final String filter = getFilter(username);

    // initializing search controls
    final SearchControls searchCtls = new SearchControls();
    searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    searchCtls.setReturningAttributes(returnDepartmentAttributes);

    try {
      final NamingEnumeration<SearchResult> user = dirContext.search(searchBase, filter, searchCtls);
      if (user.hasMore()) {
        final Attributes attrs = user.next().getAttributes();
        final Attribute departmentAttr = attrs.get(departmentAttribute);
        if (departmentAttr != null) {
          return (String) departmentAttr.get(0);
        } else {
          return null;
        }
      } else {
        return null;
      }
    } catch (final NamingException e) {
      logger.error("Error getting user department", e);
      return null;
    }
  }

}
