package com.financeiro.api.repository;

import com.financeiro.api.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByTabIdOrderByNameAsc(UUID tabId);
    Optional<Category> findByIdAndUserId(UUID id, UUID userId);
    Optional<Category> findByIdAndTabId(UUID id, UUID tabId);
    Optional<Category> findByTabIdAndName(UUID tabId, String name);
}
