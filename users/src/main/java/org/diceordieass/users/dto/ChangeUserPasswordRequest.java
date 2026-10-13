package org.diceordieass.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeUserPasswordRequest(@NotBlank @Size(max = 50) String oldPassword,
                                        @NotBlank @Size(max = 50) String newPassword) {
}
