package com.financeiro.api.service;

import com.financeiro.api.dto.debt.*;
import com.financeiro.api.entity.*;
import com.financeiro.api.exception.AppException;
import com.financeiro.api.repository.DebtPaymentRepository;
import com.financeiro.api.repository.DebtRepository;
import com.financeiro.api.repository.DebtorRepository;
import com.financeiro.api.repository.UserRepository;
import com.financeiro.api.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DebtService {

    private final DebtRepository debtRepository;
    private final DebtorRepository debtorRepository;
    private final DebtPaymentRepository debtPaymentRepository;
    private final UserRepository userRepository;

    public DebtService(DebtRepository debtRepository, DebtorRepository debtorRepository,
                        DebtPaymentRepository debtPaymentRepository, UserRepository userRepository) {
        this.debtRepository = debtRepository;
        this.debtorRepository = debtorRepository;
        this.debtPaymentRepository = debtPaymentRepository;
        this.userRepository = userRepository;
    }

    public DebtResponse create(DebtRequest request) {
        Debtor debtor = debtorRepository.findByIdAndUserId(request.debtorId(), CurrentUser.id())
                .orElseThrow(() -> new AppException("Pessoa não encontrada.", HttpStatus.NOT_FOUND));

        User user = userRepository.getReferenceById(CurrentUser.id());

        Debt debt = new Debt();
        debt.setUser(user);
        debt.setDebtor(debtor);
        debt.setAmount(request.amount());
        debt.setReason(request.reason());
        debt.setDate(request.date());
        debtRepository.save(debt);

        return toResponse(debt);
    }

    public DebtResponse update(UUID id, DebtUpdateRequest request) {
        Debt debt = findOwned(id);
        debt.setAmount(request.amount());
        debt.setReason(request.reason());
        debt.setDate(request.date());
        debtRepository.save(debt);
        return toResponse(debt);
    }

    public void delete(UUID id) {
        debtRepository.delete(findOwned(id));
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

    private DebtResponse toResponse(Debt d) {
        return new DebtResponse(d.getId(), d.getAmount(), d.getReason(), d.getDate(), d.getStatus().name().toLowerCase(), d.getPaidAmount());
    }
}
