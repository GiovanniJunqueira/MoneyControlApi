package com.financeiro.api.repository;

import com.financeiro.api.entity.Debtor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DebtorRepository extends JpaRepository<Debtor, UUID> {
    List<Debtor> findByUserIdOrderByNameAsc(UUID userId);
    Optional<Debtor> findByIdAndUserId(UUID id, UUID userId);
}
