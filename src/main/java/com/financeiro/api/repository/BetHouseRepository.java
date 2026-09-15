package com.financeiro.api.repository;

import com.financeiro.api.entity.BetHouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BetHouseRepository extends JpaRepository<BetHouse, UUID> {
    List<BetHouse> findByUserIdOrderByNameAsc(UUID userId);
    Optional<BetHouse> findByIdAndUserId(UUID id, UUID userId);
}
