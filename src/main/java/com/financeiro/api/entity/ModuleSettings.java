package com.financeiro.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "module_settings", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "module"}))
@Getter
@Setter
@NoArgsConstructor
public class ModuleSettings {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModuleType module;

    @Column(nullable = false)
    private Integer closingDay = 1;

    public ModuleSettings(User user, ModuleType module, Integer closingDay) {
        this.user = user;
        this.module = module;
        this.closingDay = closingDay;
    }
}
