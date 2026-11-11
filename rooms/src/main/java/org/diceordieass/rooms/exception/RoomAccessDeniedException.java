package org.diceordieass.rooms.exception;

public class RoomAccessDeniedException extends RuntimeException {
    public RoomAccessDeniedException() {
        super("Недостаточно прав для этой операции с комнатой");
    }
}
