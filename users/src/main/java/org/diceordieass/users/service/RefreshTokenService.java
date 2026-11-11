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

    // Возвращает владельца; выпуск новой пары токенов координирует AuthService.
    @Transactional
    public UUID rotate(String rawOldToken) {

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawOldToken))
                .orElseThrow(() ->  new InvalidRefreshTokenException("Invalid Refresh Token"));

        // Повторное использование отозванного токена рассматриваем как возможную кражу.
        Optional.of(token).filter(RefreshToken::isRevoked).ifPresent(rt -> {
            revokeAllForUser(rt.getUserId());
            throw new InvalidRefreshTokenException("Invalid Refresh Token");
        });

        if (token.getExpiredAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Invalid Refresh Token");
        }

        token.revoke();

        return token.getUserId();
    }

    public String createToken(UUID userId) {

        String rawToken = generateToken();

        String tokenHash = hash(rawToken);

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
