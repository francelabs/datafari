package com.francelabs.datafari.security.standard.token.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduled task responsible for periodically removing expired access tokens
 * from the Datafari token store.
 *
 * <p>The legacy token mechanism used by Datafari persists issued access tokens
 * so they can later be resolved back into authenticated users when a bearer
 * token is presented on protected endpoints. Since expired tokens are no longer
 * valid but may still remain in the persistence layer, this task performs
 * regular housekeeping by deleting them.</p>
 *
 * <p>This cleanup is intentionally handled outside of the request flow so that
 * token validation and token issuance remain simple and fast, while expired
 * records are removed asynchronously during normal application life.</p>
 *
 * <p>The cleanup job is scheduled with a fixed delay of one hour between the
 * end of one execution and the start of the next one.</p>
 *
 * <p>This class also enables Spring scheduling support through
 * {@link org.springframework.scheduling.annotation.EnableScheduling}.</p>
 */
@Configuration
@EnableScheduling
public class DatafariExpiredTokenCleanupTask {
  private static final Logger LOGGER = LogManager.getLogger(DatafariExpiredTokenCleanupTask.class);
  private final DatafariTokenService tokenService;
  /**
   * Fixed delay between two cleanup executions: one hour.
   */
  private static final long CLEANUP_DELAY_MS = 3_600_000L;

  public DatafariExpiredTokenCleanupTask(DatafariTokenService tokenService) {
    this.tokenService = tokenService;
    LOGGER.debug("DatafariExpiredTokenCleanupTask created");
  }

  /**
   * Deletes all expired access tokens from the persistence store.
   *
   * <p>This method is executed automatically by Spring according to the
   * configured schedule and does not need to be invoked manually.</p>
   */
  @Scheduled(fixedDelay = CLEANUP_DELAY_MS)
  public void cleanupExpiredTokens() {
    tokenService.deleteExpiredTokens();
  }

}
