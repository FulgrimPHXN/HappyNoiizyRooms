package org.diceordieass.rooms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.UUID;

/*
 * В отличие от users.security.JwtService, здесь нет generateToken и expirationMs:
 * :rooms никогда не выпускает JWT — он только проверяет подпись токена, выпущенного
 * :users (общий секрет, см. комментарий в rooms/application.properties). Урезанный
 * класс — не копия ради экономии, а честное отражение того, что этому сервису
 * реально нужно.
 */
@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public UUID extractUserId(String token) {
        String subject = parseClaims(token).getSubject();
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new MalformedJwtException("sub токена не является UUID: " + subject, e);
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
