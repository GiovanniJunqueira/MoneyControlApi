package com.financeiro.api.repository;

import com.financeiro.api.entity.BetMonth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BetMonthRepository extends JpaRepository<BetMonth, UUID> {
    Optional<BetMonth> findByUserIdAndEndDateIsNull(UUID userId);
    List<BetMonth> findByUserIdOrderByStartDateDesc(UUID userId);
}
