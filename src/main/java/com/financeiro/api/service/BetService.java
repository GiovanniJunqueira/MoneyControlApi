package com.financeiro.api.service;

import com.financeiro.api.dto.bets.*;
import com.financeiro.api.entity.BetDailyBalance;
import com.financeiro.api.entity.BetHouse;
import com.financeiro.api.entity.BetHouseGroup;
import com.financeiro.api.entity.BetMonth;
import com.financeiro.api.entity.BetMonthStartingBalance;
import com.financeiro.api.entity.BetUnitValueChange;
import com.financeiro.api.entity.User;
import com.financeiro.api.exception.AppException;
import com.financeiro.api.repository.BetDailyBalanceRepository;
import com.financeiro.api.repository.BetHouseGroupRepository;
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
import java.time.DateTimeException;
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
    private final BetHouseGroupRepository betHouseGroupRepository;
    private final BetMonthRepository betMonthRepository;
    private final BetUnitValueChangeRepository betUnitValueChangeRepository;
    private final BetDailyBalanceRepository betDailyBalanceRepository;
    private final BetMonthStartingBalanceRepository betMonthStartingBalanceRepository;
    private final UserRepository userRepository;

    public BetService(BetHouseRepository betHouseRepository, BetHouseGroupRepository betHouseGroupRepository,
                       BetMonthRepository betMonthRepository,
                       BetUnitValueChangeRepository betUnitValueChangeRepository,
                       BetDailyBalanceRepository betDailyBalanceRepository,
                       BetMonthStartingBalanceRepository betMonthStartingBalanceRepository,
                       UserRepository userRepository) {
        this.betHouseRepository = betHouseRepository;
        this.betHouseGroupRepository = betHouseGroupRepository;
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

    @Transactional
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

    // ---- Agrupamento de casas ----

    @Transactional(readOnly = true)
    public List<BetHouseGroupResponse> listHouseGroups() {
        return betHouseGroupRepository.findByUserIdOrderByCreatedAtAsc(CurrentUser.id())
                .stream().map(g -> new BetHouseGroupResponse(g.getId(), g.getName())).toList();
    }

    public BetHouseGroupResponse createHouseGroup(BetHouseGroupRequest request) {
        User user = userRepository.getReferenceById(CurrentUser.id());
        BetHouseGroup group = new BetHouseGroup();
        group.setUser(user);
        group.setName(request.name());
        betHouseGroupRepository.save(group);
        return new BetHouseGroupResponse(group.getId(), group.getName());
    }

    /** Exclui o grupo - as casas que pertenciam a ele voltam a ficar sem grupo (ON DELETE SET NULL). */
    public void deleteHouseGroup(UUID id) {
        BetHouseGroup group = betHouseGroupRepository.findByIdAndUserId(id, CurrentUser.id())
                .orElseThrow(() -> new AppException("Grupo não encontrado.", HttpStatus.NOT_FOUND));
        betHouseGroupRepository.delete(group);
    }

    @Transactional
    public BetHouseResponse assignHouseGroup(UUID houseId, AssignHouseGroupRequest request) {
        BetHouse house = findOwnedHouse(houseId);
        if (request.groupId() == null) {
            house.setGroup(null);
        } else {
            BetHouseGroup group = betHouseGroupRepository.findByIdAndUserId(request.groupId(), CurrentUser.id())
                    .orElseThrow(() -> new AppException("Grupo não encontrado.", HttpStatus.NOT_FOUND));
            house.setGroup(group);
        }
        betHouseRepository.save(house);
        return toHouseResponse(house);
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

    @Transactional
    public BetMonthSummaryResponse startMonth(StartMonthRequest request) {
        UUID userId = CurrentUser.id();
        User user = userRepository.getReferenceById(userId);
        LocalDate today = LocalDate.now();

        LocalDate chosenStart;
        try {
            chosenStart = LocalDate.of(request.year(), request.month(), 1);
        } catch (Exception e) {
            throw new AppException("Mês/ano inválido.", HttpStatus.BAD_REQUEST);
        }
        LocalDate chosenLastDay = chosenStart.withDayOfMonth(chosenStart.lengthOfMonth());
        boolean fullyPast = chosenLastDay.isBefore(today);

        List<BetHouse> houses = betHouseRepository.findByUserIdOrderByPositionAsc(userId);
        BigDecimal startingBanca = BigDecimal.ZERO;
        Map<BetHouse, BigDecimal> startingBalancesByHouse = new LinkedHashMap<>();
        for (BetHouse h : houses) {
            // mês totalmente no passado: usa o saldo que a casa tinha NAQUELA época (antes do mês
            // começar), não o saldo de agora - senão uma casa criada/alimentada só meses depois
            // (ex: redistribuição de "restante das casas" feita no mês atual) vaza seu saldo de HOJE
            // pra trás, inflando a banca de um mês em que ela nem existia ainda. Mês atual/futuro
            // continua usando o saldo real de agora mesmo (é literalmente o que "abrir um mês novo"
            // deve fazer).
            BigDecimal balance = fullyPast
                    ? betDailyBalanceRepository.findTopByHouseIdAndDateLessThanOrderByDateDesc(h.getId(), chosenStart)
                            .map(BetDailyBalance::getBalance).orElse(BigDecimal.ZERO)
                    : currentBalance(h.getId());
            startingBalancesByHouse.put(h, balance);
            startingBanca = startingBanca.add(balance);
        }

        // um mês totalmente no passado (ex: registrar agosto estando em setembro) não mexe no mês
        // aberto atual - convive como um registro histórico à parte, já nascendo fechado.
        if (!fullyPast) {
            betMonthRepository.findByUserIdAndEndDateIsNull(userId).ifPresent(open -> {
                open.setEndDate(today);
                betMonthRepository.save(open);
            });
        }

        BetMonth month = new BetMonth();
        month.setUser(user);
        month.setStartDate(chosenStart);
        month.setEndDate(fullyPast ? chosenLastDay : null);
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
        change.setDate(chosenStart);
        change.setValue(request.initialUnitValue());
        betUnitValueChangeRepository.save(change);

        return toSummary(month, houses);
    }

    public void updateUnitValue(UpdateUnitValueRequest request) {
        BetMonth month = findOwnedOpenMonth();

        BetUnitValueChange change = new BetUnitValueChange();
        change.setMonth(month);
        change.setDate(LocalDate.now());
        change.setValue(request.value());
        betUnitValueChangeRepository.save(change);
    }

    /**
     * O resumo de cada mês é computado dinamicamente (nunca guardado), reaproveitando a mesma
     * varredura dia-a-dia de {@link #computeMonthDays}. Sem isso, editar um mês fechado (permitido -
     * ver updateHouseBalance) deixaria o resumo desatualizado.
     */
    @Transactional(readOnly = true)
    public List<BetMonthSummaryResponse> listMonthsHistory() {
        UUID userId = CurrentUser.id();
        List<BetMonth> months = betMonthRepository.findByUserIdOrderByStartDateDescCreatedAtDesc(userId);
        List<BetHouse> houses = betHouseRepository.findByUserIdOrderByPositionAsc(userId);
        return months.stream().map(m -> toSummary(m, houses)).toList();
    }

    /** Resumo do mês de UM USUÁRIO ESPECÍFICO num ano/mês exato - usado pelo ranking de competição
     * pra comparar o resultado de cada participante sem expor nada além do total do mês dele. Vazio
     * se a pessoa nunca começou um mês nesse período. */
    @Transactional(readOnly = true)
    public Optional<BetMonthSummaryResponse> summaryForUserAndPeriod(UUID userId, int year, int month) {
        LocalDate startDate;
        try {
            startDate = LocalDate.of(year, month, 1);
        } catch (DateTimeException e) {
            return Optional.empty();
        }
        return betMonthRepository.findByUserIdAndStartDate(userId, startDate)
                .map(betMonth -> toSummary(betMonth, betHouseRepository.findByUserIdOrderByPositionAsc(userId)));
    }

    /**
     * O lucro total é a SOMA do lucro de cada mês (cada um já somando o resultado de todas as casas).
     * A banca total é o saldo real ATUAL de todas as casas. As unidades das duas (lucro e banca) usam
     * o valor de unidade do mês mais recente (o "mês atual" do ponto de vista do usuário), resolvido na
     * data de referência (endDate se ele já fechou, hoje se ainda está aberto) - dividindo o total em R$
     * numa ÚNICA conta, não somando a unidade já calculada de cada mês (que usa o valor de unidade
     * daquela época) - o valor da unidade muda com o tempo, então somar unidades de meses com unidades
     * diferentes entre si não representa o total em unidades de HOJE. Pedido explícito do usuário.
     */
    @Transactional(readOnly = true)
    public BetOverviewResponse overview() {
        UUID userId = CurrentUser.id();
        List<BetMonth> months = betMonthRepository.findByUserIdOrderByStartDateDescCreatedAtDesc(userId);
        List<BetHouse> houses = betHouseRepository.findByUserIdOrderByPositionAsc(userId);

        BigDecimal totalProfit = BigDecimal.ZERO;
        for (BetMonth m : months) {
            totalProfit = totalProfit.add(computeMonthDays(m, houses).profitLoss());
        }
        BigDecimal totalBanca = totalCurrentBanca(userId);

        BigDecimal currentUnitValue = BigDecimal.ZERO;
        if (!months.isEmpty()) {
            BetMonth latest = months.get(0);
            LocalDate refDate = latest.getEndDate() != null ? latest.getEndDate() : LocalDate.now();
            currentUnitValue = resolveUnitValue(latest, refDate);
        }

        return new BetOverviewResponse(totalProfit, divideForUnits(totalProfit, currentUnitValue),
                totalBanca, divideForUnits(totalBanca, currentUnitValue), currentUnitValue);
    }

    /** Lista todos os dias do mês (do início até hoje+1 ou até o fechamento), com o resultado do dia e por casa. */
    @Transactional(readOnly = true)
    public BetMonthDaysResponse listMonthDays(UUID monthId) {
        UUID userId = CurrentUser.id();
        BetMonth month = betMonthRepository.findByIdAndUserId(monthId, userId)
                .orElseThrow(() -> new AppException("Mês não encontrado.", HttpStatus.NOT_FOUND));
        List<BetHouse> houses = betHouseRepository.findByUserIdOrderByPositionAsc(userId);
        return computeMonthDays(month, houses);
    }

    private BetMonthSummaryResponse toSummary(BetMonth month, List<BetHouse> houses) {
        BetMonthDaysResponse days = computeMonthDays(month, houses);
        return new BetMonthSummaryResponse(days.monthId(), days.startDate(), days.endDate(), days.open(),
                days.startingBanca(), days.endingBanca(), days.profitLoss(), days.profitLossUnits());
    }

    /**
     * Intervalo de dias a mostrar pro mês: se já fechado, do início até o fim. Se ainda não começou
     * (mês futuro escolhido), só o dia 1, como placeholder. Senão, do início até hoje+1 (sempre um dia
     * a mais pronto pra preencher amanhã), sem passar do último dia real do mês.
     */
    private LocalDate resolveRangeEnd(BetMonth month, LocalDate today) {
        if (month.getEndDate() != null) {
            return month.getEndDate();
        }
        if (today.isBefore(month.getStartDate())) {
            return month.getStartDate();
        }
        LocalDate monthLastDay = month.getStartDate().withDayOfMonth(month.getStartDate().lengthOfMonth());
        LocalDate tomorrow = today.plusDays(1);
        return tomorrow.isAfter(monthLastDay) ? monthLastDay : tomorrow;
    }

    private BetMonthDaysResponse computeMonthDays(BetMonth month, List<BetHouse> houses) {
        LocalDate today = LocalDate.now();
        boolean open = month.getEndDate() == null;
        LocalDate rangeEnd = resolveRangeEnd(month, today);

        List<UUID> houseIds = houses.stream().map(BetHouse::getId).toList();
        Map<UUID, String> groupNames = betHouseGroupRepository.findByUserIdOrderByCreatedAtAsc(month.getUser().getId()).stream()
                .collect(Collectors.toMap(BetHouseGroup::getId, BetHouseGroup::getName));

        // saldo de cada casa no instante em que o mês começou - ponto de partida do carry-forward.
        // vem do snapshot (não de "saldo antes da data"), porque duas datas de meses diferentes podem
        // ser o mesmo dia (ver BetMonthStartingBalance). Casas criadas depois do mês começar (sem
        // snapshot) partem de zero, que é o comportamento certo pra elas.
        Map<UUID, BigDecimal> snapshotByHouse = betMonthStartingBalanceRepository.findByMonthId(month.getId()).stream()
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

        // soma do resultado em R$ (não em unidades ainda) de cada casa ao longo do mês inteiro - dias
        // sem resultado contam 0, dias negativos SUBTRAEM normalmente (BigDecimal.add com o sinal certo).
        Map<UUID, BigDecimal> houseTotalResult = new LinkedHashMap<>();
        for (BetHouse h : houses) {
            houseTotalResult.put(h.getId(), BigDecimal.ZERO);
        }
        // mesma soma que houseTotalResult, mas por grupo - só ganha entrada quando alguma casa do
        // grupo aparece num dia, então grupos sem casa nenhuma nunca aparecem no resumo.
        Map<UUID, BigDecimal> groupTotalResult = new LinkedHashMap<>();

        List<BetMonthDayResponse> days = new ArrayList<>();
        for (LocalDate date = month.getStartDate(); !date.isAfter(rangeEnd); date = date.plusDays(1)) {
            BigDecimal unitValue = resolveUnitValue(month, date);
            List<BetMonthDayHouseResponse> houseRows = new ArrayList<>();
            Map<UUID, BigDecimal[]> groupDayTotals = new LinkedHashMap<>(); // groupId -> [closing, opening]
            BigDecimal dayTotalOpening = BigDecimal.ZERO;
            BigDecimal dayTotalClosing = BigDecimal.ZERO;

            for (BetHouse h : houses) {
                BigDecimal defaultOpening = runningBalance.get(h.getId());
                BetDailyBalance entry = entriesByHouse.getOrDefault(h.getId(), Map.of()).get(date);
                BigDecimal closing = entry != null ? entry.getBalance() : defaultOpening;
                BigDecimal opening = entry != null && entry.getOpeningBalance() != null ? entry.getOpeningBalance() : defaultOpening;
                BigDecimal result = closing.subtract(opening);
                BigDecimal resultUnits = divideForUnits(result, unitValue);

                BigDecimal openingOverride = entry != null ? entry.getOpeningBalance() : null;
                UUID groupId = h.getGroup() != null ? h.getGroup().getId() : null;
                String groupName = groupId != null ? groupNames.get(groupId) : null;
                houseRows.add(new BetMonthDayHouseResponse(h.getId(), h.getName(), h.getColor(),
                        closing, divideForUnits(closing, unitValue), result, resultUnits, openingOverride,
                        groupId, groupName));

                dayTotalOpening = dayTotalOpening.add(opening);
                dayTotalClosing = dayTotalClosing.add(closing);
                runningBalance.put(h.getId(), closing); // sempre carrega o saldo FINAL, mesmo se o inicial foi ajustado
                houseTotalResult.merge(h.getId(), result, BigDecimal::add);
                if (groupId != null) {
                    BigDecimal[] acc = groupDayTotals.computeIfAbsent(groupId, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                    acc[0] = acc[0].add(closing);
                    acc[1] = acc[1].add(opening);
                    groupTotalResult.merge(groupId, result, BigDecimal::add);
                }
            }

            List<BetMonthDayGroupResponse> groupRows = groupDayTotals.entrySet().stream()
                    .map(e -> {
                        BigDecimal gClosing = e.getValue()[0];
                        BigDecimal gOpening = e.getValue()[1];
                        BigDecimal gResult = gClosing.subtract(gOpening);
                        return new BetMonthDayGroupResponse(e.getKey(), groupNames.get(e.getKey()),
                                gClosing, divideForUnits(gClosing, unitValue), gResult, divideForUnits(gResult, unitValue));
                    })
                    .toList();

            BigDecimal dayResult = dayTotalClosing.subtract(dayTotalOpening);
            days.add(new BetMonthDayResponse(date, unitValue, dayTotalClosing, divideForUnits(dayTotalClosing, unitValue),
                    dayResult, divideForUnits(dayResult, unitValue), houseRows, groupRows));
        }
        Collections.reverse(days); // mais recente primeiro

        BigDecimal endingBanca = days.isEmpty() ? month.getStartingBanca() : days.get(0).total();
        // o lucro/prejuízo do mês é a SOMA do resultado de todas as casas (mesma fonte do card "Geral
        // do mês, por casa") - não "banca final − banca inicial". Os dois normalmente batem, mas
        // divergem quando tem ajuste manual de saldo inicial (depósito/saque) no meio do mês, que
        // muda a banca real sem contar como resultado - o usuário quer os dois sempre consistentes.
        BigDecimal profitLoss = houseTotalResult.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        // converte pra unidades numa ÚNICA divisão do total em R$ (na taxa mais recente do mês), em vez
        // de somar unidades já arredondadas dia a dia - somar arredondamentos acumula erro (ex: 11 dias
        // cada um arredondado à parte podem fechar em 199,11 quando a divisão do total dá 199,12).
        BigDecimal referenceUnitValue = days.isEmpty() ? month.getInitialUnitValue() : days.get(0).unitValue();
        BigDecimal profitLossUnits = divideForUnits(profitLoss, referenceUnitValue);

        List<BetHouseMonthSummaryResponse> houseSummaries = houses.stream()
                .map(h -> {
                    UUID groupId = h.getGroup() != null ? h.getGroup().getId() : null;
                    return new BetHouseMonthSummaryResponse(h.getId(), h.getName(), h.getColor(),
                            houseTotalResult.get(h.getId()), divideForUnits(houseTotalResult.get(h.getId()), referenceUnitValue),
                            groupId, groupId != null ? groupNames.get(groupId) : null);
                })
                .toList();
        List<BetHouseGroupMonthSummaryResponse> groupSummaries = groupTotalResult.entrySet().stream()
                .map(e -> new BetHouseGroupMonthSummaryResponse(e.getKey(), groupNames.get(e.getKey()),
                        e.getValue(), divideForUnits(e.getValue(), referenceUnitValue)))
                .toList();

        return new BetMonthDaysResponse(month.getId(), month.getStartDate(), month.getEndDate(), open,
                month.getStartingBanca(), endingBanca, profitLoss, profitLossUnits, houseSummaries, groupSummaries, days);
    }

    // ---- Saldo diário ----

    public void updateHouseBalance(UUID monthId, UUID houseId, UpdateBalanceRequest request) {
        BetMonth month = betMonthRepository.findByIdAndUserId(monthId, CurrentUser.id())
                .orElseThrow(() -> new AppException("Mês não encontrado.", HttpStatus.NOT_FOUND));
        BetHouse house = findOwnedHouse(houseId);
        LocalDate date = resolveEditableDate(month, request.date());

        BetDailyBalance entry = betDailyBalanceRepository.findByHouseIdAndDate(house.getId(), date)
                .orElseGet(BetDailyBalance::new);
        entry.setHouse(house);
        entry.setMonth(month);
        entry.setDate(date);
        entry.setBalance(request.balance());
        entry.setOpeningBalance(request.openingBalance());
        betDailyBalanceRepository.save(entry);
    }

    /**
     * Saque/depósito entre uma casa e uma "conta" (outra casa qualquer, escolhida pela pessoa - não
     * é mais fixo numa casa chamada "Banco", porque tem gente com mais de uma conta/banco) - move
     * dinheiro sem contar como resultado de aposta. Os dois lados têm saldo inicial E final ajustados
     * juntos pelo mesmo delta (a casa perde, a conta ganha, ou vice-versa no depósito) - assim o saldo
     * de cada um já reflete a transferência na hora, sem gerar resultado (nem temporariamente, até o
     * resultado real do dia ser lançado depois pelo fluxo normal de editar saldo).
     */
    @Transactional
    public void transferBetweenHouses(UUID monthId, UUID houseId, TransferRequest request) {
        UUID userId = CurrentUser.id();
        BetMonth month = betMonthRepository.findByIdAndUserId(monthId, userId)
                .orElseThrow(() -> new AppException("Mês não encontrado.", HttpStatus.NOT_FOUND));
        BetHouse house = findOwnedHouse(houseId);
        BetHouse counterpart = findOwnedHouse(request.counterpartHouseId());
        if (counterpart.getId().equals(house.getId())) {
            throw new AppException("Escolha uma conta diferente da casa selecionada.", HttpStatus.BAD_REQUEST);
        }
        List<BetHouse> houses = betHouseRepository.findByUserIdOrderByPositionAsc(userId);

        boolean isWithdrawal = "SAQUE".equalsIgnoreCase(request.type());
        boolean isDeposit = "DEPOSITO".equalsIgnoreCase(request.type());
        if (!isWithdrawal && !isDeposit) {
            throw new AppException("Tipo inválido - use SAQUE ou DEPOSITO.", HttpStatus.BAD_REQUEST);
        }

        LocalDate date = resolveEditableDate(month, request.date());
        BigDecimal amount = request.amount();
        BigDecimal houseDelta = isWithdrawal ? amount.negate() : amount;
        BigDecimal counterpartDelta = isWithdrawal ? amount : amount.negate();

        BetMonthDaysResponse computed = computeMonthDays(month, houses);
        BetMonthDayResponse dayData = computed.days().stream()
                .filter(d -> d.date().equals(date))
                .findFirst()
                .orElseThrow(() -> new AppException("Dia fora do intervalo editável desse mês.", HttpStatus.BAD_REQUEST));

        // move o saldo inicial E final juntos, dos dois lados - assim a transferência nunca aparece
        // como resultado (nem temporariamente, antes do resultado real do dia ser lançado depois) e
        // a ordem entre "fazer a transferência" e "lançar o resultado do dia" deixa de importar.
        applyTransferDelta(month, house, date, dayData, houseDelta);
        applyTransferDelta(month, counterpart, date, dayData, counterpartDelta);
    }

    private void applyTransferDelta(BetMonth month, BetHouse house, LocalDate date, BetMonthDayResponse dayData, BigDecimal delta) {
        BetMonthDayHouseResponse h = findHouseInDay(dayData, house.getId());
        BigDecimal opening = h.balance().subtract(h.result());
        BigDecimal closing = h.balance();
        saveBalanceEntry(month, house, date, closing.add(delta), opening.add(delta));
    }

    private BetMonthDayHouseResponse findHouseInDay(BetMonthDayResponse dayData, UUID houseId) {
        return dayData.houses().stream()
                .filter(h -> h.houseId().equals(houseId))
                .findFirst()
                .orElseThrow(() -> new AppException("Casa não encontrada nesse dia.", HttpStatus.BAD_REQUEST));
    }

    private void saveBalanceEntry(BetMonth month, BetHouse house, LocalDate date, BigDecimal balance, BigDecimal openingOverride) {
        BetDailyBalance entry = betDailyBalanceRepository.findByHouseIdAndDate(house.getId(), date)
                .orElseGet(BetDailyBalance::new);
        entry.setHouse(house);
        entry.setMonth(month);
        entry.setDate(date);
        entry.setBalance(balance);
        entry.setOpeningBalance(openingOverride);
        betDailyBalanceRepository.save(entry);
    }

    /** Valida e resolve a data (null = hoje) contra o intervalo editável desse mês (aberto ou fechado, nunca no futuro). */
    private LocalDate resolveEditableDate(BetMonth month, LocalDate requestedDate) {
        LocalDate today = LocalDate.now();
        LocalDate date = requestedDate != null ? requestedDate : today;
        LocalDate monthLastDay = month.getStartDate().withDayOfMonth(month.getStartDate().lengthOfMonth());
        LocalDate maxEditable = monthLastDay.isBefore(today) ? monthLastDay : today;
        if (date.isBefore(month.getStartDate()) || date.isAfter(maxEditable)) {
            throw new AppException("Data fora do intervalo editável desse mês.", HttpStatus.BAD_REQUEST);
        }
        return date;
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
        BetHouseGroup group = h.getGroup();
        return new BetHouseResponse(h.getId(), h.getName(), h.getColor(), h.getPosition(),
                group != null ? group.getId() : null, group != null ? group.getName() : null);
    }
}
