package org.diceordieass.rooms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomRequestDto (@NotBlank @Size(max = 60) String roomName){
}
