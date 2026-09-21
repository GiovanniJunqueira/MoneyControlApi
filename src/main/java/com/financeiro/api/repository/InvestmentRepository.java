package com.financeiro.api.repository;

import com.financeiro.api.entity.Investment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvestmentRepository extends JpaRepository<Investment, UUID> {
    List<Investment> findByUserIdOrderByCreatedAtAsc(UUID userId);
    Optional<Investment> findByIdAndUserId(UUID id, UUID userId);
}
