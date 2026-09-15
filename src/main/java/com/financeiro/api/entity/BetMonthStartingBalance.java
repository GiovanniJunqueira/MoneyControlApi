package com.financeiro.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Saldo de uma casa no instante exato em que um mês começou - a quebra por casa da "startingBanca" do mês. */
@Entity
@Table(name = "bet_month_starting_balances", uniqueConstraints = @UniqueConstraint(columnNames = {"month_id", "house_id"}))
@Getter
@Setter
@NoArgsConstructor
public class BetMonthStartingBalance {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "month_id", nullable = false)
    private BetMonth month;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id", nullable = false)
    private BetHouse house;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance;
}
