package org.diceordieass.rooms.exception;

public class PlayerNotInRoomException extends RuntimeException {
    public PlayerNotInRoomException() {
        super("Вы не участник этой комнаты");
    }
}
