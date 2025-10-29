package com.francelabs.datafari.aggregator.utils;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.Random;

public class SearchAggregatorPasswordManager {

  private static final String CLIENT_CREDENTIALS = "client_credentials";
  private static final String SCOPE_READ = "read";

//  @Autowired
//  PgsqlClientDetailsRepository clientDetailsRepo;

  @Autowired
  PasswordEncoder passwordEncoder;

  public String renewPassword() {
    throw new NotImplementedException("If this method is still useful, it needs to be completely revised in relation to Spring 6. You probably need to implement an Authorization Server with Spring Authorization Server library");

/*    final Optional<PgsqlClientDetails> oClientDetails = clientDetailsRepo.findByClientId("search-aggregator");
    if (oClientDetails.isPresent()) {
      final PgsqlClientDetails existingSAClient = oClientDetails.get();
      clientDetailsRepo.delete(existingSAClient);
    }
    final Set<String> searchAggScopes = new HashSet<String>(Arrays.asList(SCOPE_READ));
    final Set<String> searchAggGrant = new HashSet<String>(Arrays.asList(CLIENT_CREDENTIALS));
    final String randomPassword = generateRandomPassword();
    final String encodedPassword = passwordEncoder.encode(randomPassword);
    final PgsqlClientDetails searchAggClient = new PgsqlClientDetails("search-aggregator", encodedPassword, new HashSet<String>(), searchAggScopes, searchAggGrant, new HashSet<String>(),
        new HashSet<String>(), 60 * 15, 0);
    clientDetailsRepo.save(searchAggClient);
    return randomPassword;*/
  }

  private String generateRandomPassword() {

    final char[] allAllowed = "abcdefghijklmnopqrstuvwxyzABCDEFGJKLMNPRSTUVWXYZ0123456789-+_&@=.!".toCharArray();

    // Use cryptographically secure random number generator
    final Random random = new SecureRandom();

    final int length = allAllowed.length + random.nextInt(5);

    final StringBuilder password = new StringBuilder();
    for (int i = 0; i < length; i++) {
      password.append(allAllowed[random.nextInt(allAllowed.length)]);
    }

    return password.toString();
  }

}
