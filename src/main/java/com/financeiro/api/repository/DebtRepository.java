package com.financeiro.api.repository;

import com.financeiro.api.entity.Debt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DebtRepository extends JpaRepository<Debt, UUID> {
    Optional<Debt> findByIdAndUserId(UUID id, UUID userId);
    List<Debt> findByDebtorId(UUID debtorId);
    List<Debt> findByTabIdAndDateBetween(UUID tabId, LocalDate start, LocalDate end);
    List<Debt> findByInstallmentGroupIdAndDateGreaterThanEqual(UUID installmentGroupId, LocalDate date);
}
