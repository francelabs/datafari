package com.francelabs.datafari.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ActiveDirectoryUtils {

  private static final String baseFilter = "(&((&(objectCategory=Person)(objectClass=User)))";
  private static final String[] returnAttributes = { "sAMAccountName", "givenName", "userAccountControl" };
  private static final String departmentAttribute = "department";
  private static final String[] returnDepartmentAttributes = { "sAMAccountName", "givenName", "userAccountControl", departmentAttribute };
  // For paged results
  private static final int pageSize = 1000;

  private static final Logger logger = LogManager.getLogger(ActiveDirectoryUtils.class);

  /**
   * Gets the AD LDAP context of the provided url
   *
   * @param url
   *          the url ("ldap://serverName:serverPort")
   * @param user
   *          the user
   * @param password
   *          the user's password
   * @return the LDAP context
   * @throws IOException
   * @throws NamingException
   *           .
   */
  public static LdapContext getLdapContext(final String url, final String user, final String password) throws NamingException, IOException {
    // create an initial directory context
    final Hashtable<String, Object> env = new Hashtable<>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put("com.sun.jndi.ldap.connect.timeout", "1000");
    env.put(Context.PROVIDER_URL, url);
    env.put(Context.SECURITY_PRINCIPAL, user);
    env.put(Context.SECURITY_CREDENTIALS, password);

    // Create the initial directory context
    final LdapContext ldapCtx = new InitialLdapContext(env, null);
    ldapCtx.setRequestControls(new Control[] { new PagedResultsControl(pageSize, Control.NONCRITICAL) });
    return ldapCtx;
  }

  /**
   * Check in the Active directory if the given username exists
   *
   * @param username
   *          a {@link java.lang.String} object - username value
   * @param searchBase
   *          a {@link java.lang.String} object - search base value for scope
   *          tree for eg. DC=myjeeva,DC=com
   * @return true if the username exists, false otherwise
   * @throws NamingException
   */
  public static boolean checkUser(final String username, final String searchBase, final DirContext dirContext) throws NamingException {

    final String filter = getFilter(username);

    // initializing search controls
    final SearchControls searchCtls = new SearchControls();
    searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    searchCtls.setReturningAttributes(returnAttributes);
    return dirContext.search(searchBase, filter, searchCtls).hasMore();
  }

  private static String getFilter(final String username) {
    String filter = baseFilter;
    filter += "(samaccountname=" + username + "))";
    return filter;
  }

  /**
   * Search for users in the provided AD context and AD userBase
   *
   * @param ctx
   *          AD context
   * @param userBase
   *          AD userBase
   * @param userSubTree
   *          should search for users in subTree ?
   * @return the list of found users
   * @throws NamingException
   * @throws IOException
   */
  public static List<String> listAllusers(final LdapContext ldapContext, final String searchBase, final boolean userSubtree) throws NamingException, IOException {
    final List<String> usersList = new ArrayList<>();
    final String filter = baseFilter + ")";
    int scope = SearchControls.SUBTREE_SCOPE;
    if (!userSubtree) {
      scope = SearchControls.ONELEVEL_SCOPE;
    }

    // initializing search controls
    final SearchControls searchCtls = new SearchControls();
    searchCtls.setSearchScope(scope);
    searchCtls.setReturningAttributes(returnAttributes);

    byte[] cookie = null;

    do {
      final NamingEnumeration<SearchResult> users = ldapContext.search(searchBase, filter, searchCtls);

      while (users.hasMore()) {
        final Attributes attrs = users.next().getAttributes();

        // Check if the user is activated before adding him to the list
        final Attribute bitsAttribute = attrs.get("userAccountControl");
        if (bitsAttribute != null) {
          final long lng = Long.parseLong(bitsAttribute.get(0).toString());
          final long secondBit = lng & 2; // get bit 2
          if (secondBit == 0) { // User activated
            final Attribute accountAttribute = attrs.get("sAMAccountName");
            if (accountAttribute != null) {
              usersList.add((String) accountAttribute.get(0));
            }
          }
        }
      }

      // Examine the paged results control response
      final Control[] controls = ldapContext.getResponseControls();
      if (controls != null) {
        for (int i = 0; i < controls.length; i++) {
          if (controls[i] instanceof PagedResultsResponseControl) {
            final PagedResultsResponseControl prrc = (PagedResultsResponseControl) controls[i];
            cookie = prrc.getCookie();
          }
        }
      }

      // Re-activate paged results
      ldapContext.setRequestControls(new Control[] { new PagedResultsControl(pageSize, cookie, Control.CRITICAL) });

    } while (cookie != null);

    return usersList;
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
