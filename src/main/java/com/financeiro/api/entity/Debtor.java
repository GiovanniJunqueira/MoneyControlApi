package com.financeiro.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "debtors")
@Getter
@Setter
@NoArgsConstructor
public class Debtor {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    private String notes;

    @OneToMany(mappedBy = "debtor", fetch = FetchType.LAZY)
    private List<Debt> debts = new ArrayList<>();
}
