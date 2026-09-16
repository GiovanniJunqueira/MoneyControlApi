package com.financeiro.api.service;

import com.financeiro.api.dto.debt.DebtResponse;
import com.financeiro.api.dto.debt.DebtorDetailResponse;
import com.financeiro.api.dto.debtor.DebtorRequest;
import com.financeiro.api.dto.debtor.DebtorSummaryResponse;
import com.financeiro.api.entity.Debt;
import com.financeiro.api.entity.DebtStatus;
import com.financeiro.api.entity.Debtor;
import com.financeiro.api.entity.ModuleType;
import com.financeiro.api.entity.Tab;
import com.financeiro.api.entity.User;
import com.financeiro.api.exception.AppException;
import com.financeiro.api.repository.DebtorRepository;
import com.financeiro.api.repository.TabRepository;
import com.financeiro.api.repository.UserRepository;
import com.financeiro.api.security.CurrentUser;
import com.financeiro.api.util.FiscalPeriod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class DebtorService {

    private final DebtorRepository debtorRepository;
    private final UserRepository userRepository;
    private final TabRepository tabRepository;
    private final ModuleSettingsService moduleSettingsService;

    public DebtorService(DebtorRepository debtorRepository, UserRepository userRepository, TabRepository tabRepository,
                          ModuleSettingsService moduleSettingsService) {
        this.debtorRepository = debtorRepository;
        this.userRepository = userRepository;
        this.tabRepository = tabRepository;
        this.moduleSettingsService = moduleSettingsService;
    }

    @Transactional(readOnly = true)
    public List<DebtorSummaryResponse> list(UUID tabId) {
        Tab tab = findOwnedTab(tabId);
        return debtorRepository.findByTabIdOrderByNameAsc(tab.getId()).stream().map(d -> {
            BigDecimal totalDevido = d.getDebts().stream()
                    .filter(debt -> debt.getStatus() != DebtStatus.QUITADO)
                    .map(debt -> debt.getAmount().subtract(debt.getPaidAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long quantidade = d.getDebts().stream().filter(debt -> debt.getStatus() != DebtStatus.QUITADO).count();

            return new DebtorSummaryResponse(d.getId(), d.getName(), d.getNotes(), totalDevido, quantidade);
        }).toList();
    }

    @Transactional(readOnly = true)
    public DebtorDetailResponse detail(UUID id, String periodParam) {
        Debtor debtor = findOwned(id);

        BigDecimal totalDevido = debtor.getDebts().stream()
                .filter(debt -> debt.getStatus() != DebtStatus.QUITADO)
                .map(debt -> debt.getAmount().subtract(debt.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int closingDay = moduleSettingsService.getClosingDay(debtor.getTab().getId(), ModuleType.DEVEDORES);
        FiscalPeriod period = ExpenseService.resolvePeriod(periodParam, closingDay);

        List<DebtResponse> debts = debtor.getDebts().stream()
                .filter(d -> !d.getDate().isBefore(period.start()) && !d.getDate().isAfter(period.end()))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .map(this::toDebtResponse)
                .toList();

        return new DebtorDetailResponse(debtor.getId(), debtor.getName(), debtor.getNotes(), totalDevido, period, debts);
    }

    public DebtorSummaryResponse create(UUID tabId, DebtorRequest request) {
        Tab tab = findOwnedTab(tabId);
        User user = userRepository.getReferenceById(CurrentUser.id());
        Debtor debtor = new Debtor();
        debtor.setUser(user);
        debtor.setTab(tab);
        debtor.setName(request.name());
        debtor.setNotes(request.notes());
        debtorRepository.save(debtor);
        return new DebtorSummaryResponse(debtor.getId(), debtor.getName(), debtor.getNotes(), BigDecimal.ZERO, 0);
    }

    public DebtorSummaryResponse update(UUID id, DebtorRequest request) {
        Debtor debtor = findOwned(id);
        debtor.setName(request.name());
        debtor.setNotes(request.notes());
        debtorRepository.save(debtor);
        return new DebtorSummaryResponse(debtor.getId(), debtor.getName(), debtor.getNotes(), BigDecimal.ZERO, 0);
    }

    public void delete(UUID id) {
        debtorRepository.delete(findOwned(id));
    }

    private Debtor findOwned(UUID id) {
        return debtorRepository.findByIdAndUserId(id, CurrentUser.id())
                .orElseThrow(() -> new AppException("Pessoa não encontrada.", HttpStatus.NOT_FOUND));
    }

    private Tab findOwnedTab(UUID tabId) {
        return tabRepository.findByIdAndUserId(tabId, CurrentUser.id())
                .orElseThrow(() -> new AppException("Aba não encontrada.", HttpStatus.NOT_FOUND));
    }

    private DebtResponse toDebtResponse(Debt d) {
        return new DebtResponse(d.getId(), d.getAmount(), d.getReason(), d.getDate(), d.getStatus().name().toLowerCase(), d.getPaidAmount(),
                d.getInstallmentGroupId(), d.getInstallmentNumber(), d.getInstallmentTotal());
    }
}
