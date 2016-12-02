package com.francelabs.datafari.realm;

import java.util.Hashtable;

import javax.naming.Context;

import org.apache.catalina.realm.JNDIRealm;
import org.apache.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.system.ManifoldCF;

public class DatafariJNDIRealm extends JNDIRealm {

	private static final Logger log = Logger.getLogger(DatafariJNDIRealm.class);

	@Override
	protected Hashtable<String, String> getDirectoryContextEnvironment() {
		final Hashtable<String, String> env = new Hashtable<>();
		// Configure our directory context environment.
		// Here we decrypt password and set it
		try {
			this.connectionPassword = ManifoldCF.deobfuscate(this.connectionPassword);
		} catch (final ManifoldCFException e) {
			log.error("Deobfuscate error on password ! Did you use clear text password ?", e);
		}

		env.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
		if (connectionName != null) {
			env.put(Context.SECURITY_PRINCIPAL, connectionName);
		}
		if (connectionPassword != null) {
			env.put(Context.SECURITY_CREDENTIALS, connectionPassword);
		}
		if (connectionURL != null && connectionAttempt == 0) {
			env.put(Context.PROVIDER_URL, connectionURL);
		} else if (alternateURL != null && connectionAttempt > 0) {
			env.put(Context.PROVIDER_URL, alternateURL);
		}
		if (authentication != null) {
			env.put(Context.SECURITY_AUTHENTICATION, authentication);
		}
		if (protocol != null) {
			env.put(Context.SECURITY_PROTOCOL, protocol);
		}
		if (referrals != null) {
			env.put(Context.REFERRAL, referrals);
		}
		if (derefAliases != null) {
			env.put(JNDIRealm.DEREF_ALIASES, derefAliases);
		}
		if (connectionTimeout != null) {
			env.put("com.sun.jndi.ldap.connect.timeout", connectionTimeout);
		}
		return env;
	}

}
