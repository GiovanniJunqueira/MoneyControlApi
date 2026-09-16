package com.financeiro.api.service;

import com.financeiro.api.dto.bets.BetCompetitionRankingEntryResponse;
import com.financeiro.api.dto.bets.BetCompetitionRankingResponse;
import com.financeiro.api.dto.bets.BetCompetitionResponse;
import com.financeiro.api.dto.bets.BetMonthSummaryResponse;
import com.financeiro.api.dto.bets.CreateCompetitionRequest;
import com.financeiro.api.dto.bets.JoinCompetitionRequest;
import com.financeiro.api.entity.BetCompetition;
import com.financeiro.api.entity.BetCompetitionMember;
import com.financeiro.api.entity.User;
import com.financeiro.api.exception.AppException;
import com.financeiro.api.repository.BetCompetitionMemberRepository;
import com.financeiro.api.repository.BetCompetitionRepository;
import com.financeiro.api.repository.UserRepository;
import com.financeiro.api.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BetCompetitionService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sem O/0/I/1 pra evitar confusão
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BetCompetitionRepository betCompetitionRepository;
    private final BetCompetitionMemberRepository betCompetitionMemberRepository;
    private final UserRepository userRepository;
    private final BetService betService;

    public BetCompetitionService(BetCompetitionRepository betCompetitionRepository,
                                  BetCompetitionMemberRepository betCompetitionMemberRepository,
                                  UserRepository userRepository, BetService betService) {
        this.betCompetitionRepository = betCompetitionRepository;
        this.betCompetitionMemberRepository = betCompetitionMemberRepository;
        this.userRepository = userRepository;
        this.betService = betService;
    }

    @Transactional
    public BetCompetitionResponse create(CreateCompetitionRequest request) {
        UUID userId = CurrentUser.id();
        User creator = userRepository.getReferenceById(userId);

        BetCompetition competition = new BetCompetition();
        competition.setCreator(creator);
        competition.setName(request.name());
        competition.setYear(request.year());
        competition.setMonth(request.month());
        competition.setCode(generateUniqueCode());
        betCompetitionRepository.save(competition);

        BetCompetitionMember member = new BetCompetitionMember();
        member.setCompetition(competition);
        member.setUser(creator);
        betCompetitionMemberRepository.save(member);

        return toResponse(competition, userId);
    }

    @Transactional
    public BetCompetitionResponse join(JoinCompetitionRequest request) {
        UUID userId = CurrentUser.id();
        BetCompetition competition = betCompetitionRepository.findByCode(request.code().trim().toUpperCase())
                .orElseThrow(() -> new AppException("Código não encontrado.", HttpStatus.NOT_FOUND));

        if (betCompetitionMemberRepository.existsByCompetitionIdAndUserId(competition.getId(), userId)) {
            throw new AppException("Você já está nessa competição.", HttpStatus.BAD_REQUEST);
        }

        BetCompetitionMember member = new BetCompetitionMember();
        member.setCompetition(competition);
        member.setUser(userRepository.getReferenceById(userId));
        betCompetitionMemberRepository.save(member);

        return toResponse(competition, userId);
    }

    @Transactional(readOnly = true)
    public List<BetCompetitionResponse> listMine() {
        UUID userId = CurrentUser.id();
        return betCompetitionMemberRepository.findByUserIdOrderByJoinedAtDesc(userId).stream()
                .map(m -> toResponse(m.getCompetition(), userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public BetCompetitionRankingResponse ranking(UUID competitionId) {
        UUID userId = CurrentUser.id();
        BetCompetition competition = betCompetitionRepository.findById(competitionId)
                .orElseThrow(() -> new AppException("Competição não encontrada.", HttpStatus.NOT_FOUND));
        if (!betCompetitionMemberRepository.existsByCompetitionIdAndUserId(competitionId, userId)) {
            throw new AppException("Competição não encontrada.", HttpStatus.NOT_FOUND);
        }

        List<BetCompetitionMember> members = betCompetitionMemberRepository.findByCompetitionId(competitionId);

        List<BetCompetitionRankingEntryResponse> entries = members.stream()
                .map(member -> {
                    User memberUser = member.getUser();
                    Optional<BetMonthSummaryResponse> summary = betService
                            .summaryForUserAndPeriod(memberUser.getId(), competition.getYear(), competition.getMonth());
                    BigDecimal profitLoss = summary.map(BetMonthSummaryResponse::profitLoss).orElse(BigDecimal.ZERO);
                    BigDecimal profitLossUnits = summary.map(BetMonthSummaryResponse::profitLossUnits).orElse(BigDecimal.ZERO);
                    return new BetCompetitionRankingEntryResponse(0, memberUser.getId(), memberUser.getName(),
                            memberUser.getId().equals(userId), summary.isPresent(), profitLoss, profitLossUnits);
                })
                .sorted(Comparator.comparing(BetCompetitionRankingEntryResponse::profitLossUnits).reversed())
                .toList();

        List<BetCompetitionRankingEntryResponse> ranked = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            BetCompetitionRankingEntryResponse e = entries.get(i);
            ranked.add(new BetCompetitionRankingEntryResponse(i + 1, e.userId(), e.userName(), e.isYou(), e.hasData(),
                    e.profitLoss(), e.profitLossUnits()));
        }

        return new BetCompetitionRankingResponse(competition.getId(), competition.getName(), competition.getCode(),
                competition.getYear(), competition.getMonth(), ranked);
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            code = sb.toString();
        } while (betCompetitionRepository.existsByCode(code));
        return code;
    }

    private BetCompetitionResponse toResponse(BetCompetition c, UUID userId) {
        return new BetCompetitionResponse(c.getId(), c.getName(), c.getCode(), c.getYear(), c.getMonth(),
                c.getCreator().getId().equals(userId));
    }
}
