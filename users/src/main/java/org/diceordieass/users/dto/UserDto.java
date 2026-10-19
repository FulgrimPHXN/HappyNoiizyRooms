package org.diceordieass.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserDto(
        @NotBlank @Size(max = 30) String userName,
        @NotBlank @Size(max = 50) String rawPassword) {
}
