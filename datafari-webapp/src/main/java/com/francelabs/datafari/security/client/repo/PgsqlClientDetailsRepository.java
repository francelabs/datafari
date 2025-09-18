package com.francelabs.datafari.security.client.repo;

import com.francelabs.datafari.security.client.model.PgsqlClientDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PgsqlClientDetailsRepository extends JpaRepository<PgsqlClientDetails, String> {
    Optional<PgsqlClientDetails> findByClientId(String clientId);
}