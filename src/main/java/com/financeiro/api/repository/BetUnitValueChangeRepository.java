package com.financeiro.api.repository;

import com.financeiro.api.entity.BetUnitValueChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface BetUnitValueChangeRepository extends JpaRepository<BetUnitValueChange, UUID> {
    /**
     * A mudança mais recente com date <= referência - é o valor vigente naquele dia.
     * Duas mudanças no mesmo dia empatam em "date", por isso o desempate por createdAt:
     * a mais recente delas (a última que a pessoa registrou naquele dia) vence.
     */
    Optional<BetUnitValueChange> findTopByMonthIdAndDateLessThanEqualOrderByDateDescCreatedAtDesc(UUID monthId, LocalDate date);
}
