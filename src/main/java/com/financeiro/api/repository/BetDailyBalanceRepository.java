package com.financeiro.api.repository;

import com.financeiro.api.entity.BetDailyBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface BetDailyBalanceRepository extends JpaRepository<BetDailyBalance, UUID> {
    Optional<BetDailyBalance> findByHouseIdAndDate(UUID houseId, LocalDate date);

    /** O saldo mais recente registrado pra essa casa, sem importar o mes - usado pra "banca atual" e continuidade entre meses. */
    Optional<BetDailyBalance> findTopByHouseIdOrderByDateDesc(UUID houseId);

    /** O saldo mais recente ANTES da data informada - e o "saldo inicio do dia" ao atualizar. */
    Optional<BetDailyBalance> findTopByHouseIdAndDateLessThanOrderByDateDesc(UUID houseId, LocalDate date);
}
