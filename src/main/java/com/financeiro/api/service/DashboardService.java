package com.financeiro.api.service;

import com.financeiro.api.dto.category.CategoryResponse;
import com.financeiro.api.dto.dashboard.*;
import com.financeiro.api.dto.debt.DebtResponse;
import com.financeiro.api.dto.debtor.DebtorSummaryResponse;
import com.financeiro.api.dto.expense.ExpenseResponse;
import com.financeiro.api.entity.Debt;
import com.financeiro.api.entity.Expense;
import com.financeiro.api.entity.ModuleType;
import com.financeiro.api.entity.Tab;
import com.financeiro.api.exception.AppException;
import com.financeiro.api.repository.DebtRepository;
import com.financeiro.api.repository.ExpenseRepository;
import com.financeiro.api.repository.TabRepository;
import com.financeiro.api.security.CurrentUser;
import com.financeiro.api.util.FiscalPeriod;
import com.financeiro.api.util.FiscalPeriodCalculator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class DashboardService {

    private final ExpenseRepository expenseRepository;
    private final DebtRepository debtRepository;
    private final TabRepository tabRepository;
    private final ModuleSettingsService moduleSettingsService;
    private final DebtorService debtorService;

    public DashboardService(ExpenseRepository expenseRepository, DebtRepository debtRepository,
                             TabRepository tabRepository, ModuleSettingsService moduleSettingsService,
                             DebtorService debtorService) {
        this.expenseRepository = expenseRepository;
        this.debtRepository = debtRepository;
        this.tabRepository = tabRepository;
        this.moduleSettingsService = moduleSettingsService;
        this.debtorService = debtorService;
    }

    /** Devedores de todas as abas do usuario, sem merge por nome (cada linha diz de qual aba veio) - usado na tela inicial. */
    @Transactional(readOnly = true)
    public List<DevedorGeralResponse> devedoresGeral() {
        List<Tab> tabs = tabRepository.findByUserIdOrderByNameAsc(CurrentUser.id());
        List<DevedorGeralResponse> result = new ArrayList<>();
        for (Tab tab : tabs) {
            for (DebtorSummaryResponse d : debtorService.list(tab.getId())) {
                if (d.totalDevido().compareTo(BigDecimal.ZERO) > 0) {
                    result.add(new DevedorGeralResponse(
                            d.id(), d.name(), d.totalDevido(), d.quantidadeDividas(),
                            tab.getId(), tab.getName(), tab.getColor()
                    ));
                }
            }
        }
        result.sort((a, b) -> b.totalDevido().compareTo(a.totalDevido()));
        return result;
    }

    @Transactional(readOnly = true)
    public GastosDashboardResponse gastos(UUID tabId, String periodParam) {
        return gastosParaAba(findOwnedTab(tabId), periodParam);
    }

    @Transactional(readOnly = true)
    public DevedoresDashboardResponse devedores(UUID tabId, String periodParam) {
        return devedoresParaAba(findOwnedTab(tabId), periodParam);
    }

    @Transactional(readOnly = true)
    public VisaoGeralResponse visaoGeral(String periodParam) {
        // Fixa a mesma "chave" de período pra todas as abas (cada uma resolve sua própria janela de
        // datas a partir dela, com seu closingDay) - sem isso, abas com closingDay diferente podiam
        // cair num período "atual" diferente uma da outra num dia de transição. Sem period explícito,
        // usa a mesma lógica de rollover de fechamento (não o mês de calendário puro) com um
        // closingDay de referência (1, o default de toda aba nova) pra decidir a chave "atual".
        String key = (periodParam != null && !periodParam.isBlank())
                ? periodParam
                : FiscalPeriodCalculator.getCurrentFiscalPeriod(1).key();

        List<Tab> tabs = tabRepository.findByUserIdOrderByNameAsc(CurrentUser.id());

        BigDecimal totalGastoGeral = BigDecimal.ZERO;
        long qtdLancamentosGeral = 0;
        BigDecimal totalEmprestadoGeral = BigDecimal.ZERO;
        BigDecimal totalRecebidoGeral = BigDecimal.ZERO;
        long qtdPessoasGeral = 0;

        Map<String, CategoriaAcc> categoriaPorNome = new LinkedHashMap<>();
        Map<String, PessoaAcc> pessoaPorNome = new LinkedHashMap<>();
        List<TabSummary> abas = new ArrayList<>();

        for (Tab tab : tabs) {
            GastosDashboardResponse g = gastosParaAba(tab, key);
            DevedoresDashboardResponse d = devedoresParaAba(tab, key);

            totalGastoGeral = totalGastoGeral.add(g.resumo().total());
            qtdLancamentosGeral += g.resumo().quantidadeLancamentos();
            totalEmprestadoGeral = totalEmprestadoGeral.add(d.resumo().totalEmprestado());
            totalRecebidoGeral = totalRecebidoGeral.add(d.resumo().totalRecebido());
            qtdPessoasGeral += d.resumo().quantidadePessoas();

            for (CategoriaResumo c : g.porCategoria()) {
                CategoriaAcc acc = categoriaPorNome.computeIfAbsent(c.nome(), k -> new CategoriaAcc(c.categoryId(), c.nome(), c.cor()));
                acc.total = acc.total.add(c.total());
                acc.quantidade += c.quantidade();
            }
            for (PessoaResumo p : d.porPessoa()) {
                PessoaAcc acc = pessoaPorNome.computeIfAbsent(p.nome(), k -> new PessoaAcc(p.debtorId(), p.nome()));
                acc.totalDevido = acc.totalDevido.add(p.totalDevido());
                acc.totalPago = acc.totalPago.add(p.totalPago());
                acc.dividas.addAll(p.dividas());
            }

            abas.add(new TabSummary(tab.getId(), tab.getName(), tab.getColor(), g.resumo().total(), d.resumo().totalPendente()));
        }

        BigDecimal totalPendenteGeral = totalEmprestadoGeral.subtract(totalRecebidoGeral);
        BigDecimal mediaGeral = qtdLancamentosGeral == 0 ? BigDecimal.ZERO
                : totalGastoGeral.divide(BigDecimal.valueOf(qtdLancamentosGeral), 2, RoundingMode.HALF_UP);

        List<CategoriaResumo> porCategoria = new ArrayList<>();
        for (CategoriaAcc acc : categoriaPorNome.values()) {
            double percentual = totalGastoGeral.compareTo(BigDecimal.ZERO) > 0
                    ? acc.total.divide(totalGastoGeral, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            porCategoria.add(new CategoriaResumo(acc.categoryId, acc.nome, acc.cor, acc.total, acc.quantidade, percentual));
        }
        porCategoria.sort((a, b) -> b.total().compareTo(a.total()));

        List<PessoaResumo> porPessoa = new ArrayList<>();
        for (PessoaAcc acc : pessoaPorNome.values()) {
            porPessoa.add(new PessoaResumo(acc.debtorId, acc.nome, acc.totalDevido, acc.totalPago, acc.dividas));
        }
        porPessoa.sort((a, b) -> b.totalDevido().compareTo(a.totalDevido()));

        return new VisaoGeralResponse(
                key,
                abas,
                new GastosResumo(totalGastoGeral, qtdLancamentosGeral, mediaGeral),
                porCategoria,
                new DevedoresResumo(totalEmprestadoGeral, totalRecebidoGeral, totalPendenteGeral, qtdPessoasGeral),
                porPessoa
        );
    }

    private GastosDashboardResponse gastosParaAba(Tab tab, String periodParam) {
        int closingDay = moduleSettingsService.getClosingDay(tab.getId(), ModuleType.GASTOS);
        FiscalPeriod period = ExpenseService.resolvePeriod(periodParam, closingDay);

        List<Expense> expenses = expenseRepository
                .findByTabIdAndDateBetweenOrderByDateDesc(tab.getId(), period.start(), period.end());

        BigDecimal total = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<UUID, CategoriaAcc> porCategoriaMap = new LinkedHashMap<>();
        for (Expense e : expenses) {
            UUID catId = e.getCategory().getId();
            CategoriaAcc acc = porCategoriaMap.computeIfAbsent(catId, k -> new CategoriaAcc(catId, e.getCategory().getName(), e.getCategory().getColor()));
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

    private DevedoresDashboardResponse devedoresParaAba(Tab tab, String periodParam) {
        int closingDay = moduleSettingsService.getClosingDay(tab.getId(), ModuleType.DEVEDORES);
        FiscalPeriod period = ExpenseService.resolvePeriod(periodParam, closingDay);

        List<Debt> debts = debtRepository.findByTabIdAndDateBetween(tab.getId(), period.start(), period.end());

        BigDecimal totalEmprestado = debts.stream().map(Debt::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRecebido = debts.stream().map(Debt::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPendente = totalEmprestado.subtract(totalRecebido);

        Map<UUID, PessoaAcc> porPessoaMap = new LinkedHashMap<>();
        for (Debt d : debts) {
            UUID debtorId = d.getDebtor().getId();
            PessoaAcc acc = porPessoaMap.computeIfAbsent(debtorId, k -> new PessoaAcc(debtorId, d.getDebtor().getName()));
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

    private Tab findOwnedTab(UUID tabId) {
        return tabRepository.findByIdAndUserId(tabId, CurrentUser.id())
                .orElseThrow(() -> new AppException("Aba não encontrada.", HttpStatus.NOT_FOUND));
    }

    private static class CategoriaAcc {
        UUID categoryId;
        String nome;
        String cor;
        BigDecimal total = BigDecimal.ZERO;
        long quantidade = 0;

        CategoriaAcc(UUID categoryId, String nome, String cor) {
            this.categoryId = categoryId;
            this.nome = nome;
            this.cor = cor;
        }
    }

    private static class PessoaAcc {
        UUID debtorId;
        String nome;
        BigDecimal totalDevido = BigDecimal.ZERO;
        BigDecimal totalPago = BigDecimal.ZERO;
        List<DebtResponse> dividas = new ArrayList<>();

        PessoaAcc(UUID debtorId, String nome) {
            this.debtorId = debtorId;
            this.nome = nome;
        }
    }
}
