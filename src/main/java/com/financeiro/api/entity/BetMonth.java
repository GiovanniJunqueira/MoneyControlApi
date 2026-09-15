package com.financeiro.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bet_months")
@Getter
@Setter
@NoArgsConstructor
public class BetMonth {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate startDate;

    /** NULL enquanto o mês está aberto (o atual). Só pode existir um mês aberto por usuário. */
    private LocalDate endDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal initialUnitValue;

    /** Banca total (soma de todas as casas) no instante em que o mês começou - base pro cálculo de lucro/prejuízo. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal startingBanca;

    /**
     * Banca total no instante em que o mês fechou - gravada direto (não inferida do próximo mês),
     * pra continuar correta mesmo se o próximo mês for excluído depois. NULL = mês ainda aberto,
     * ou fechado antes dessa coluna existir (nesse caso o service cai no fallback antigo).
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal endingBanca;

    /** Desempate pra quando dois meses começam no mesmo dia (start_date empatado) - ver BetService. */
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
