package org.diceordieass.rooms.repository;

import org.diceordieass.rooms.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    @Override
    @EntityGraph(attributePaths = "players")
    Optional<Room> findById(UUID id);

    Optional<Room> findRoomByName(String name);

    @EntityGraph(attributePaths = "players")
    List<Room> findRoomByOwnerId(UUID ownerId);

    @EntityGraph(attributePaths = "players")
    Optional<Room> findRoomById(UUID id);
}
