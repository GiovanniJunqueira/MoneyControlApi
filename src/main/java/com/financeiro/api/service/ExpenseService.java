package com.financeiro.api.service;

import com.financeiro.api.dto.category.CategoryResponse;
import com.financeiro.api.dto.expense.ExpenseListResponse;
import com.financeiro.api.dto.expense.ExpenseRequest;
import com.financeiro.api.dto.expense.ExpenseResponse;
import com.financeiro.api.dto.expense.ExpenseUpdateRequest;
import com.financeiro.api.entity.Category;
import com.financeiro.api.entity.Expense;
import com.financeiro.api.entity.ModuleType;
import com.financeiro.api.entity.Tab;
import com.financeiro.api.entity.User;
import com.financeiro.api.exception.AppException;
import com.financeiro.api.repository.CategoryRepository;
import com.financeiro.api.repository.ExpenseRepository;
import com.financeiro.api.repository.TabRepository;
import com.financeiro.api.repository.UserRepository;
import com.financeiro.api.security.CurrentUser;
import com.financeiro.api.util.FiscalPeriod;
import com.financeiro.api.util.FiscalPeriodCalculator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ExpenseService {

    /** Gasto recorrente "indefinido" não tem data pra acabar - materializa esse tanto de meses de
     * uma vez (não é infinito de verdade, mas cobre bastante tempo à frente sem precisar de um job
     * de background pra ir gerando mais). Se a pessoa precisar de mais no futuro, cadastra de novo. */
    private static final int INDEFINITE_HORIZON_MONTHS = 24;

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TabRepository tabRepository;
    private final ModuleSettingsService moduleSettingsService;

    public ExpenseService(ExpenseRepository expenseRepository, CategoryRepository categoryRepository,
                           UserRepository userRepository, TabRepository tabRepository,
                           ModuleSettingsService moduleSettingsService) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.tabRepository = tabRepository;
        this.moduleSettingsService = moduleSettingsService;
    }

    @Transactional(readOnly = true)
    public ExpenseListResponse list(UUID tabId, String periodParam) {
        Tab tab = findOwnedTab(tabId);
        int closingDay = moduleSettingsService.getClosingDay(tab.getId(), ModuleType.GASTOS);
        FiscalPeriod period = resolvePeriod(periodParam, closingDay);

        var expenses = expenseRepository
                .findByTabIdAndDateBetweenOrderByDateDesc(tab.getId(), period.start(), period.end())
                .stream().map(this::toResponse).toList();

        return new ExpenseListResponse(period, expenses);
    }

    @Transactional
    public ExpenseResponse create(UUID tabId, ExpenseRequest request) {
        Tab tab = findOwnedTab(tabId);
        Category category = categoryRepository.findByIdAndTabId(request.categoryId(), tab.getId())
                .orElseThrow(() -> new AppException("Categoria não encontrada.", HttpStatus.NOT_FOUND));

        User user = userRepository.getReferenceById(CurrentUser.id());

        int months = resolveOccurrenceCount(request.recurrence(), request.recurrenceMonths());
        UUID recurringGroupId = months > 1 ? UUID.randomUUID() : null;

        Expense first = null;
        for (int i = 0; i < months; i++) {
            Expense expense = new Expense();
            expense.setUser(user);
            expense.setTab(tab);
            expense.setCategory(category);
            expense.setAmount(request.amount());
            expense.setDescription(request.description());
            expense.setDate(request.date().plusMonths(i));
            expense.setRecurringGroupId(recurringGroupId);
            expenseRepository.save(expense);
            if (i == 0) first = expense;
        }

        return toResponse(first);
    }

    /** null/"NONE" = 1 ocorrência avulsa. "FIXED" = recurrenceMonths ocorrências. "INDEFINITE" = um
     * horizonte longo materializado de uma vez (ver INDEFINITE_HORIZON_MONTHS). */
    private int resolveOccurrenceCount(String recurrence, Integer recurrenceMonths) {
        if (recurrence == null || recurrence.isBlank() || "NONE".equalsIgnoreCase(recurrence)) {
            return 1;
        }
        if ("INDEFINITE".equalsIgnoreCase(recurrence)) {
            return INDEFINITE_HORIZON_MONTHS;
        }
        if ("FIXED".equalsIgnoreCase(recurrence)) {
            if (recurrenceMonths == null || recurrenceMonths < 1) {
                throw new AppException("Informe por quantos meses o gasto se repete.", HttpStatus.BAD_REQUEST);
            }
            if (recurrenceMonths > 360) {
                throw new AppException("Máximo de 360 meses de recorrência.", HttpStatus.BAD_REQUEST);
            }
            return recurrenceMonths;
        }
        throw new AppException("Tipo de recorrência inválido.", HttpStatus.BAD_REQUEST);
    }

    @Transactional
    public ExpenseResponse update(UUID id, ExpenseUpdateRequest request) {
        Expense expense = findOwned(id);

        Category category = categoryRepository.findByIdAndTabId(request.categoryId(), expense.getTab().getId())
                .orElseThrow(() -> new AppException("Categoria não encontrada.", HttpStatus.NOT_FOUND));

        if (request.applyToFuture() && expense.getRecurringGroupId() != null) {
            List<Expense> occurrences = expenseRepository
                    .findByRecurringGroupIdAndDateGreaterThanEqual(expense.getRecurringGroupId(), expense.getDate());
            for (Expense e : occurrences) {
                e.setCategory(category);
                e.setAmount(request.amount());
                e.setDescription(request.description());
                // a data de cada ocorrência não muda aqui - só a categoria/valor/descrição se repetem.
            }
            expenseRepository.saveAll(occurrences);
        } else {
            expense.setCategory(category);
            expense.setAmount(request.amount());
            expense.setDescription(request.description());
            expense.setDate(request.date());
            expenseRepository.save(expense);
        }

        return toResponse(expense);
    }

    @Transactional
    public void delete(UUID id, boolean applyToFuture) {
        Expense expense = findOwned(id);
        if (applyToFuture && expense.getRecurringGroupId() != null) {
            expenseRepository.deleteAll(
                    expenseRepository.findByRecurringGroupIdAndDateGreaterThanEqual(expense.getRecurringGroupId(), expense.getDate()));
        } else {
            expenseRepository.delete(expense);
        }
    }

    private Expense findOwned(UUID id) {
        return expenseRepository.findByIdAndUserId(id, CurrentUser.id())
                .orElseThrow(() -> new AppException("Gasto não encontrado.", HttpStatus.NOT_FOUND));
    }

    private Tab findOwnedTab(UUID tabId) {
        return tabRepository.findByIdAndUserId(tabId, CurrentUser.id())
                .orElseThrow(() -> new AppException("Aba não encontrada.", HttpStatus.NOT_FOUND));
    }

    /** Se periodParam vier no formato "YYYY-MM", resolve aquele período específico; senão, usa o atual. */
    static FiscalPeriod resolvePeriod(String periodParam, int closingDay) {
        if (periodParam == null || periodParam.isBlank()) {
            return FiscalPeriodCalculator.getCurrentFiscalPeriod(closingDay);
        }
        String[] parts = periodParam.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        LocalDate reference = LocalDate.of(year, month, Math.min(closingDay, 28));
        return FiscalPeriodCalculator.getFiscalPeriod(reference, closingDay);
    }

    private ExpenseResponse toResponse(Expense e) {
        Category c = e.getCategory();
        return new ExpenseResponse(
                e.getId(), e.getAmount(), e.getDescription(), e.getDate(),
                new CategoryResponse(c.getId(), c.getName(), c.getColor(), c.getIcon()),
                e.getRecurringGroupId()
        );
    }
}
