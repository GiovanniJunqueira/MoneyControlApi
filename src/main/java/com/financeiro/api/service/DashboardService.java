package com.financeiro.api.service;

import com.financeiro.api.dto.category.CategoryResponse;
import com.financeiro.api.dto.dashboard.*;
import com.financeiro.api.dto.debt.DebtResponse;
import com.financeiro.api.dto.expense.ExpenseResponse;
import com.financeiro.api.entity.Debt;
import com.financeiro.api.entity.Expense;
import com.financeiro.api.entity.ModuleType;
import com.financeiro.api.repository.DebtRepository;
import com.financeiro.api.repository.ExpenseRepository;
import com.financeiro.api.security.CurrentUser;
import com.financeiro.api.util.FiscalPeriod;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class DashboardService {

    private final ExpenseRepository expenseRepository;
    private final DebtRepository debtRepository;
    private final ModuleSettingsService moduleSettingsService;

    public DashboardService(ExpenseRepository expenseRepository, DebtRepository debtRepository,
                             ModuleSettingsService moduleSettingsService) {
        this.expenseRepository = expenseRepository;
        this.debtRepository = debtRepository;
        this.moduleSettingsService = moduleSettingsService;
    }

    public GastosDashboardResponse gastos(String periodParam) {
        int closingDay = moduleSettingsService.getClosingDay(ModuleType.GASTOS);
        FiscalPeriod period = ExpenseService.resolvePeriod(periodParam, closingDay);

        List<Expense> expenses = expenseRepository
                .findByUserIdAndDateBetweenOrderByDateDesc(CurrentUser.id(), period.start(), period.end());

        BigDecimal total = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<UUID, CategoriaAcc> porCategoriaMap = new LinkedHashMap<>();
        for (Expense e : expenses) {
            UUID catId = e.getCategory().getId();
            CategoriaAcc acc = porCategoriaMap.computeIfAbsent(catId, k -> new CategoriaAcc(e.getCategory().getName(), e.getCategory().getColor()));
            acc.total = acc.total.add(e.getAmount());
            acc.quantidade += 1;
        }

        List<CategoriaResumo> porCategoria = new ArrayList<>();
        for (var entry : porCategoriaMap.entrySet()) {
            CategoriaAcc acc = entry.getValue();
            double percentual = total.compareTo(BigDecimal.ZERO) > 0
                    ? acc.total.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            porCategoria.add(new CategoriaResumo(entry.getKey(), acc.nome, acc.cor, acc.total, acc.quantidade, percentual));
        }
        porCategoria.sort((a, b) -> b.total().compareTo(a.total()));

        BigDecimal media = expenses.isEmpty() ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP);

        GastosResumo resumo = new GastosResumo(total, expenses.size(), media);

        List<ExpenseResponse> lancamentos = expenses.stream().map(e -> new ExpenseResponse(
                e.getId(), e.getAmount(), e.getDescription(), e.getDate(),
                new CategoryResponse(e.getCategory().getId(), e.getCategory().getName(), e.getCategory().getColor(), e.getCategory().getIcon())
        )).toList();

        return new GastosDashboardResponse(period, resumo, porCategoria, lancamentos);
    }

    public DevedoresDashboardResponse devedores(String periodParam) {
        int closingDay = moduleSettingsService.getClosingDay(ModuleType.DEVEDORES);
        FiscalPeriod period = ExpenseService.resolvePeriod(periodParam, closingDay);

        List<Debt> debts = debtRepository.findByUserIdAndDateBetween(CurrentUser.id(), period.start(), period.end());

        BigDecimal totalEmprestado = debts.stream().map(Debt::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRecebido = debts.stream().map(Debt::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPendente = totalEmprestado.subtract(totalRecebido);

        Map<UUID, PessoaAcc> porPessoaMap = new LinkedHashMap<>();
        for (Debt d : debts) {
            UUID debtorId = d.getDebtor().getId();
            PessoaAcc acc = porPessoaMap.computeIfAbsent(debtorId, k -> new PessoaAcc(d.getDebtor().getName()));
            acc.totalDevido = acc.totalDevido.add(d.getAmount().subtract(d.getPaidAmount()));
            acc.totalPago = acc.totalPago.add(d.getPaidAmount());
            acc.dividas.add(new DebtResponse(d.getId(), d.getAmount(), d.getReason(), d.getDate(), d.getStatus().name().toLowerCase(), d.getPaidAmount()));
        }

        List<PessoaResumo> porPessoa = new ArrayList<>();
        for (var entry : porPessoaMap.entrySet()) {
            PessoaAcc acc = entry.getValue();
            porPessoa.add(new PessoaResumo(entry.getKey(), acc.nome, acc.totalDevido, acc.totalPago, acc.dividas));
        }
        porPessoa.sort((a, b) -> b.totalDevido().compareTo(a.totalDevido()));

        DevedoresResumo resumo = new DevedoresResumo(totalEmprestado, totalRecebido, totalPendente, porPessoaMap.size());

        return new DevedoresDashboardResponse(period, resumo, porPessoa);
    }

    private static class CategoriaAcc {
        String nome;
        String cor;
        BigDecimal total = BigDecimal.ZERO;
        long quantidade = 0;

        CategoriaAcc(String nome, String cor) {
            this.nome = nome;
            this.cor = cor;
        }
    }

    private static class PessoaAcc {
        String nome;
        BigDecimal totalDevido = BigDecimal.ZERO;
        BigDecimal totalPago = BigDecimal.ZERO;
        List<DebtResponse> dividas = new ArrayList<>();

        PessoaAcc(String nome) {
            this.nome = nome;
        }
    }
}
