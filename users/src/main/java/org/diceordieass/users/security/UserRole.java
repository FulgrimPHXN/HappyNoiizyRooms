package org.diceordieass.users.security;

import lombok.Getter;

@Getter
public enum UserRole {

    ROLE_ADMIN("Администратор"),
    ROLE_USER("Пользователь");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }
}
