package org.diceordieass.users.dto;

import org.diceordieass.users.entity.User;

import java.util.UUID;

public record UserResponse(UUID id, String userName) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUserName());
    }
}
