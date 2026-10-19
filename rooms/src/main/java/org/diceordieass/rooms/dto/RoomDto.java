package org.diceordieass.rooms.dto;

import org.diceordieass.rooms.entity.Room;

import java.util.UUID;

public record RoomDto(UUID roomId,String roomName, String inviteCode) {
    public static RoomDto from(Room room) {
        return new RoomDto(room.getId(), room.getName(), room.getInviteCode());
    }
}
