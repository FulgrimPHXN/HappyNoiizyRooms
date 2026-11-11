package org.diceordieass.rooms.exception;

public class InvalidInviteCodeException extends RuntimeException {
    public InvalidInviteCodeException() {
        super("Неверный код приглашения");
    }
}
