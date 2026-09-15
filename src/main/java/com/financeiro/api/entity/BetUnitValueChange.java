package com.financeiro.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Registro de mudança do valor da unidade monetária - vale a partir de "date" (inclusive) até a próxima mudança. */
@Entity
@Table(name = "bet_unit_value_changes")
@Getter
@Setter
@NoArgsConstructor
public class BetUnitValueChange {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "month_id", nullable = false)
    private BetMonth month;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
