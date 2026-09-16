package com.financeiro.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bet_competitions")
@Getter
@Setter
@NoArgsConstructor
public class BetCompetition {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false)
    private String name;

    /** Código curto que outras pessoas usam pra entrar - único no sistema todo. */
    @Column(nullable = false, unique = true)
    private String code;

    /** Mês/ano cujo resultado essa competição compara - cada membro é comparado pelo próprio BetMonth dele nesse período. */
    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
