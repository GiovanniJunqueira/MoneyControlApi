package com.financeiro.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "categories", uniqueConstraints = @UniqueConstraint(columnNames = {"tab_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tab_id", nullable = false)
    private Tab tab;

    @Column(nullable = false)
    private String name;

    private String color;

    private String icon;

    /** Limite de gasto mensal opcional - null = sem orçamento definido pra essa categoria. */
    @Column(name = "monthly_budget", precision = 12, scale = 2)
    private BigDecimal monthlyBudget;
}
