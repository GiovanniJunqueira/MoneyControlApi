package com.financeiro.api.repository;

import com.financeiro.api.entity.BetMonth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BetMonthRepository extends JpaRepository<BetMonth, UUID> {
    Optional<BetMonth> findByUserIdAndEndDateIsNull(UUID userId);

    /** Mais recente primeiro; desempata por createdAt quando dois meses começam no mesmo dia. */
    List<BetMonth> findByUserIdOrderByStartDateDescCreatedAtDesc(UUID userId);

    Optional<BetMonth> findByIdAndUserId(UUID id, UUID userId);

    /** startDate é sempre dia 1 do mês escolhido - usado pra achar "o mês de fulano em setembro/2026" numa competição. */
    Optional<BetMonth> findByUserIdAndStartDate(UUID userId, LocalDate startDate);
}
