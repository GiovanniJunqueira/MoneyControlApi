package com.financeiro.api.service;

import com.financeiro.api.dto.bets.*;
import com.financeiro.api.entity.BetDailyBalance;
import com.financeiro.api.entity.BetHouse;
import com.financeiro.api.entity.BetMonth;
import com.financeiro.api.entity.BetMonthStartingBalance;
import com.financeiro.api.entity.BetUnitValueChange;
import com.financeiro.api.entity.User;
import com.financeiro.api.exception.AppException;
import com.financeiro.api.repository.BetDailyBalanceRepository;
import com.financeiro.api.repository.BetHouseRepository;
import com.financeiro.api.repository.BetMonthRepository;
import com.financeiro.api.repository.BetMonthStartingBalanceRepository;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BetService {

    private final BetHouseRepository betHouseRepository;
    private final BetMonthRepository betMonthRepository;
    private final BetUnitValueChangeRepository betUnitValueChangeRepository;
    private final BetDailyBalanceRepository betDailyBalanceRepository;
    private final BetMonthStartingBalanceRepository betMonthStartingBalanceRepository;
    private final UserRepository userRepository;

    public BetService(BetHouseRepository betHouseRepository, BetMonthRepository betMonthRepository,
                       BetUnitValueChangeRepository betUnitValueChangeRepository,
                       BetDailyBalanceRepository betDailyBalanceRepository,
                       BetMonthStartingBalanceRepository betMonthStartingBalanceRepository,
                       UserRepository userRepository) {
        this.betHouseRepository = betHouseRepository;
        this.betMonthRepository = betMonthRepository;
        this.betUnitValueChangeRepository = betUnitValueChangeRepository;
        this.betDailyBalanceRepository = betDailyBalanceRepository;
        this.betMonthStartingBalanceRepository = betMonthStartingBalanceRepository;
        this.userRepository = userRepository;
    }

    // ---- Casas ----

    @Transactional(readOnly = true)
    public List<BetHouseResponse> listHouses() {
        return betHouseRepository.findByUserIdOrderByPositionAsc(CurrentUser.id())
                .stream().map(this::toHouseResponse).toList();
    }

    public BetHouseResponse createHouse(BetHouseRequest request) {
        UUID userId = CurrentUser.id();
        User user = userRepository.getReferenceById(userId);
        BetHouse house = new BetHouse();
        house.setUser(user);
        house.setName(request.name());
        house.setColor(request.color());
        house.setPosition(betHouseRepository.countByUserId(userId));
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

    public void reorderHouses(ReorderHousesRequest request) {
        UUID userId = CurrentUser.id();
        List<BetHouse> houses = betHouseRepository.findAllById(request.houseIds());
        Map<UUID, BetHouse> byId = new HashMap<>();
        for (BetHouse h : houses) {
            if (!h.getUser().getId().equals(userId)) {
                throw new AppException("Casa não encontrada.", HttpStatus.NOT_FOUND);
            }
            byId.put(h.getId(), h);
        }
        List<UUID> ids = request.houseIds();
        for (int i = 0; i < ids.size(); i++) {
            BetHouse house = byId.get(ids.get(i));
            if (house == null) {
                throw new AppException("Casa não encontrada.", HttpStatus.NOT_FOUND);
            }
            house.setPosition(i);
        }
        betHouseRepository.saveAll(houses);
    }

    // ---- Mês ----

    public BetMonthSummaryResponse startMonth(StartMonthRequest request) {
        UUID userId = CurrentUser.id();
        User user = userRepository.getReferenceById(userId);
        LocalDate today = LocalDate.now();

        List<BetHouse> houses = betHouseRepository.findByUserIdOrderByPositionAsc(userId);
        BigDecimal startingBanca = BigDecimal.ZERO;
        Map<BetHouse, BigDecimal> startingBalancesByHouse = new LinkedHashMap<>();
        for (BetHouse h : houses) {
            BigDecimal balance = currentBalance(h.getId());
            startingBalancesByHouse.put(h, balance);
            startingBanca = startingBanca.add(balance);
        }

        // fecha o mes aberto (se tiver) com a MESMA banca que vira a startingBanca do novo mes -
        // grava direto no mes fechado pra não depender do novo mes continuar existindo depois.
        Optional<BetMonth> previousOpen = betMonthRepository.findByUserIdAndEndDateIsNull(userId);
        if (previousOpen.isPresent()) {
            BetMonth open = previousOpen.get();
            open.setEndDate(today);
            open.setEndingBanca(startingBanca);
            betMonthRepository.save(open);
        }

        BetMonth month = new BetMonth();
        month.setUser(user);
        month.setStartDate(today);
        month.setInitialUnitValue(request.initialUnitValue());
        month.setStartingBanca(startingBanca);
        betMonthRepository.save(month);

        for (Map.Entry<BetHouse, BigDecimal> entry : startingBalancesByHouse.entrySet()) {
            BetMonthStartingBalance snapshot = new BetMonthStartingBalance();
            snapshot.setMonth(month);
            snapshot.setHouse(entry.getKey());
            snapshot.setBalance(entry.getValue());
            betMonthStartingBalanceRepository.save(snapshot);
        }

        BetUnitValueChange change = new BetUnitValueChange();
        change.setMonth(month);
        change.setDate(today);
        change.setValue(request.initialUnitValue());
        betUnitValueChangeRepository.save(change);

        return new BetMonthSummaryResponse(month.getId(), month.getStartDate(), null, true,
                startingBanca, startingBanca, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public void updateUnitValue(UpdateUnitValueRequest request) {
        BetMonth month = findOwnedOpenMonth();

        BetUnitValueChange change = new BetUnitValueChange();
        change.setMonth(month);
        change.setDate(LocalDate.now());
        change.setValue(request.value());
        betUnitValueChangeRepository.save(change);
    }

    @Transactional(readOnly = true)
    public List<BetMonthSummaryResponse> listMonthsHistory() {
        UUID userId = CurrentUser.id();
        List<BetMonth> months = betMonthRepository.findByUserIdOrderByStartDateDescCreatedAtDesc(userId);
        // precisa da ordem cronologica (mais antigo primeiro) pra achar a "banca final" de cada mes fechado
        List<BetMonth> chronological = new ArrayList<>(months);
        Collections.reverse(chronological);

        List<BetMonthSummaryResponse> result = new ArrayList<>();
        for (int i = 0; i < chronological.size(); i++) {
            BetMonth m = chronological.get(i);
            boolean open = m.getEndDate() == null;
            BigDecimal endingBanca;
            if (open) {
                endingBanca = totalCurrentBanca(userId);
            } else if (m.getEndingBanca() != null) {
                endingBanca = m.getEndingBanca();
            } else if (i + 1 < chronological.size()) {
                // fallback pra meses fechados antes da coluna ending_banca existir
                endingBanca = chronological.get(i + 1).getStartingBanca();
            } else {
                endingBanca = m.getStartingBanca();
            }
            BigDecimal profitLoss = endingBanca.subtract(m.getStartingBanca());
            LocalDate unitRefDate = m.getEndDate() != null ? m.getEndDate() : LocalDate.now();
            BigDecimal profitLossUnits = divideForUnits(profitLoss, resolveUnitValue(m, unitRefDate));
            result.add(new BetMonthSummaryResponse(m.getId(), m.getStartDate(), m.getEndDate(), open,
                    m.getStartingBanca(), endingBanca, profitLoss, profitLossUnits));
        }

        result.sort((a, b) -> b.startDate().compareTo(a.startDate()));
        return result;
    }

    @Transactional(readOnly = true)
    public BetOverviewResponse overview() {
        UUID userId = CurrentUser.id();
        List<BetMonth> months = betMonthRepository.findByUserIdOrderByStartDateDescCreatedAtDesc(userId);
        if (months.isEmpty()) {
            return new BetOverviewResponse(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal totalBanca = totalCurrentBanca(userId);
        BetMonth oldest = months.get(months.size() - 1);
        BigDecimal totalProfit = totalBanca.subtract(oldest.getStartingBanca());

        BetMonth mostRecent = months.get(0);
        LocalDate unitRefDate = mostRecent.getEndDate() != null ? mostRecent.getEndDate() : LocalDate.now();
        BigDecimal totalProfitUnits = divideForUnits(totalProfit, resolveUnitValue(mostRecent, unitRefDate));

        return new BetOverviewResponse(totalProfit, totalProfitUnits);
    }

    /** Lista todos os dias do mês (do início até hoje ou até o fechamento), com o resultado do dia e por casa. */
    @Transactional(readOnly = true)
    public BetMonthDaysResponse listMonthDays(UUID monthId) {
        UUID userId = CurrentUser.id();
        BetMonth month = betMonthRepository.findByIdAndUserId(monthId, userId)
                .orElseThrow(() -> new AppException("Mês não encontrado.", HttpStatus.NOT_FOUND));

        boolean open = month.getEndDate() == null;
        LocalDate rangeEnd = open ? LocalDate.now() : month.getEndDate();

        List<BetHouse> houses = betHouseRepository.findByUserIdOrderByPositionAsc(userId);
        List<UUID> houseIds = houses.stream().map(BetHouse::getId).toList();

        // saldo de cada casa no instante em que o mês começou - ponto de partida do carry-forward.
        // vem do snapshot (não de "saldo antes da data"), porque duas datas de meses diferentes podem
        // ser o mesmo dia (ver BetMonthStartingBalance). Casas criadas depois do mês começar (sem
        // snapshot) partem de zero, que é o comportamento certo pra elas.
        Map<UUID, BigDecimal> snapshotByHouse = betMonthStartingBalanceRepository.findByMonthId(monthId).stream()
                .collect(Collectors.toMap(s -> s.getHouse().getId(), BetMonthStartingBalance::getBalance));
        Map<UUID, BigDecimal> runningBalance = new LinkedHashMap<>();
        for (BetHouse h : houses) {
            runningBalance.put(h.getId(), snapshotByHouse.getOrDefault(h.getId(), BigDecimal.ZERO));
        }

        Map<UUID, Map<LocalDate, BetDailyBalance>> entriesByHouse = new HashMap<>();
        if (!houseIds.isEmpty()) {
            for (BetDailyBalance e : betDailyBalanceRepository
                    .findByHouseIdInAndDateBetweenOrderByDateAsc(houseIds, month.getStartDate(), rangeEnd)) {
                entriesByHouse.computeIfAbsent(e.getHouse().getId(), k -> new HashMap<>()).put(e.getDate(), e);
            }
        }

        List<BetMonthDayResponse> days = new ArrayList<>();
        for (LocalDate date = month.getStartDate(); !date.isAfter(rangeEnd); date = date.plusDays(1)) {
            BigDecimal unitValue = resolveUnitValue(month, date);
            List<BetMonthDayHouseResponse> houseRows = new ArrayList<>();
            BigDecimal dayTotalOpening = BigDecimal.ZERO;
            BigDecimal dayTotalClosing = BigDecimal.ZERO;

            for (BetHouse h : houses) {
                BigDecimal defaultOpening = runningBalance.get(h.getId());
                BetDailyBalance entry = entriesByHouse.getOrDefault(h.getId(), Map.of()).get(date);
                BigDecimal closing = entry != null ? entry.getBalance() : defaultOpening;
                BigDecimal opening = entry != null && entry.getOpeningBalance() != null ? entry.getOpeningBalance() : defaultOpening;
                BigDecimal result = closing.subtract(opening);

                houseRows.add(new BetMonthDayHouseResponse(h.getId(), h.getName(), h.getColor(),
                        closing, divideForUnits(closing, unitValue), result, divideForUnits(result, unitValue)));

                dayTotalOpening = dayTotalOpening.add(opening);
                dayTotalClosing = dayTotalClosing.add(closing);
                runningBalance.put(h.getId(), closing); // sempre carrega o saldo FINAL, mesmo se o inicial foi ajustado
            }

            BigDecimal dayResult = dayTotalClosing.subtract(dayTotalOpening);
            days.add(new BetMonthDayResponse(date, unitValue, dayTotalClosing, divideForUnits(dayTotalClosing, unitValue),
                    dayResult, divideForUnits(dayResult, unitValue), houseRows));
        }
        Collections.reverse(days); // mais recente primeiro

        BigDecimal endingBanca = days.isEmpty() ? month.getStartingBanca() : days.get(0).total();
        BigDecimal profitLoss = endingBanca.subtract(month.getStartingBanca());
        BigDecimal profitLossUnits = days.isEmpty() ? BigDecimal.ZERO : divideForUnits(profitLoss, days.get(0).unitValue());

        return new BetMonthDaysResponse(month.getId(), month.getStartDate(), month.getEndDate(), open,
                month.getStartingBanca(), endingBanca, profitLoss, profitLossUnits, days);
    }

    // ---- Saldo diário ----

    public void updateHouseBalance(UUID houseId, UpdateBalanceRequest request) {
        BetMonth month = findOwnedOpenMonth();
        BetHouse house = findOwnedHouse(houseId);
        LocalDate today = LocalDate.now();
        LocalDate date = request.date() != null ? request.date() : today;
        if (date.isAfter(today) || date.isBefore(month.getStartDate())) {
            throw new AppException("Só é possível editar dias do mês atual, até hoje.", HttpStatus.BAD_REQUEST);
        }

        BetDailyBalance entry = betDailyBalanceRepository.findByHouseIdAndDate(house.getId(), date)
                .orElseGet(BetDailyBalance::new);
        entry.setHouse(house);
        entry.setMonth(month);
        entry.setDate(date);
        entry.setBalance(request.balance());
        entry.setOpeningBalance(request.openingBalance());
        betDailyBalanceRepository.save(entry);
    }

    public void deleteMonth(UUID id) {
        BetMonth month = betMonthRepository.findByIdAndUserId(id, CurrentUser.id())
                .orElseThrow(() -> new AppException("Mês não encontrado.", HttpStatus.NOT_FOUND));
        betMonthRepository.delete(month);
    }

    // ---- Helpers ----

    private BigDecimal totalCurrentBanca(UUID userId) {
        return betHouseRepository.findByUserIdOrderByPositionAsc(userId).stream()
                .map(h -> currentBalance(h.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal currentBalance(UUID houseId) {
        return betDailyBalanceRepository.findTopByHouseIdOrderByDateDesc(houseId)
                .map(BetDailyBalance::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * A mudança mais recente com date <= referência - é o valor vigente naquele dia.
     * Duas mudanças no mesmo dia empatam em "date", por isso o desempate por createdAt:
     * a mais recente delas (a última que a pessoa registrou naquele dia) vence.
     */
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
        return new BetHouseResponse(h.getId(), h.getName(), h.getColor(), h.getPosition());
    }
}
