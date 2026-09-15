package com.financeiro.api.repository;

import com.financeiro.api.entity.ModuleSettings;
import com.financeiro.api.entity.ModuleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ModuleSettingsRepository extends JpaRepository<ModuleSettings, UUID> {
    Optional<ModuleSettings> findByTabIdAndModule(UUID tabId, ModuleType module);
}
