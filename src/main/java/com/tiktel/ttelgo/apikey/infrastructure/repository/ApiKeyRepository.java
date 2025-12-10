package com.tiktel.ttelgo.apikey.infrastructure.repository;

import com.tiktel.ttelgo.apikey.domain.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    
    Optional<ApiKey> findByApiKey(String apiKey);
    
    List<ApiKey> findByUserId(Long userId);
    
    List<ApiKey> findByCustomerEmail(String customerEmail);
    
    List<ApiKey> findByIsActiveTrue();
    
    @Query("SELECT ak FROM ApiKey ak WHERE ak.isActive = true AND (ak.expiresAt IS NULL OR ak.expiresAt > CURRENT_TIMESTAMP)")
    List<ApiKey> findActiveValidKeys();
    
    @Query("SELECT COUNT(ak) FROM ApiKey ak WHERE ak.isActive = true")
    Long countActiveKeys();
    
    @Query("SELECT ak FROM ApiKey ak WHERE ak.customerName LIKE %:search% OR ak.customerEmail LIKE %:search% OR ak.keyName LIKE %:search%")
    List<ApiKey> searchByCustomerOrName(@Param("search") String search);
}

