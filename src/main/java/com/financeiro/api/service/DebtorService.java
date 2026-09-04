package com.financeiro.api.service;

import com.financeiro.api.dto.debt.DebtResponse;
import com.financeiro.api.dto.debt.DebtorDetailResponse;
import com.financeiro.api.dto.debtor.DebtorRequest;
import com.financeiro.api.dto.debtor.DebtorSummaryResponse;
import com.financeiro.api.entity.Debt;
import com.financeiro.api.entity.DebtStatus;
import com.financeiro.api.entity.Debtor;
import com.financeiro.api.entity.User;
import com.financeiro.api.exception.AppException;
import com.financeiro.api.repository.DebtorRepository;
import com.financeiro.api.repository.UserRepository;
import com.financeiro.api.security.CurrentUser;
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

    public DebtorService(DebtorRepository debtorRepository, UserRepository userRepository) {
        this.debtorRepository = debtorRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<DebtorSummaryResponse> list() {
        return debtorRepository.findByUserIdOrderByNameAsc(CurrentUser.id()).stream().map(d -> {
            BigDecimal totalDevido = d.getDebts().stream()
                    .filter(debt -> debt.getStatus() != DebtStatus.QUITADO)
                    .map(debt -> debt.getAmount().subtract(debt.getPaidAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long quantidade = d.getDebts().stream().filter(debt -> debt.getStatus() != DebtStatus.QUITADO).count();

            return new DebtorSummaryResponse(d.getId(), d.getName(), d.getNotes(), totalDevido, quantidade);
        }).toList();
    }

    @Transactional(readOnly = true)
    public DebtorDetailResponse detail(UUID id) {
        Debtor debtor = findOwned(id);
        List<DebtResponse> debts = debtor.getDebts().stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .map(this::toDebtResponse)
                .toList();
        return new DebtorDetailResponse(debtor.getId(), debtor.getName(), debtor.getNotes(), debts);
    }

    public DebtorSummaryResponse create(DebtorRequest request) {
        User user = userRepository.getReferenceById(CurrentUser.id());
        Debtor debtor = new Debtor();
        debtor.setUser(user);
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

    private DebtResponse toDebtResponse(Debt d) {
        return new DebtResponse(d.getId(), d.getAmount(), d.getReason(), d.getDate(), d.getStatus().name().toLowerCase(), d.getPaidAmount());
    }
}
