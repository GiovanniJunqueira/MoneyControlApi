package com.financeiro.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bet_daily_balances", uniqueConstraints = @UniqueConstraint(columnNames = {"house_id", "date"}))
@Getter
@Setter
@NoArgsConstructor
public class BetDailyBalance {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id", nullable = false)
    private BetHouse house;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "month_id", nullable = false)
    private BetMonth month;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance;

    /**
     * Saldo inicial do dia, só quando ajustado manualmente (ex: depósito/saque na casa) - NULL
     * significa "usa o saldo final do dia anterior" (carry-forward automático). Existe pra separar
     * resultado de aposta de movimentação de dinheiro, que não deve contar como lucro/prejuízo.
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal openingBalance;
}
