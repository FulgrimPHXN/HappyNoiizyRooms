package org.diceordieass.rooms.exception;

public class PlayerAlreadyJoinedException extends RuntimeException {
    public PlayerAlreadyJoinedException() {
        super("Вы уже участник этой комнаты");
    }
}
