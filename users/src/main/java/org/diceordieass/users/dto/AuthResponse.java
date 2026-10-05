package org.diceordieass.users.dto;

import org.diceordieass.users.entity.User;
import org.diceordieass.users.security.UserRole;

import java.util.UUID;

public record AuthResponse(UUID id, String userName, UserRole role, String token) {

    public static AuthResponse from(User user, String token) {
        return new AuthResponse(user.getId(), user.getUserName(),  user.getRole(), token);
    }
}
