package com.financeiro.api.service;

import com.financeiro.api.dto.bets.BetFriendMonthDetailResponse;
import com.financeiro.api.dto.bets.BetMonthSummaryResponse;
import com.financeiro.api.dto.bets.FriendCodeResponse;
import com.financeiro.api.dto.bets.FriendRequestResponse;
import com.financeiro.api.dto.bets.FriendResponse;
import com.financeiro.api.dto.bets.SendFriendRequestRequest;
import com.financeiro.api.dto.bets.SendFriendRequestResponse;
import com.financeiro.api.entity.BetFriendRequest;
import com.financeiro.api.entity.BetFriendship;
import com.financeiro.api.entity.User;
import com.financeiro.api.exception.AppException;
import com.financeiro.api.repository.BetFriendRequestRepository;
import com.financeiro.api.repository.BetFriendshipRepository;
import com.financeiro.api.repository.UserRepository;
import com.financeiro.api.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/**
 * Amigos do módulo Bets - permite ver o resultado mensal por casa de quem aceitou seu convite.
 * Totalmente separado de Competição (que compara várias pessoas por período) - aqui é só uma
 * relação bilateral entre duas pessoas, sem prazo nem mês associado.
 */
@Service
public class BetFriendService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sem O/0/I/1
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final BetFriendRequestRepository betFriendRequestRepository;
    private final BetFriendshipRepository betFriendshipRepository;
    private final BetService betService;

    public BetFriendService(UserRepository userRepository, BetFriendRequestRepository betFriendRequestRepository,
                             BetFriendshipRepository betFriendshipRepository, BetService betService) {
        this.userRepository = userRepository;
        this.betFriendRequestRepository = betFriendRequestRepository;
        this.betFriendshipRepository = betFriendshipRepository;
        this.betService = betService;
    }

    /** Gera o código na primeira vez que a pessoa pede (não no registro) - a maioria nunca vai usar
     * essa feature, então não faz sentido gastar um código único por conta criada. */
    @Transactional
    public FriendCodeResponse myCode() {
        User user = userRepository.getReferenceById(CurrentUser.id());
        if (user.getFriendCode() == null) {
            user.setFriendCode(generateUniqueCode());
            userRepository.save(user);
        }
        return new FriendCodeResponse(user.getFriendCode());
    }

    @Transactional
    public SendFriendRequestResponse sendRequest(SendFriendRequestRequest request) {
        UUID userId = CurrentUser.id();
        User me = userRepository.getReferenceById(userId);
        User target = userRepository.findByFriendCode(request.code().trim().toUpperCase())
                .orElseThrow(() -> new AppException("Código não encontrado.", HttpStatus.NOT_FOUND));

        if (target.getId().equals(userId)) {
            throw new AppException("Você não pode adicionar a si mesmo.", HttpStatus.BAD_REQUEST);
        }
        if (areFriends(userId, target.getId())) {
            throw new AppException("Vocês já são amigos.", HttpStatus.BAD_REQUEST);
        }

        // a outra pessoa já te convidou antes - passar o código dela agora é "adicionar de volta",
        // então em vez de criar outro convite, essa chamada já fecha a amizade dos dois lados.
        var reverse = betFriendRequestRepository.findBySenderIdAndReceiverId(target.getId(), userId);
        if (reverse.isPresent()) {
            betFriendRequestRepository.delete(reverse.get());
            createFriendship(me, target);
            return new SendFriendRequestResponse(true, target.getName());
        }

        if (betFriendRequestRepository.existsBySenderIdAndReceiverId(userId, target.getId())) {
            throw new AppException("Convite já enviado - aguarde a pessoa aceitar.", HttpStatus.BAD_REQUEST);
        }

        BetFriendRequest fr = new BetFriendRequest();
        fr.setSender(me);
        fr.setReceiver(target);
        betFriendRequestRepository.save(fr);
        return new SendFriendRequestResponse(false, target.getName());
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> listReceivedRequests() {
        return betFriendRequestRepository.findByReceiverIdOrderByCreatedAtDesc(CurrentUser.id()).stream()
                .map(fr -> new FriendRequestResponse(fr.getId(), fr.getSender().getId(), fr.getSender().getName(), fr.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void acceptRequest(UUID requestId) {
        UUID userId = CurrentUser.id();
        BetFriendRequest fr = betFriendRequestRepository.findByIdAndReceiverId(requestId, userId)
                .orElseThrow(() -> new AppException("Convite não encontrado.", HttpStatus.NOT_FOUND));
        createFriendship(fr.getSender(), fr.getReceiver());
        betFriendRequestRepository.delete(fr);
    }

    @Transactional
    public void declineRequest(UUID requestId) {
        UUID userId = CurrentUser.id();
        BetFriendRequest fr = betFriendRequestRepository.findByIdAndReceiverId(requestId, userId)
                .orElseThrow(() -> new AppException("Convite não encontrado.", HttpStatus.NOT_FOUND));
        betFriendRequestRepository.delete(fr);
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> listFriends() {
        UUID userId = CurrentUser.id();
        return betFriendshipRepository.findByUserOneIdOrUserTwoId(userId, userId).stream()
                .map(f -> f.getUserOne().getId().equals(userId) ? f.getUserTwo() : f.getUserOne())
                .map(u -> new FriendResponse(u.getId(), u.getName()))
                .toList();
    }

    @Transactional
    public void removeFriend(UUID friendUserId) {
        UUID userId = CurrentUser.id();
        UUID low = userId.compareTo(friendUserId) < 0 ? userId : friendUserId;
        UUID high = userId.compareTo(friendUserId) < 0 ? friendUserId : userId;
        BetFriendship friendship = betFriendshipRepository.findByUserOneIdAndUserTwoId(low, high)
                .orElseThrow(() -> new AppException("Vocês não são amigos.", HttpStatus.NOT_FOUND));
        betFriendshipRepository.delete(friendship);
    }

    @Transactional(readOnly = true)
    public List<BetMonthSummaryResponse> friendMonths(UUID friendUserId) {
        requireFriends(friendUserId);
        return betService.monthsForUser(friendUserId);
    }

    @Transactional(readOnly = true)
    public BetFriendMonthDetailResponse friendMonthDetail(UUID friendUserId, UUID monthId) {
        requireFriends(friendUserId);
        return betService.monthDetailForUser(friendUserId, monthId);
    }

    private void requireFriends(UUID friendUserId) {
        if (!areFriends(CurrentUser.id(), friendUserId)) {
            throw new AppException("Vocês não são amigos.", HttpStatus.NOT_FOUND);
        }
    }

    private boolean areFriends(UUID userId, UUID otherId) {
        UUID low = userId.compareTo(otherId) < 0 ? userId : otherId;
        UUID high = userId.compareTo(otherId) < 0 ? otherId : userId;
        return betFriendshipRepository.existsByUserOneIdAndUserTwoId(low, high);
    }

    private void createFriendship(User a, User b) {
        boolean aIsLower = a.getId().compareTo(b.getId()) < 0;
        BetFriendship friendship = new BetFriendship();
        friendship.setUserOne(aIsLower ? a : b);
        friendship.setUserTwo(aIsLower ? b : a);
        betFriendshipRepository.save(friendship);
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            code = sb.toString();
        } while (userRepository.existsByFriendCode(code));
        return code;
    }
}
