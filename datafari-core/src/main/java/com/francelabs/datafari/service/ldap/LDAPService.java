package com.francelabs.datafari.service.ldap;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;

public class LDAPService {

	private static LDAPService instance;

	public static synchronized LDAPService getInstance() {
		if (instance == null) {
			instance = new LDAPService();
		}
		return instance;
	}

	// LDAPService.getInstance().testLDAPConnection("ldap://52.16.74.128:389",
	// "admin@corp.francelabs.com", "Jailesdroits");
	public void testLDAPConnection(final String connectionString, final String userPrincipal, final String password) throws NamingException {

		final Hashtable<String, String> env = new Hashtable<>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put("com.sun.jndi.ldap.connect.timeout", "1000");
		env.put(Context.PROVIDER_URL, connectionString);
		env.put(Context.SECURITY_PRINCIPAL, userPrincipal);
		env.put(Context.SECURITY_CREDENTIALS, password);

		final InitialLdapContext session = new InitialLdapContext(env, null);
		session.close();
	}

}
