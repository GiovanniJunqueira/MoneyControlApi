package com.financeiro.api.repository;

import com.financeiro.api.entity.BetCompetition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BetCompetitionRepository extends JpaRepository<BetCompetition, UUID> {
    Optional<BetCompetition> findByCode(String code);
    boolean existsByCode(String code);
}
