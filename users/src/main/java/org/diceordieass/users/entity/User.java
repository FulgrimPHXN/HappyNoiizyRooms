package org.diceordieass.users.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.diceordieass.users.security.UserRole;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(name = "user_role")
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(name = "user_name", unique = true)
    private String userName;

    @Column(name = "password_hash")
    private String passwordHash;

    public User(String userName, String passwordHash) {
        this.id = UuidCreator.getTimeOrderedEpoch();
        this.userName = Objects.requireNonNull(userName, "userName must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.role = UserRole.ROLE_USER;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash, "newPasswordHash must not be null");
    }

    public void rename(String newUserName) {
        this.userName = Objects.requireNonNull(newUserName, "newUserName must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) {
            return false;
        }
        return id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
