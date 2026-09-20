package com.financeiro.api.repository;

import com.financeiro.api.entity.BetFriendship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** userOneId é sempre o menor UUID do par - ver BetFriendService.sortPair. */
public interface BetFriendshipRepository extends JpaRepository<BetFriendship, UUID> {
    boolean existsByUserOneIdAndUserTwoId(UUID userOneId, UUID userTwoId);
    Optional<BetFriendship> findByUserOneIdAndUserTwoId(UUID userOneId, UUID userTwoId);
    List<BetFriendship> findByUserOneIdOrUserTwoId(UUID userOneId, UUID userTwoId);
}
