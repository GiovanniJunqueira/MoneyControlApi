package com.financeiro.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Amizade mútua já confirmada dos dois lados. {@code userOne}/{@code userTwo} são armazenados
 * sempre com o menor UUID em userOne (ver BetFriendService.sortPair) - assim a mesma dupla nunca
 * gera duas linhas diferentes dependendo de quem consultou primeiro.
 */
@Entity
@Table(name = "bet_friendships", uniqueConstraints = @UniqueConstraint(columnNames = {"user_one_id", "user_two_id"}))
@Getter
@Setter
@NoArgsConstructor
public class BetFriendship {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_one_id", nullable = false)
    private User userOne;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_two_id", nullable = false)
    private User userTwo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
