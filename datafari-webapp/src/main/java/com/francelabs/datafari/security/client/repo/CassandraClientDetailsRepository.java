package com.francelabs.datafari.security.client.repo;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.francelabs.datafari.security.client.model.CassandraClientDetails;

@Repository
public interface CassandraClientDetailsRepository extends CrudRepository<CassandraClientDetails, String> {

  Optional<CassandraClientDetails> findByClientId(String clientId);
}
