package org.diceordieass.rooms.entity;


import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.util.Base64;
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

    //секрет для ссылки-приглашения — намеренно не id: id светится в каждом обычном
    //запросе на просмотр/правку комнаты, а inviteCode должен быть отзываемым отдельно
    //от самой комнаты (см. regenerateInviteCode)
    @Column(name = "invite_code", unique = true)
    private String inviteCode;

    public Room (String name, UUID ownerId) {
        this.id = UuidCreator.getTimeOrderedEpoch();
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
        this.inviteCode = generateInviteCode();
    }

    //отозвать старую ссылку-приглашение, не трогая саму комнату — старая ссылка
    //сразу перестаёт работать, новую отдаёт вызывающий код (из возвращаемого значения
    //геттера после вызова), это не отдельная сущность с историей, а просто смена секрета
    public void regenerateInviteCode() {
        this.inviteCode = generateInviteCode();
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
