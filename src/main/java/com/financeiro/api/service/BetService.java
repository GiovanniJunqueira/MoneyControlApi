package com.financeiro.api.service;

import com.financeiro.api.dto.bets.*;
import com.financeiro.api.entity.BetDailyBalance;
import com.financeiro.api.entity.BetHouse;
import com.financeiro.api.entity.BetMonth;
import com.financeiro.api.entity.BetUnitValueChange;
import com.financeiro.api.entity.User;
import com.financeiro.api.exception.AppException;
import com.financeiro.api.repository.BetDailyBalanceRepository;
import com.financeiro.api.repository.BetHouseRepository;
import com.financeiro.api.repository.BetMonthRepository;
import com.financeiro.api.repository.BetUnitValueChangeRepository;
import com.financeiro.api.repository.UserRepository;
import com.financeiro.api.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class BetService {

    private final BetHouseRepository betHouseRepository;
    private final BetMonthRepository betMonthRepository;
    private final BetUnitValueChangeRepository betUnitValueChangeRepository;
    private final BetDailyBalanceRepository betDailyBalanceRepository;
    private final UserRepository userRepository;

    public BetService(BetHouseRepository betHouseRepository, BetMonthRepository betMonthRepository,
                       BetUnitValueChangeRepository betUnitValueChangeRepository,
                       BetDailyBalanceRepository betDailyBalanceRepository, UserRepository userRepository) {
        this.betHouseRepository = betHouseRepository;
        this.betMonthRepository = betMonthRepository;
        this.betUnitValueChangeRepository = betUnitValueChangeRepository;
        this.betDailyBalanceRepository = betDailyBalanceRepository;
        this.userRepository = userRepository;
    }

    // ---- Casas ----

    @Transactional(readOnly = true)
    public List<BetHouseResponse> listHouses() {
        return betHouseRepository.findByUserIdOrderByNameAsc(CurrentUser.id())
                .stream().map(this::toHouseResponse).toList();
    }

    public BetHouseResponse createHouse(BetHouseRequest request) {
        User user = userRepository.getReferenceById(CurrentUser.id());
        BetHouse house = new BetHouse();
        house.setUser(user);
        house.setName(request.name());
        house.setColor(request.color());
        betHouseRepository.save(house);
        return toHouseResponse(house);
    }

    public BetHouseResponse updateHouse(UUID id, BetHouseRequest request) {
        BetHouse house = findOwnedHouse(id);
        house.setName(request.name());
        house.setColor(request.color());
        betHouseRepository.save(house);
        return toHouseResponse(house);
    }

    public void deleteHouse(UUID id) {
        betHouseRepository.delete(findOwnedHouse(id));
    }

    // ---- Mês ----

    @Transactional(readOnly = true)
    public BetMonthResponse currentMonth() {
        return betMonthRepository.findByUserIdAndEndDateIsNull(CurrentUser.id())
                .map(this::buildDashboard)
                .orElse(null);
    }

    public BetMonthResponse startMonth(StartMonthRequest request) {
        UUID userId = CurrentUser.id();
        User user = userRepository.getReferenceById(userId);
        LocalDate today = LocalDate.now();

        betMonthRepository.findByUserIdAndEndDateIsNull(userId).ifPresent(open -> {
            open.setEndDate(today);
            betMonthRepository.save(open);
        });

        BigDecimal startingBanca = betHouseRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(h -> currentBalance(h.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BetMonth month = new BetMonth();
        month.setUser(user);
        month.setStartDate(today);
        month.setInitialUnitValue(request.initialUnitValue());
        month.setStartingBanca(startingBanca);
        betMonthRepository.save(month);

        BetUnitValueChange change = new BetUnitValueChange();
        change.setMonth(month);
        change.setDate(today);
        change.setValue(request.initialUnitValue());
        betUnitValueChangeRepository.save(change);

        return buildDashboard(month);
    }

    public BetMonthResponse updateUnitValue(UpdateUnitValueRequest request) {
        BetMonth month = findOwnedOpenMonth();
        LocalDate today = LocalDate.now();

        BetUnitValueChange change = new BetUnitValueChange();
        change.setMonth(month);
        change.setDate(today);
        change.setValue(request.value());
        betUnitValueChangeRepository.save(change);

        return buildDashboard(month);
    }

    @Transactional(readOnly = true)
    public List<BetMonthHistoryResponse> listMonthsHistory() {
        List<BetMonth> months = betMonthRepository.findByUserIdOrderByStartDateDesc(CurrentUser.id());
        // precisa da ordem cronologica (mais antigo primeiro) pra achar a "banca final" de cada mes fechado
        List<BetMonth> chronological = new ArrayList<>(months);
        Collections.reverse(chronological);

        List<BetMonthHistoryResponse> result = new ArrayList<>();
        for (int i = 0; i < chronological.size(); i++) {
            BetMonth m = chronological.get(i);
            BigDecimal endingBanca;
            if (i + 1 < chronological.size()) {
                endingBanca = chronological.get(i + 1).getStartingBanca();
            } else if (m.getEndDate() == null) {
                // mes aberto (o atual): banca final "por enquanto" e a banca total agora
                endingBanca = betHouseRepository.findByUserIdOrderByNameAsc(CurrentUser.id()).stream()
                        .map(h -> currentBalance(h.getId()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } else {
                endingBanca = m.getStartingBanca();
            }
            BigDecimal profitLoss = endingBanca.subtract(m.getStartingBanca());
            result.add(new BetMonthHistoryResponse(m.getId(), m.getStartDate(), m.getEndDate(), m.getStartingBanca(), endingBanca, profitLoss));
        }

        result.sort((a, b) -> b.startDate().compareTo(a.startDate()));
        return result;
    }

    // ---- Saldo diário ----

    public BetHouseBalanceResponse updateHouseBalance(UUID houseId, UpdateBalanceRequest request) {
        BetMonth month = findOwnedOpenMonth();
        BetHouse house = findOwnedHouse(houseId);
        LocalDate today = LocalDate.now();

        BetDailyBalance entry = betDailyBalanceRepository.findByHouseIdAndDate(house.getId(), today)
                .orElseGet(BetDailyBalance::new);
        entry.setHouse(house);
        entry.setMonth(month);
        entry.setDate(today);
        entry.setBalance(request.balance());
        betDailyBalanceRepository.save(entry);

        BigDecimal unitValue = resolveUnitValue(month, today);
        BigDecimal startOfDay = betDailyBalanceRepository.findTopByHouseIdAndDateLessThanOrderByDateDesc(house.getId(), today)
                .map(BetDailyBalance::getBalance).orElse(BigDecimal.ZERO);

        return new BetHouseBalanceResponse(house.getId(), house.getName(), house.getColor(),
                request.balance(), divideForUnits(request.balance(), unitValue), startOfDay, true);
    }

    // ---- Helpers ----

    private BetMonthResponse buildDashboard(BetMonth month) {
        LocalDate today = LocalDate.now();
        BigDecimal unitValue = resolveUnitValue(month, today);

        List<BetHouse> houses = betHouseRepository.findByUserIdOrderByNameAsc(CurrentUser.id());
        List<BetHouseBalanceResponse> houseResponses = new ArrayList<>();
        BigDecimal totalBanca = BigDecimal.ZERO;

        for (BetHouse h : houses) {
            BigDecimal current = currentBalance(h.getId());
            BigDecimal startOfDay = betDailyBalanceRepository.findTopByHouseIdAndDateLessThanOrderByDateDesc(h.getId(), today)
                    .map(BetDailyBalance::getBalance).orElse(BigDecimal.ZERO);
            boolean updatedToday = betDailyBalanceRepository.findByHouseIdAndDate(h.getId(), today).isPresent();
            totalBanca = totalBanca.add(current);
            houseResponses.add(new BetHouseBalanceResponse(h.getId(), h.getName(), h.getColor(), current,
                    divideForUnits(current, unitValue), startOfDay, updatedToday));
        }

        BigDecimal profitLoss = totalBanca.subtract(month.getStartingBanca());

        return new BetMonthResponse(month.getId(), month.getStartDate(), unitValue, totalBanca,
                divideForUnits(totalBanca, unitValue), profitLoss, divideForUnits(profitLoss, unitValue), houseResponses);
    }

    private BigDecimal currentBalance(UUID houseId) {
        return betDailyBalanceRepository.findTopByHouseIdOrderByDateDesc(houseId)
                .map(BetDailyBalance::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal resolveUnitValue(BetMonth month, LocalDate date) {
        return betUnitValueChangeRepository.findTopByMonthIdAndDateLessThanEqualOrderByDateDescCreatedAtDesc(month.getId(), date)
                .map(BetUnitValueChange::getValue)
                .orElse(month.getInitialUnitValue());
    }

    private BigDecimal divideForUnits(BigDecimal amount, BigDecimal unitValue) {
        if (unitValue == null || unitValue.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return amount.divide(unitValue, 2, RoundingMode.HALF_UP);
    }

    private BetHouse findOwnedHouse(UUID id) {
        return betHouseRepository.findByIdAndUserId(id, CurrentUser.id())
                .orElseThrow(() -> new AppException("Casa não encontrada.", HttpStatus.NOT_FOUND));
    }

    private BetMonth findOwnedOpenMonth() {
        return betMonthRepository.findByUserIdAndEndDateIsNull(CurrentUser.id())
                .orElseThrow(() -> new AppException("Nenhum mês aberto. Inicie um mês primeiro.", HttpStatus.CONFLICT));
    }

    private BetHouseResponse toHouseResponse(BetHouse h) {
        return new BetHouseResponse(h.getId(), h.getName(), h.getColor());
    }
}
