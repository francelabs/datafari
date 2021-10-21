package com.francelabs.datafari.ldap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LdapUsers {

  private static LdapUsers instance = null;
  private final Map<String, String> usersDomain;
  private final ScheduledExecutorService scheduler;

  private static final Logger logger = LogManager.getLogger(LdapUsers.class);

  public static synchronized LdapUsers getInstance() {
    if (instance == null) {
      instance = new LdapUsers();
    }
    return instance;
  }

  private LdapUsers() {
    usersDomain = new HashMap<>();
    scheduler = Executors.newScheduledThreadPool(1);
    scheduler.scheduleAtFixedRate(new SweepDomains(), 1, 1, TimeUnit.HOURS);
  }

  public synchronized String getUserDomain(final String username) {
    if (usersDomain.containsKey(username)) {
      return usersDomain.get(username);
    } else {
      final List<LdapRealm> realms = LdapConfig.getActiveDirectoryRealms();
      for (final LdapRealm realm : realms) {
        try {
          final LdapContext context = LdapUtils.getLdapContext(realm.getConnectionURL(), realm.getConnectionName(), realm.getDeobfuscatedConnectionPassword());
          for (final String userBase : realm.getUserBases()) {

            try {
              logger.debug("Testing user " + username + " on base " + userBase);
              if (LdapUtils.checkUser(username, realm.getUserSearchAttribute(), userBase, context)) {
                logger.debug("User " + username + " found in base " + userBase);
                final String domain = extractDomainName(userBase);
                logger.debug("Found domain " + domain + " for user " + username);
                usersDomain.put(username, domain);
                return domain;
              } else {
                logger.debug("User " + username + " not found in base " + userBase);
              }
            } catch (final NamingException e) {
              logger.error("Error when trying to find user \"" + username + "\" into realm " + realm.getConnectionURL() + " in userBase " + userBase, e);
            }

          }
          context.close();
        } catch (final Exception e) {
          logger.error("Search error for user " + username + " into realm " + realm.getConnectionURL(), e);
        }
      }
      usersDomain.put(username, "");
      return null;
    }
  }

  private String extractDomainName(final String userBase) {
    final String[] parts = userBase.toLowerCase().split(",");
    String domain = "";
    for (int i = 0; i < parts.length; i++) {
      if (parts[i].indexOf("dc=") != -1) { // Check if the current
        // part is a domain
        // component
        if (!domain.isEmpty()) {
          domain += ".";
        }
        domain += parts[i].substring(parts[i].indexOf('=') + 1);
      }
    }
    return domain;
  }

  public void stopSweep() {

    scheduler.shutdownNow();

  }

  private final class SweepDomains implements Runnable {

    @Override
    public void run() {

      LdapUsers.getInstance().usersDomain.clear();

    }

  }

}
