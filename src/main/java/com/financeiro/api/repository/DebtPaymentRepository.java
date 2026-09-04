package com.financeiro.api.repository;

import com.financeiro.api.entity.DebtPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DebtPaymentRepository extends JpaRepository<DebtPayment, UUID> {
    List<DebtPayment> findByDebtId(UUID debtId);
}
