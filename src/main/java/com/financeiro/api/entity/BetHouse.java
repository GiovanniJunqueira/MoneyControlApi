package com.financeiro.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bet_houses")
@Getter
@Setter
@NoArgsConstructor
public class BetHouse {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    private String color;

    /** Ordem de exibição, definida pelo usuário arrastando na tela de gerenciar casas. */
    @Column(nullable = false)
    private int position = 0;

    /** Agrupamento opcional (ex: várias sub-contas de uma mesma casa mãe) - null = sem grupo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private BetHouseGroup group;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * A partir dessa data (inclusive), a casa some da lista de casas ativas e do dia-a-dia de
     * QUALQUER mês (passado ou futuro) - sem apagar nada do que já foi registrado antes dela.
     * Null = nunca foi arquivada (comportamento de sempre). "Excluir" uma casa no
     * gerenciamento passou a ser isso (setar pra hoje), não mais um DELETE de verdade.
     */
    private LocalDate archivedFrom;
}
