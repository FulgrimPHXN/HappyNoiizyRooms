package org.diceordieass.users.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User {

    @Id
    private UUID id;

    /**
     * Оптимистическая блокировка JPA/Hibernate: при каждом обновлении строки версия увеличивается,
     * и если две транзакции одновременно читают одну и ту же сущность и обе пытаются её сохранить,
     * вторая получит {@code OptimisticLockException} вместо тихой потери первой правки.
     * <p>
     * Тип — ссылочный {@code Long}, не примитив {@code long}, потому что по нему же Spring Data
     * определяет, новая сущность или уже сохранённая: раз {@code id} у нас никогда не {@code null}
     * (генерируется в конструкторе), отличить "новая/старая" можно только по {@code version == null}.
     * Без этого перед каждой вставкой Hibernate делал бы лишний разведочный {@code SELECT}.
     */
    @Version
    private Long version;

    @Column(name = "user_name", unique = true)
    private String userName;

    @Column(name = "password_hash")
    private String passwordHash;

    /**
     * {@code id} генерируется здесь, в приложении, а не через {@code @GeneratedValue}/на стороне БД:
     * <ul>
     *     <li>нужен именно UUIDv7 (упорядоченный по времени), а не случайный v4, который обычно даёт БД/Hibernate;</li>
     *     <li>id должен быть готов сразу в памяти, до вставки в БД — без лишнего round-trip за сгенерированным значением;</li>
     *     <li>не хочется зависеть от конкретного расширения конкретной СУБД ради генерации UUID;</li>
     *     <li>признак "новая сущность" у Spring Data строится на {@code id == null} вместе с {@code @Version} — для этого id обязан существовать ещё до сохранения.</li>
     * </ul>
     */
    public User(String userName, String passwordHash) {
        this.id = UuidCreator.getTimeOrderedEpoch();//Статический метод из библиотеки uuid-creator, генерирует новый UUID
        this.userName = Objects.requireNonNull(userName, "userName must not be null"); //Проверяет, что переданный параметр userName не null. Если он null — тут же бросает NullPointerException
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
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
