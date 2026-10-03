package org.diceordieass.users.exception;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String userName) {
        super("Пользователь с именем \"" + userName + "\" уже существует");
    }
}
