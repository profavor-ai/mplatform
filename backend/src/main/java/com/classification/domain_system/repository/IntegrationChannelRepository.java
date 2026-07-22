package com.classification.domain_system.repository;

import com.classification.domain_system.entity.IntegrationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IntegrationChannelRepository extends JpaRepository<IntegrationChannel, UUID> {
    List<IntegrationChannel> findByIsActiveTrue();
    List<IntegrationChannel> findByIsActiveTrueAndDirection(String direction);
}
