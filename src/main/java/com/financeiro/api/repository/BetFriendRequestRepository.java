package com.financeiro.api.repository;

import com.financeiro.api.entity.BetFriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BetFriendRequestRepository extends JpaRepository<BetFriendRequest, UUID> {
    List<BetFriendRequest> findByReceiverIdOrderByCreatedAtDesc(UUID receiverId);
    Optional<BetFriendRequest> findBySenderIdAndReceiverId(UUID senderId, UUID receiverId);
    boolean existsBySenderIdAndReceiverId(UUID senderId, UUID receiverId);
    Optional<BetFriendRequest> findByIdAndReceiverId(UUID id, UUID receiverId);
}
