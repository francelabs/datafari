package com.francelabs.datafari.utils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

public class AcitveDirectoryUtils {

	private static final String baseFilter = "(&((&(objectCategory=Person)(objectClass=User)))";
	private static final String[] returnAttributes = { "sAMAccountName", "givenName", "userAccountControl" };

	/**
	 * Gets the AD directory context of the provided url
	 *
	 * @param url
	 *            the url ("ldap://serverName:serverPort")
	 * @param user
	 *            the user
	 * @param password
	 *            the user's password
	 * @return the directory context
	 * @throws NamingException
	 *             .
	 */
	public static DirContext getDirContext(final String url, final String user, final String password) throws NamingException {
		// create an initial directory context
		final Hashtable<String, Object> env = new Hashtable<String, Object>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put("com.sun.jndi.ldap.connect.timeout", "1000");
		env.put(Context.PROVIDER_URL, url);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, user);
		env.put(Context.SECURITY_CREDENTIALS, password);

		// Create the initial directory context
		return new InitialDirContext(env);
	}

	/**
	 * Check in the Active directory if the given username exists
	 *
	 * @param username
	 *            a {@link java.lang.String} object - username value
	 * @param searchBase
	 *            a {@link java.lang.String} object - search base value for
	 *            scope tree for eg. DC=myjeeva,DC=com
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

	private static String getFilter(final String searchValue) {
		String filter = baseFilter;
		filter += "(samaccountname=" + searchValue + "))";
		return filter;
	}

	/**
	 * Search for users in the provided AD context and AD userBase
	 *
	 * @param ctx
	 *            AD context
	 * @param userBase
	 *            AD userBase
	 * @param userSubTree
	 *            should search for users in subTree ?
	 * @return the list of found users
	 * @throws NamingException
	 */
	public static List<String> listAllusers(final DirContext dirContext, final String searchBase, final boolean userSubtree) throws NamingException {
		final List<String> usersList = new ArrayList<String>();
		final String filter = baseFilter + ")";
		int scope = SearchControls.SUBTREE_SCOPE;
		if (!userSubtree) {
			scope = SearchControls.ONELEVEL_SCOPE;
		}

		// initializing search controls
		final SearchControls searchCtls = new SearchControls();
		searchCtls.setSearchScope(scope);
		searchCtls.setReturningAttributes(returnAttributes);

		final NamingEnumeration<SearchResult> users = dirContext.search(searchBase, filter, searchCtls);

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

		return usersList;
	}

}
