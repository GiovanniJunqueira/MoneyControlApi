package com.financeiro.api.controller;

import com.financeiro.api.dto.bets.BetFriendMonthDetailResponse;
import com.financeiro.api.dto.bets.FriendCodeResponse;
import com.financeiro.api.dto.bets.FriendRequestResponse;
import com.financeiro.api.dto.bets.FriendResponse;
import com.financeiro.api.dto.bets.SendFriendRequestRequest;
import com.financeiro.api.dto.bets.SendFriendRequestResponse;
import com.financeiro.api.service.BetFriendService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bets/friends")
public class BetFriendController {

    private final BetFriendService betFriendService;

    public BetFriendController(BetFriendService betFriendService) {
        this.betFriendService = betFriendService;
    }

    @GetMapping("/me")
    public FriendCodeResponse myCode() {
        return betFriendService.myCode();
    }

    @GetMapping
    public List<FriendResponse> listFriends() {
        return betFriendService.listFriends();
    }

    @DeleteMapping("/{friendUserId}")
    public ResponseEntity<Void> removeFriend(@PathVariable UUID friendUserId) {
        betFriendService.removeFriend(friendUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/requests")
    public List<FriendRequestResponse> listReceivedRequests() {
        return betFriendService.listReceivedRequests();
    }

    @PostMapping("/requests")
    public SendFriendRequestResponse sendRequest(@Valid @RequestBody SendFriendRequestRequest request) {
        return betFriendService.sendRequest(request);
    }

    @PostMapping("/requests/{id}/accept")
    public ResponseEntity<Void> acceptRequest(@PathVariable UUID id) {
        betFriendService.acceptRequest(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{id}/decline")
    public ResponseEntity<Void> declineRequest(@PathVariable UUID id) {
        betFriendService.declineRequest(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{friendUserId}/current-month")
    public BetFriendMonthDetailResponse friendCurrentMonth(@PathVariable UUID friendUserId) {
        return betFriendService.friendCurrentMonth(friendUserId);
    }
}
