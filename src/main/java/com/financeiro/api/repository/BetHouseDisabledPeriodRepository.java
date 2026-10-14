package com.financeiro.api.repository;

import com.financeiro.api.entity.BetHouseDisabledPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BetHouseDisabledPeriodRepository extends JpaRepository<BetHouseDisabledPeriod, UUID> {
    List<BetHouseDisabledPeriod> findByHouseIdIn(List<UUID> houseIds);
    Optional<BetHouseDisabledPeriod> findByHouseIdAndToDateIsNull(UUID houseId);
}
