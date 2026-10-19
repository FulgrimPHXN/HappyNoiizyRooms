package org.diceordieass.users.dto;

import org.diceordieass.users.entity.User;
import org.diceordieass.users.security.UserRole;

import java.util.UUID;

public record UserResponse(UUID id, String userName, UserRole role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUserName(), user.getRole());
    }
}
