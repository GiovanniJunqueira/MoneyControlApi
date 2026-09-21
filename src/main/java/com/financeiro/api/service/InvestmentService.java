package com.financeiro.api.service;

import com.financeiro.api.dto.investment.InvestmentProjectionItemResponse;
import com.financeiro.api.dto.investment.InvestmentProjectionResponse;
import com.financeiro.api.dto.investment.InvestmentRequest;
import com.financeiro.api.dto.investment.InvestmentResponse;
import com.financeiro.api.dto.investment.InvestmentTransactionRequest;
import com.financeiro.api.dto.investment.InvestmentsOverviewResponse;
import com.financeiro.api.entity.Investment;
import com.financeiro.api.entity.User;
import com.financeiro.api.exception.AppException;
import com.financeiro.api.repository.InvestmentRepository;
import com.financeiro.api.repository.UserRepository;
import com.financeiro.api.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Investimentos (renda fixa, fundos imobiliários etc.) - lista global do usuário, sem relação
 * nenhuma com abas ou com o módulo Bets. Cada investimento tem um valor investido e uma taxa de
 * rendimento mensal em % - a projeção aplica juros compostos sobre essa taxa (convenção padrão
 * pra "rende X% ao mês" em produtos financeiros brasileiros).
 */
@Service
public class InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final UserRepository userRepository;

    public InvestmentService(InvestmentRepository investmentRepository, UserRepository userRepository) {
        this.investmentRepository = investmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public InvestmentsOverviewResponse list() {
        List<Investment> investments = investmentRepository.findByUserIdOrderByCreatedAtAsc(CurrentUser.id());
        BigDecimal total = investments.stream().map(Investment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new InvestmentsOverviewResponse(total, investments.stream().map(this::toResponse).toList());
    }

    public InvestmentResponse create(InvestmentRequest request) {
        User user = userRepository.getReferenceById(CurrentUser.id());
        Investment investment = new Investment();
        investment.setUser(user);
        applyRequest(investment, request);
        investmentRepository.save(investment);
        return toResponse(investment);
    }

    public InvestmentResponse update(UUID id, InvestmentRequest request) {
        Investment investment = findOwned(id);
        applyRequest(investment, request);
        investmentRepository.save(investment);
        return toResponse(investment);
    }

    public void delete(UUID id) {
        investmentRepository.delete(findOwned(id));
    }

    /** Saque/depósito - ajusta o valor investido sem precisar reeditar o investimento inteiro.
     * Pedido explícito do usuário: "a pessoa possa sempre depositar ou sacar desse valor". */
    public InvestmentResponse applyTransaction(UUID id, InvestmentTransactionRequest request) {
        Investment investment = findOwned(id);
        boolean isWithdrawal = "SAQUE".equalsIgnoreCase(request.type());
        boolean isDeposit = "DEPOSITO".equalsIgnoreCase(request.type());
        if (!isWithdrawal && !isDeposit) {
            throw new AppException("Tipo inválido - use SAQUE ou DEPOSITO.", HttpStatus.BAD_REQUEST);
        }
        BigDecimal delta = isWithdrawal ? request.amount().negate() : request.amount();
        BigDecimal newAmount = investment.getAmount().add(delta);
        if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException("Saldo insuficiente pra esse saque.", HttpStatus.BAD_REQUEST);
        }
        investment.setAmount(newAmount);
        investmentRepository.save(investment);
        return toResponse(investment);
    }

    /**
     * Projeção pra daqui a N meses: aplica juros compostos mensais sobre a taxa de cada investimento
     * (valor final = valor atual × (1 + taxa/100)^meses), soma tudo no final. É uma projeção simples -
     * assume que a taxa se mantém constante e que não entra nem sai dinheiro além do rendimento.
     */
    @Transactional(readOnly = true)
    public InvestmentProjectionResponse projection(int months) {
        if (months < 0 || months > 1200) {
            throw new AppException("Quantidade de meses inválida (máximo 1200).", HttpStatus.BAD_REQUEST);
        }
        List<Investment> investments = investmentRepository.findByUserIdOrderByCreatedAtAsc(CurrentUser.id());

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal projectedTotal = BigDecimal.ZERO;
        List<InvestmentProjectionItemResponse> items = investments.stream().map(inv -> {
            BigDecimal projected = compoundValue(inv.getAmount(), inv.getMonthlyRatePercent(), months);
            return new InvestmentProjectionItemResponse(inv.getId(), inv.getName(), inv.getAmount(),
                    projected, projected.subtract(inv.getAmount()));
        }).toList();

        for (Investment inv : investments) {
            totalAmount = totalAmount.add(inv.getAmount());
        }
        for (InvestmentProjectionItemResponse item : items) {
            projectedTotal = projectedTotal.add(item.projectedAmount());
        }

        return new InvestmentProjectionResponse(months, totalAmount, projectedTotal,
                projectedTotal.subtract(totalAmount), items);
    }

    private BigDecimal compoundValue(BigDecimal amount, BigDecimal monthlyRatePercent, int months) {
        // fator com escala alta antes de elevar à potência, pra não acumular erro de arredondamento
        // ao longo dos meses - só arredonda pra 2 casas no valor final.
        BigDecimal monthlyFactor = BigDecimal.ONE.add(monthlyRatePercent.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        BigDecimal factorPow = monthlyFactor.pow(months);
        return amount.multiply(factorPow).setScale(2, RoundingMode.HALF_UP);
    }

    private void applyRequest(Investment investment, InvestmentRequest request) {
        investment.setName(request.name());
        investment.setAmount(request.amount());
        investment.setMonthlyRatePercent(request.monthlyRatePercent());
    }

    private Investment findOwned(UUID id) {
        return investmentRepository.findByIdAndUserId(id, CurrentUser.id())
                .orElseThrow(() -> new AppException("Investimento não encontrado.", HttpStatus.NOT_FOUND));
    }

    private InvestmentResponse toResponse(Investment investment) {
        return new InvestmentResponse(investment.getId(), investment.getName(), investment.getAmount(),
                investment.getMonthlyRatePercent());
    }
}
