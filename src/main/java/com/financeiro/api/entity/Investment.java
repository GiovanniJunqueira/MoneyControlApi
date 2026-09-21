package com.financeiro.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "investments")
@Getter
@Setter
@NoArgsConstructor
public class Investment {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    /** Quanto está investido/guardado hoje. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /** Taxa de rendimento mensal em %, ex: 1.25 = 1,25% ao mês. */
    @Column(name = "monthly_rate_percent", nullable = false, precision = 7, scale = 4)
    private BigDecimal monthlyRatePercent;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
