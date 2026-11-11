package org.diceordieass.rooms.service;

import org.diceordieass.rooms.dto.RoomRequestDto;
import org.diceordieass.rooms.dto.RoomDto;
import org.diceordieass.rooms.entity.Room;
import org.diceordieass.rooms.entity.RoomRole;
import org.diceordieass.rooms.exception.*;
import org.diceordieass.rooms.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:rooms;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class RoomServiceTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();
    private Room room;

    @BeforeEach
    void setUp() {
        roomRepository.deleteAll();
        room = roomService.createRoom(ownerId, new RoomRequestDto("Room"));
    }

    @Test
    void createRoom_ownerBecomesGm() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Room saved = roomRepository.findById(room.getId()).orElseThrow();
            assertThat(saved.getPlayers()).containsEntry(ownerId, RoomRole.GM);
        });
    }

    @Test
    void ownerList_canMapParticipantsOutsideTransaction() {
        assertThat(RoomDto.from(roomService.getOwnerRooms(ownerId).getFirst()).roomId())
                .isEqualTo(room.getId());
    }

    @Test
    void joinAndLeave_validInvite_persistsMembership() {
        roomService.joinRoom(room.getId(), playerId, room.getInviteCode());
        assertThat(roomService.getRoom(room.getId(), playerId).getId()).isEqualTo(room.getId());
        roomService.leaveRoom(room.getId(), playerId);
        assertThatThrownBy(() -> roomService.getRoom(room.getId(), playerId))
                .isInstanceOf(RoomAccessDeniedException.class);
    }

    @Test
    void joinRoom_invalidInvite_rejectsPlayer() {
        assertThatThrownBy(() -> roomService.joinRoom(room.getId(), playerId, "wrong"))
                .isInstanceOf(InvalidInviteCodeException.class);
        assertThatThrownBy(() -> roomService.getRoom(room.getId(), playerId))
                .isInstanceOf(RoomAccessDeniedException.class);
    }

    @Test
    void joinRoom_alreadyJoined_rejectsDuplicate() {
        roomService.joinRoom(room.getId(), playerId, room.getInviteCode());
        assertThatThrownBy(() -> roomService.joinRoom(room.getId(), playerId, room.getInviteCode()))
                .isInstanceOf(PlayerAlreadyJoinedException.class);
    }

    @Test
    void leaveRoom_outsiderOrOwner_rejectsOperation() {
        assertThatThrownBy(() -> roomService.leaveRoom(room.getId(), playerId))
                .isInstanceOf(PlayerNotInRoomException.class);
        assertThatThrownBy(() -> roomService.leaveRoom(room.getId(), ownerId))
                .isInstanceOf(RoomAccessDeniedException.class);
    }

    @Test
    void updateAndDelete_outsider_rejectsOperation() {
        assertThatThrownBy(() -> roomService.updateRoom(room.getId(), playerId, new RoomRequestDto("Other")))
                .isInstanceOf(RoomAccessDeniedException.class);
        assertThatThrownBy(() -> roomService.deleteRoom(room.getId(), playerId))
                .isInstanceOf(RoomAccessDeniedException.class);
        assertThat(roomService.getRoom(room.getId(), ownerId).getName()).isEqualTo("Room");
    }

    @Test
    void updateRoom_owner_persistsName() {
        roomService.updateRoom(room.getId(), ownerId, new RoomRequestDto("Other"));
        assertThat(roomService.getRoom(room.getId(), ownerId).getName()).isEqualTo("Other");
    }

    @Test
    void deleteRoom_withPlayers_deletesMembership() {
        roomService.joinRoom(room.getId(), playerId, room.getInviteCode());
        roomService.deleteRoom(room.getId(), ownerId);
        assertThat(roomRepository.findById(room.getId())).isEmpty();
    }

    @Test
    void concurrentJoin_staleTransaction_rollsBack() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        TransactionTemplate concurrent = new TransactionTemplate(transactionManager);
        concurrent.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        UUID otherPlayerId = UUID.randomUUID();

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            Room stale = roomRepository.findById(room.getId()).orElseThrow();
            stale.getPlayers().size();
            concurrent.executeWithoutResult(other ->
                    roomService.joinRoom(room.getId(), playerId, room.getInviteCode()));
            stale.addPlayer(otherPlayerId, RoomRole.PLAYER);
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        transaction.executeWithoutResult(status -> {
            Room saved = roomRepository.findById(room.getId()).orElseThrow();
            assertThat(saved.getPlayers()).containsKey(playerId).doesNotContainKey(otherPlayerId);
            assertThat(saved.getVersion()).isGreaterThan(room.getVersion());
        });
    }
}
