package org.diceordieass.rooms.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Room {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "name")
    private String name;

    // Отдельный секрет позволяет отозвать приглашение без смены ID комнаты.
    @Column(name = "invite_code", unique = true)
    private String inviteCode;

    // Участие принадлежит комнате и хранится вместе с ней, без отдельной сущности пользователя.
    @ElementCollection
    @CollectionTable(name = "room_players", joinColumns = @JoinColumn(name = "room_id"))
    @MapKeyColumn(name = "user_id")
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Map<UUID, RoomRole> players = new HashMap<>();

    public Room (String name, UUID ownerId) {
        this.id = UuidCreator.getTimeOrderedEpoch();
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
        this.inviteCode = generateInviteCode();
        players.put(ownerId, RoomRole.GM);
    }

    public void regenerateInviteCode() {
        this.inviteCode = generateInviteCode();
    }

    public void rename(String newName) {
        this.name = Objects.requireNonNull(newName, "name must not be null");
    }

    public Map<UUID, RoomRole> getPlayers() {
        // Изменения состава проходят только через методы комнаты.
        return Collections.unmodifiableMap(players);
    }

    public void addPlayer(UUID userId, RoomRole role) {
        players.put(Objects.requireNonNull(userId, "userId must not be null"),
                Objects.requireNonNull(role, "role must not be null"));
    }

    public void removePlayer(UUID userId) {
        players.remove(userId);
    }

    private static String generateInviteCode() {
        byte[] randomBytes = new byte[16];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room other)) {
            return false;
        }
        return id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
