package com.financeiro.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Gasto "no meio do caminho" pelo bot do WhatsApp - já sabemos o valor e a descrição, só falta a
 * pessoa escolher a categoria (respondendo com o número da lista que o bot mandou). Uma linha por
 * telefone - uma nova tentativa de gasto antes de terminar essa substitui a pendente anterior.
 */
@Entity
@Table(name = "whatsapp_pending_expenses")
@Getter
@Setter
@NoArgsConstructor
public class WhatsAppPendingExpense {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String phone;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tab_id", nullable = false)
    private Tab tab;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    private String description;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
