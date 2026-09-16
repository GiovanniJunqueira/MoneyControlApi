package com.financeiro.api.repository;

import com.financeiro.api.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    Optional<Expense> findByIdAndUserId(UUID id, UUID userId);

    List<Expense> findByTabIdAndDateBetweenOrderByDateDesc(UUID tabId, LocalDate start, LocalDate end);

    List<Expense> findByRecurringGroupIdAndDateGreaterThanEqual(UUID recurringGroupId, LocalDate date);

    long countByCategoryId(UUID categoryId);
}
