package com.financeiro.api.repository;

import com.financeiro.api.entity.BetMonthStartingBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BetMonthStartingBalanceRepository extends JpaRepository<BetMonthStartingBalance, UUID> {
    List<BetMonthStartingBalance> findByMonthId(UUID monthId);
}
