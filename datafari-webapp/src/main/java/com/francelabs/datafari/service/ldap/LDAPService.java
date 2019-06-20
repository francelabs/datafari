/*******************************************************************************
 * Copyright 2019 France Labs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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
