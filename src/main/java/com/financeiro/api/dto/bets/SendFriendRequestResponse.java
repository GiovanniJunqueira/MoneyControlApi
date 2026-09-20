package com.financeiro.api.dto.bets;

/** friended=true quando isso completou uma amizade mútua na hora (a outra pessoa já tinha te
 * convidado antes); false quando só criou um convite pendente aguardando o outro lado aceitar. */
public record SendFriendRequestResponse(boolean friended, String otherUserName) {
}
