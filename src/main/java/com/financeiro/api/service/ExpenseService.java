package com.financeiro.api.service;

import com.financeiro.api.dto.category.CategoryResponse;
import com.financeiro.api.dto.expense.ExpenseListResponse;
import com.financeiro.api.dto.expense.ExpenseRequest;
import com.financeiro.api.dto.expense.ExpenseResponse;
import com.financeiro.api.entity.Category;
import com.financeiro.api.entity.Expense;
import com.financeiro.api.entity.ModuleType;
import com.financeiro.api.entity.User;
import com.financeiro.api.exception.AppException;
import com.financeiro.api.repository.CategoryRepository;
import com.financeiro.api.repository.ExpenseRepository;
import com.financeiro.api.repository.UserRepository;
import com.financeiro.api.security.CurrentUser;
import com.financeiro.api.util.FiscalPeriod;
import com.financeiro.api.util.FiscalPeriodCalculator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ModuleSettingsService moduleSettingsService;

    public ExpenseService(ExpenseRepository expenseRepository, CategoryRepository categoryRepository,
                           UserRepository userRepository, ModuleSettingsService moduleSettingsService) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.moduleSettingsService = moduleSettingsService;
    }

    @Transactional(readOnly = true)
    public ExpenseListResponse list(String periodParam) {
        int closingDay = moduleSettingsService.getClosingDay(ModuleType.GASTOS);
        FiscalPeriod period = resolvePeriod(periodParam, closingDay);

        var expenses = expenseRepository
                .findByUserIdAndDateBetweenOrderByDateDesc(CurrentUser.id(), period.start(), period.end())
                .stream().map(this::toResponse).toList();

        return new ExpenseListResponse(period, expenses);
    }

    public ExpenseResponse create(ExpenseRequest request) {
        Category category = categoryRepository.findByIdAndUserId(request.categoryId(), CurrentUser.id())
                .orElseThrow(() -> new AppException("Categoria não encontrada.", HttpStatus.NOT_FOUND));

        User user = userRepository.getReferenceById(CurrentUser.id());

        Expense expense = new Expense();
        expense.setUser(user);
        expense.setCategory(category);
        expense.setAmount(request.amount());
        expense.setDescription(request.description());
        expense.setDate(request.date());
        expenseRepository.save(expense);

        return toResponse(expense);
    }

    public ExpenseResponse update(UUID id, ExpenseRequest request) {
        Expense expense = findOwned(id);

        Category category = categoryRepository.findByIdAndUserId(request.categoryId(), CurrentUser.id())
                .orElseThrow(() -> new AppException("Categoria não encontrada.", HttpStatus.NOT_FOUND));

        expense.setCategory(category);
        expense.setAmount(request.amount());
        expense.setDescription(request.description());
        expense.setDate(request.date());
        expenseRepository.save(expense);

        return toResponse(expense);
    }

    public void delete(UUID id) {
        expenseRepository.delete(findOwned(id));
    }

    private Expense findOwned(UUID id) {
        return expenseRepository.findByIdAndUserId(id, CurrentUser.id())
                .orElseThrow(() -> new AppException("Gasto não encontrado.", HttpStatus.NOT_FOUND));
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
                new CategoryResponse(c.getId(), c.getName(), c.getColor(), c.getIcon())
        );
    }
}
