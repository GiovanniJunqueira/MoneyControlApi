package com.financeiro.api.repository;

import com.financeiro.api.entity.BetHouseGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BetHouseGroupRepository extends JpaRepository<BetHouseGroup, UUID> {
    List<BetHouseGroup> findByUserIdOrderByCreatedAtAsc(UUID userId);
    Optional<BetHouseGroup> findByIdAndUserId(UUID id, UUID userId);
}
