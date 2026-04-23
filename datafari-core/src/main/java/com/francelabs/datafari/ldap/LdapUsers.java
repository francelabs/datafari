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

import com.francelabs.datafari.utils.DatafariThreadFactory;

public class LdapUsers {

  private static final Logger logger = LogManager.getLogger(LdapUsers.class);

  private static LdapUsers instance = null;

  private final Map<String, String> usersDomain;
  private ScheduledExecutorService scheduler;

  public static synchronized LdapUsers getInstance() {
    if (instance == null) {
      instance = new LdapUsers();
    }
    return instance;
  }

  private LdapUsers() {
    usersDomain = new HashMap<>();
    startScheduler();
  }

  private synchronized void startScheduler() {
    if (scheduler != null && !scheduler.isShutdown() && !scheduler.isTerminated()) {
      return;
    }

    scheduler = Executors.newSingleThreadScheduledExecutor(new DatafariThreadFactory("ldap-users-scheduler", logger));

    scheduler.scheduleAtFixedRate(new SweepDomains(), 1, 1, TimeUnit.HOURS);
  }

  public synchronized String getUserDomain(final String username) {
    if (usersDomain.containsKey(username)) {
      return usersDomain.get(username);
    } else {
      final List<LdapRealm> realms = LdapConfig.getActiveDirectoryRealms();
      for (final LdapRealm realm : realms) {
        try {
          final LdapContext context = LdapUtils.getLdapContext(
              realm.getConnectionURL(),
              realm.getConnectionName(),
              realm.getDeobfuscatedConnectionPassword());

          for (final String userBase : realm.getUserBases()) {
            try {
              logger.debug("Testing user " + username + " on base " + userBase);
              if (LdapUtils.checkUser(username, realm.getUserSearchAttribute(), userBase, context)) {
                logger.debug("User " + username + " found in base " + userBase);
                final String domain = realm.getDomainName();
                logger.debug("Found domain " + domain + " for user " + username);
                usersDomain.put(username, domain);
                return domain;
              } else {
                logger.debug("User " + username + " not found in base " + userBase);
              }
            } catch (final NamingException e) {
              logger.error("Error when trying to find user \"" + username + "\" into realm "
                  + realm.getConnectionURL() + " in userBase " + userBase, e);
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

  public synchronized void stopSweep() {
    if (scheduler != null) {
      logger.info("Stopping LdapUsers scheduler");
      scheduler.shutdown();
      try {
        if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
          logger.warn("LdapUsers scheduler did not stop cleanly, forcing shutdown");
          scheduler.shutdownNow();
          if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
            logger.warn("LdapUsers scheduler still not terminated");
          }
        }
      } catch (InterruptedException e) {
        logger.warn("Interrupted while stopping LdapUsers scheduler", e);
        scheduler.shutdownNow();
        Thread.currentThread().interrupt();
      } finally {
        scheduler = null;
      }
    }
  }

  private final class SweepDomains implements Runnable {
    @Override
    public void run() {
      synchronized (LdapUsers.this) {
        usersDomain.clear();
      }
    }
  }
}