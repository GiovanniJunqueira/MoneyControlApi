package com.financeiro.api.repository;

import com.financeiro.api.entity.WhatsAppPendingExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WhatsAppPendingExpenseRepository extends JpaRepository<WhatsAppPendingExpense, UUID> {
    Optional<WhatsAppPendingExpense> findByPhone(String phone);
    void deleteByPhone(String phone);
}
