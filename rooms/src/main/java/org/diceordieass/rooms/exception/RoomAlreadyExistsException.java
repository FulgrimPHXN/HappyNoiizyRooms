package org.diceordieass.rooms.exception;

public class RoomAlreadyExistsException extends RuntimeException {
    public RoomAlreadyExistsException(String roomName) {
        super("Комната с названием \"" + roomName + "\" уже существует");
    }
}
