package org.diceordieass.rooms.service;

import lombok.RequiredArgsConstructor;
import org.diceordieass.rooms.dto.RoomRequestDto;
import org.diceordieass.rooms.entity.Room;
import org.diceordieass.rooms.exception.RoomAlreadyExistsException;
import org.diceordieass.rooms.repository.RoomRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    public Room createRoom(UUID ownerId, RoomRequestDto roomRequestDto) {
        roomRepository.findRoomByName(roomRequestDto.roomName()).ifPresent(existing -> {
            throw new RoomAlreadyExistsException(roomRequestDto.roomName());
        });
        return roomRepository.save(new Room(roomRequestDto.roomName(), ownerId));
    }

    public Room getRoom(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException("Комната не найдена"));
    }

    public List<Room> getOwnerRooms(UUID ownerId) {
        return roomRepository.findRoomByOwnerId(ownerId);
    }

    public Room updateRoom(UUID roomId, UUID callerId, RoomRequestDto dto) {
        Room room = checkRoomOwner(roomId, callerId);
        room.rename(dto.roomName());
        return roomRepository.save(room);
    }

    public void deleteRoom(UUID roomId, UUID callerId) {
        roomRepository.deleteById((checkRoomOwner(roomId, callerId)).getId());
    }

    private Room checkRoomOwner (UUID roomId, UUID callerId) {
        Room room =  roomRepository.findRoomById((roomId))
                .orElseThrow(NoSuchElementException::new);
        return Optional.of(room)
                .filter(r -> r.getOwnerId().equals(callerId))
                .orElseThrow(NoSuchElementException::new);
    }
}
