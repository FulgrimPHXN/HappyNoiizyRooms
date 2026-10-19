package org.diceordieass.users.service;

import org.diceordieass.users.entity.RefreshToken;
import org.diceordieass.users.exception.InvalidRefreshTokenException;
import org.diceordieass.users.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final long refreshExpireTime;
    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpireTime) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpireTime = refreshExpireTime;
    }

    @Transactional
    public void revoke (String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(RefreshToken::revoke);
    }

    //метод отзыва старого refresh токена с подтверждением его владельца
    //создание нового токена сюда больше не входит: этому методу для подтверждения владельца
    //нужен только userId, а чем дальше распорядится вызывающий код (новый access + новый refresh) — не его забота
    @Transactional
    public UUID rotate(String rawOldToken) {
        //хеш refresh токена клиента хранится в бд, мы сравниваем его с его же сырым токеном
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawOldToken))
                .orElseThrow(() ->  new InvalidRefreshTokenException("Invalid Refresh Token"));

        //Если токен был отозван, то отзывает вообще все активные токены этого пользователя
        // потому что повторное предъявление уже использованного токена — признак кражи
        Optional.of(token).filter(RefreshToken::isRevoked).ifPresent(rt -> {
            revokeAllForUser(rt.getUserId());
            throw new InvalidRefreshTokenException("Invalid Refresh Token");
        });
        //время токена вышло
        if (token.getExpiredAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Invalid Refresh Token");
        }
        //старый токен протухает
        token.revoke();
        //отдаём наружу владельца токена — вызывающий код сам решает, что с этим делать
        //(новый access-JWT нужен username, новый refresh — отдельный вызов createToken)
        return token.getUserId();
    }

    public String createToken(UUID userId) {
        //создает случайную строку
        String rawToken = generateToken();
        //хеширует для хранения
        String tokenHash = hash(rawToken);
        //Текущий момент + TTL из конфига = момент истечения.
        Instant expiresAt = Instant.now().plusMillis(refreshExpireTime);
        refreshTokenRepository.save(new RefreshToken(userId, tokenHash, expiresAt));
        return rawToken;
    }

    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId)
                .forEach(RefreshToken::revoke);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
    }

    private String generateToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
