package com.financeiro.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Intervalo em que uma casa está desabilitada: some do dia-a-dia e do resumo do mês de
 * fromDate (inclusive) até toDate (exclusive, null = ainda desabilitada). Sem apagar nenhum saldo
 * já registrado - ver BetService.computeMonthDays().
 */
@Entity
@Table(name = "bet_house_disabled_periods")
@Getter
@Setter
@NoArgsConstructor
public class BetHouseDisabledPeriod {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id", nullable = false)
    private BetHouse house;

    @Column(nullable = false)
    private LocalDate fromDate;

    private LocalDate toDate;

    public boolean covers(LocalDate date) {
        return !date.isBefore(fromDate) && (toDate == null || date.isBefore(toDate));
    }
}
