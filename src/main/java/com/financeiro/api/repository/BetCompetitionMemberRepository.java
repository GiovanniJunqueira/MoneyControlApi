package com.financeiro.api.repository;

import com.financeiro.api.entity.BetCompetitionMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BetCompetitionMemberRepository extends JpaRepository<BetCompetitionMember, UUID> {
    List<BetCompetitionMember> findByCompetitionId(UUID competitionId);
    List<BetCompetitionMember> findByUserIdOrderByJoinedAtDesc(UUID userId);
    Optional<BetCompetitionMember> findByCompetitionIdAndUserId(UUID competitionId, UUID userId);
    boolean existsByCompetitionIdAndUserId(UUID competitionId, UUID userId);
}
