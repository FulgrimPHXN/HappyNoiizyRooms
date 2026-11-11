package org.diceordieass.rooms.service;

import lombok.RequiredArgsConstructor;
import org.diceordieass.rooms.dto.RoomRequestDto;
import org.diceordieass.rooms.entity.Room;
import org.diceordieass.rooms.entity.RoomRole;
import org.diceordieass.rooms.exception.InvalidInviteCodeException;
import org.diceordieass.rooms.exception.PlayerAlreadyJoinedException;
import org.diceordieass.rooms.exception.PlayerNotInRoomException;
import org.diceordieass.rooms.exception.RoomAlreadyExistsException;
import org.diceordieass.rooms.exception.RoomAccessDeniedException;
import org.diceordieass.rooms.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;

    @Transactional
    public Room createRoom(UUID ownerId, RoomRequestDto roomRequestDto) {
        roomRepository.findRoomByName(roomRequestDto.roomName()).ifPresent(existing -> {
            throw new RoomAlreadyExistsException(roomRequestDto.roomName());
        });
        return roomRepository.save(new Room(roomRequestDto.roomName(), ownerId));
    }

    public Room getRoom(UUID roomId, UUID callerId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException("Комната не найдена"));
        if (!room.getPlayers().containsKey(callerId)) {
            throw new RoomAccessDeniedException();
        }
        return room;
    }

    public List<Room> getOwnerRooms(UUID ownerId) {
        return roomRepository.findRoomByOwnerId(ownerId);
    }

    @Transactional
    public Room updateRoom(UUID roomId, UUID callerId, RoomRequestDto dto) {
        Room room = checkRoomOwner(roomId, callerId);
        room.rename(dto.roomName());
        return room;
    }

    @Transactional
    public void deleteRoom(UUID roomId, UUID callerId) {
        roomRepository.delete(checkRoomOwner(roomId, callerId));
    }

    @Transactional
    public Room joinRoom(UUID roomId, UUID callerId, String inviteCode) {
        Room room = roomRepository.findRoomById(roomId)
                .orElseThrow(() -> new NoSuchElementException("Комната не найдена"));
        if (!room.getInviteCode().equals(inviteCode)) {
            throw new InvalidInviteCodeException();
        }
        if (room.getPlayers().containsKey(callerId)) {
            throw new PlayerAlreadyJoinedException();
        }
        room.addPlayer(callerId, RoomRole.PLAYER);
        return room;
    }

    @Transactional
    public Room leaveRoom(UUID roomId, UUID callerId) {
        Room room = roomRepository.findRoomById(roomId)
                .orElseThrow(() -> new NoSuchElementException("Комната не найдена"));
        if (!room.getPlayers().containsKey(callerId)) {
            throw new PlayerNotInRoomException();
        }
        if (room.getOwnerId().equals(callerId)) {
            throw new RoomAccessDeniedException();
        }
        room.removePlayer(callerId);
        return room;
    }

    private Room checkRoomOwner (UUID roomId, UUID callerId) {
        Room room =  roomRepository.findRoomById((roomId))
                .orElseThrow(NoSuchElementException::new);
        if (!room.getOwnerId().equals(callerId)) {
            throw new RoomAccessDeniedException();
        }
        return room;
    }
}
