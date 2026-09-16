package com.financeiro.api.service;

import com.financeiro.api.dto.debt.*;
import com.financeiro.api.entity.*;
import com.financeiro.api.exception.AppException;
import com.financeiro.api.repository.DebtPaymentRepository;
import com.financeiro.api.repository.DebtRepository;
import com.financeiro.api.repository.DebtorRepository;
import com.financeiro.api.repository.TabRepository;
import com.financeiro.api.repository.UserRepository;
import com.financeiro.api.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DebtService {

    private final DebtRepository debtRepository;
    private final DebtorRepository debtorRepository;
    private final DebtPaymentRepository debtPaymentRepository;
    private final UserRepository userRepository;
    private final TabRepository tabRepository;

    public DebtService(DebtRepository debtRepository, DebtorRepository debtorRepository,
                        DebtPaymentRepository debtPaymentRepository, UserRepository userRepository,
                        TabRepository tabRepository) {
        this.debtRepository = debtRepository;
        this.debtorRepository = debtorRepository;
        this.debtPaymentRepository = debtPaymentRepository;
        this.userRepository = userRepository;
        this.tabRepository = tabRepository;
    }

    @Transactional
    public DebtResponse create(UUID tabId, DebtRequest request) {
        Tab tab = findOwnedTab(tabId);
        Debtor debtor = debtorRepository.findByIdAndTabId(request.debtorId(), tab.getId())
                .orElseThrow(() -> new AppException("Pessoa não encontrada.", HttpStatus.NOT_FOUND));

        User user = userRepository.getReferenceById(CurrentUser.id());

        int installments = request.installments() != null && request.installments() > 1 ? request.installments() : 1;
        if (installments > 360) {
            throw new AppException("Máximo de 360 parcelas.", HttpStatus.BAD_REQUEST);
        }
        UUID installmentGroupId = installments > 1 ? UUID.randomUUID() : null;

        // divide o total em N parcelas que somam exatamente o valor pedido - a diferença de
        // arredondamento (se o total não divide igual) fica toda na última parcela.
        BigDecimal installmentAmount = installments > 1
                ? request.amount().divide(BigDecimal.valueOf(installments), 2, RoundingMode.DOWN)
                : request.amount();
        BigDecimal accumulated = BigDecimal.ZERO;

        Debt first = null;
        for (int i = 0; i < installments; i++) {
            BigDecimal amount = (i == installments - 1) ? request.amount().subtract(accumulated) : installmentAmount;
            accumulated = accumulated.add(amount);

            Debt debt = new Debt();
            debt.setUser(user);
            debt.setTab(tab);
            debt.setDebtor(debtor);
            debt.setAmount(amount);
            debt.setReason(request.reason());
            debt.setDate(request.date().plusMonths(i));
            if (installmentGroupId != null) {
                debt.setInstallmentGroupId(installmentGroupId);
                debt.setInstallmentNumber(i + 1);
                debt.setInstallmentTotal(installments);
            }
            debtRepository.save(debt);
            if (i == 0) first = debt;
        }

        return toResponse(first);
    }

    @Transactional
    public DebtResponse update(UUID id, DebtUpdateRequest request) {
        Debt debt = findOwned(id);

        if (request.applyToFuture() && debt.getInstallmentGroupId() != null) {
            List<Debt> installments = debtRepository
                    .findByInstallmentGroupIdAndDateGreaterThanEqual(debt.getInstallmentGroupId(), debt.getDate());
            for (Debt d : installments) {
                d.setAmount(request.amount());
                d.setReason(request.reason());
                // a data de cada parcela não muda aqui - só valor/motivo se repetem.
            }
            debtRepository.saveAll(installments);
        } else {
            debt.setAmount(request.amount());
            debt.setReason(request.reason());
            debt.setDate(request.date());
            debtRepository.save(debt);
        }

        return toResponse(debt);
    }

    @Transactional
    public void delete(UUID id, boolean applyToFuture) {
        Debt debt = findOwned(id);
        if (applyToFuture && debt.getInstallmentGroupId() != null) {
            debtRepository.deleteAll(
                    debtRepository.findByInstallmentGroupIdAndDateGreaterThanEqual(debt.getInstallmentGroupId(), debt.getDate()));
        } else {
            debtRepository.delete(debt);
        }
    }

    public DebtResponse registerPayment(UUID debtId, PaymentRequest request) {
        Debt debt = findOwned(debtId);

        DebtPayment payment = new DebtPayment();
        payment.setDebt(debt);
        payment.setAmount(request.amount());
        payment.setDate(request.date() != null ? request.date() : LocalDateTime.now());
        debtPaymentRepository.save(payment);

        BigDecimal totalPago = debtPaymentRepository.findByDebtId(debt.getId()).stream()
                .map(DebtPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        debt.setPaidAmount(totalPago);
        debt.setStatus(resolveStatus(totalPago, debt.getAmount()));
        debtRepository.save(debt);

        return toResponse(debt);
    }

    private DebtStatus resolveStatus(BigDecimal totalPago, BigDecimal amount) {
        if (totalPago.compareTo(amount) >= 0) return DebtStatus.QUITADO;
        if (totalPago.compareTo(BigDecimal.ZERO) > 0) return DebtStatus.PARCIAL;
        return DebtStatus.PENDENTE;
    }

    private Debt findOwned(UUID id) {
        return debtRepository.findByIdAndUserId(id, CurrentUser.id())
                .orElseThrow(() -> new AppException("Dívida não encontrada.", HttpStatus.NOT_FOUND));
    }

    private Tab findOwnedTab(UUID tabId) {
        return tabRepository.findByIdAndUserId(tabId, CurrentUser.id())
                .orElseThrow(() -> new AppException("Aba não encontrada.", HttpStatus.NOT_FOUND));
    }

    private DebtResponse toResponse(Debt d) {
        return new DebtResponse(d.getId(), d.getAmount(), d.getReason(), d.getDate(), d.getStatus().name().toLowerCase(), d.getPaidAmount(),
                d.getInstallmentGroupId(), d.getInstallmentNumber(), d.getInstallmentTotal());
    }
}
