package org.diceordieass.users.service;

import org.diceordieass.users.entity.RefreshToken;
import org.diceordieass.users.exception.InvalidRefreshTokenException;
import org.diceordieass.users.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final long TEST_TTL_MS = 3_600_000L;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setup() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, TEST_TTL_MS);
    }

    /*мы тестируем метод ротации нашего токена, его "happy path", когда все идет валидно, сервер должен:
    убедиться, что токен реальный (есть в базе), убедиться, что он не украден,
    (не был уже использован раньше - иначе это признак того, что токен скомпрометирован);
    убедиться, что он не просрочен; если всё ок - "сжечь" старый токен (отозвать) и отдать userId,
    чтобы вызывающий код (контроллер) мог сгенерировать новые токены для этого пользователя.
     */
    @Test
    //Проверка контракта метода rotate для одного конкретного сценария входных данных ("токен существует,
    //не отозван, не просрочен"). Контракт - это не одна вещь, а три факта, которые должны быть одновременно верны после вызова:
    //Правильный результат - метод должен вернуть userId именно того пользователя, которому принадлежал токен
    //правильный побочный эффект на самом токене. Старый токен должен быть помечен как отозванный (revoked = true).
    //Отсутствие лишнего побочного эффекта. Метод НЕ должен задевать другие токены этого пользователя
    void rotate_validToken_revokesOldAndReturnsUserId() {
        //Генерируем случайный userId - представим, что это тот пользователь, которому принадлежит токен.
        //Его конкретное значение неважно - важно, что сервис должен вернуть именно его, не какой-то другой.
        UUID uuid = UUID.randomUUID();
        //Создаём "как бы существующий в базе" токен - реальный объект сущности (не мок!).
        RefreshToken rToken = new RefreshToken(uuid, "N:OUIFWUIQHNQ", Instant.now().plusSeconds(3600));
        //Это "подмена поведения" мока.  "Когда у refreshTokenRepository вызовут метод findByTokenHash
        // с любым аргументом (any()) - ответь Optional.of(rToken)", то есть "я нашёл этот токен в базе".
        // Без этой строки мок  по умолчанию отвечает Optional.empty().
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(rToken));
        //Сам вызов тестируемого кода. Передаём произвольную строку как "сырой" токен клиента, внутри rotate сервис захеширует её
        // и позвонит в refreshTokenRepository.findByTokenHash(...), а мок ответит на любой хеш одинаково (мы это настроили строкой выше через any()).
        UUID rotatedUUID = refreshTokenService.rotate("WFDQLIUQJPOI(NKLC)");

        //Проверка результата: метод должен был вернуть ровно того пользователя, который владел токеном.
        assertThat(rotatedUUID).isEqualTo(uuid);
        //Проверка побочного эффекта: раз метод "сжигает" старый токен при ротации - убеждаемся, что revoke() реально был вызван.
        assertThat(rToken.isRevoked()).isTrue();
        //Проверка "чего не произошло". findAllByUserIdAndRevokedFalse вызывается только в ветке "токен украден".
        //Эта строка - явное утверждение "в этом сценарии такого не было", то есть мы случайно не попали в чужую ветку кода.
        //verify(мок, never()).метод(...) — способ проверить вызовы (или их отсутствие) именно на моках, а не на обычных объектах
        verify(refreshTokenRepository, never()).findAllByUserIdAndRevokedFalse(any());
    }

    @Test
    void rotate_tokenNotFound_throwsInvalidRefreshTokenException() {
        //Настраивает мок так, что на любой хеш (any()) репозиторий отвечает «ничего не нашёл»
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThatExceptionOfType(InvalidRefreshTokenException.class)
                .isThrownBy(() -> refreshTokenService.rotate("WFDQLIUQJPOI"));
        //проверка отсутствия побочного эффекта - убеждаемся, что ветка «не найден» не утекла по ошибке
        // в логику «отозвать все токены пользователя»
        verify(refreshTokenRepository, never()).findAllByUserIdAndRevokedFalse(any());
    }

    @Test
    void rotate_tokenAlreadyRevoked_throwsInvalidRefreshTokenException() {
        UUID userId = UUID.randomUUID();
        RefreshToken expiredRefreshToken = new RefreshToken(userId, "qdqwqwqw", Instant.now().plusSeconds(3600));
        expiredRefreshToken.revoke();
        RefreshToken nonExpired = new RefreshToken(userId, "wqeqweqewq", Instant.now().plusSeconds(3600));
        RefreshToken nonExpired2 = new RefreshToken(userId, "dqwdwqdqw", Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expiredRefreshToken));
        when(refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId)).thenReturn(List.of(nonExpired, nonExpired2));

        assertThatExceptionOfType(InvalidRefreshTokenException.class)
                .isThrownBy(() -> refreshTokenService.rotate("WFDQLIUQJPOI"));
        assertThat(nonExpired.isRevoked()).isTrue();
        assertThat(nonExpired2.isRevoked()).isTrue();
        verify(refreshTokenRepository).findAllByUserIdAndRevokedFalse(userId);
    }

    @Test
        //Сначала генерим токен, потом пишем что при поиске любого токена, будет возвращен именно этот,
        //потом проверяем что вылетит нужная нам ошибка при вызове сервисного метода, которого мы проверяем,
        // потом проверяем что наш токен не отозван
        //потом проверяем, что не будет вызываться метод, который при проверке того, отозван токен или нет,
        //отзывает все другие токены
    void rotate_tokenExpired_throwsInvalidRefreshTokenException() {
        RefreshToken expiredRToken = new RefreshToken(UUID.randomUUID(), "wqeqweqewq", Instant.now().minusSeconds(3600));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expiredRToken));

        assertThatExceptionOfType(InvalidRefreshTokenException.class)
                .isThrownBy(() -> refreshTokenService.rotate("WFDQLIUQJPOI"));

        assertThat(expiredRToken.isRevoked()).isFalse();
        verify(refreshTokenRepository, never()).findAllByUserIdAndRevokedFalse(any());
    }

    @Test
    void revoke_existingToken_marksItRevoked() {
        RefreshToken rToken = new RefreshToken(UUID.randomUUID(), "wqeqweqewq", Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(rToken));
        refreshTokenService.revoke("WFDQLIUQJPOI");
        assertThat(rToken.isRevoked()).isTrue();


    }

    @Test
    void revoke_tokenNotFound_doesNothing() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThatCode(() -> refreshTokenService.revoke("WFDQLIUQJPOI"))
                .doesNotThrowAnyException();
        verify(refreshTokenRepository, never()).findAllByUserIdAndRevokedFalse(any());

    }

    @Test
    void createToken_validUserId_savesHashedTokenAndReturnRawToken() {
        UUID userId = UUID.randomUUID();
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        String rawRToken = refreshTokenService.createToken(userId);

        assertThat(rawRToken).isNotBlank();

        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken refreshToken = captor.getValue();

        assertThat(refreshToken.getUserId()).isEqualTo(userId);
        assertThat(refreshToken.isRevoked()).isFalse();
        assertThat(refreshToken.getExpiredAt().isAfter(Instant.now())).isTrue();
    }

    @Test
    void revokeAllForUser_activeTokensExist_revokesAll() {
        UUID userId = UUID.randomUUID();
        RefreshToken rToken1 = new RefreshToken(userId, "wqeqweQWDWQqewq", Instant.now().plusSeconds(3600));
        RefreshToken rToken2 = new RefreshToken(userId, "wqeDCDqweqewq", Instant.now().plusSeconds(3600));
        RefreshToken rToken3 = new RefreshToken(userId, "wqeqweqeAQwq", Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId)).thenReturn(List.of(rToken1, rToken2, rToken3));

        refreshTokenService.revokeAllForUser(userId);

        assertThat(rToken1.isRevoked()).isTrue();
        assertThat(rToken2.isRevoked()).isTrue();
        assertThat(rToken3.isRevoked()).isTrue();
        verify(refreshTokenRepository).findAllByUserIdAndRevokedFalse(userId);
    }

    @Test
    void revokeAllForUser_noActiveTokens_doesNothing() {
        UUID userId = UUID.randomUUID();
        when(refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId)).thenReturn(List.of());

        assertThatCode(() -> refreshTokenService.revokeAllForUser(userId))
                .doesNotThrowAnyException();
        verify(refreshTokenRepository).findAllByUserIdAndRevokedFalse(userId);
    }
}
