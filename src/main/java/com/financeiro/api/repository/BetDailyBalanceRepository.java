package com.financeiro.api.repository;

import com.financeiro.api.entity.BetDailyBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BetDailyBalanceRepository extends JpaRepository<BetDailyBalance, UUID> {
    Optional<BetDailyBalance> findByHouseIdAndDate(UUID houseId, LocalDate date);

    /** O saldo mais recente registrado pra essa casa, sem importar o mes - usado pra "banca atual" e continuidade entre meses. */
    Optional<BetDailyBalance> findTopByHouseIdOrderByDateDesc(UUID houseId);

    /** Todas as entradas das casas informadas num intervalo de datas - usado pra montar a lista dia a dia de um mês. */
    List<BetDailyBalance> findByHouseIdInAndDateBetweenOrderByDateAsc(List<UUID> houseIds, LocalDate start, LocalDate end);
}
