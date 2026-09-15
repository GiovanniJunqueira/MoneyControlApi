package com.financeiro.api.repository;

import com.financeiro.api.entity.Tab;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TabRepository extends JpaRepository<Tab, UUID> {
    List<Tab> findByUserIdOrderByNameAsc(UUID userId);
    Optional<Tab> findByIdAndUserId(UUID id, UUID userId);
    Optional<Tab> findByUserIdAndName(UUID userId, String name);
}
