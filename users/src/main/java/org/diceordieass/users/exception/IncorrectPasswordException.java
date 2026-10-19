package org.diceordieass.users.exception;

public class IncorrectPasswordException extends RuntimeException {


    public IncorrectPasswordException(String message) {
        super(message);
    }
}
