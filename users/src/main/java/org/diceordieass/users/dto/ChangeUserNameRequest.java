package org.diceordieass.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeUserNameRequest(@NotBlank @Size(max = 30) String userName) {
}
