package org.diceordieass.rooms.repository;

import org.diceordieass.rooms.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    Optional<Room> findRoomByName(String name);

    List<Room> findRoomByOwnerId(UUID ownerId);

    Optional<Room> findRoomById(UUID id);
}
