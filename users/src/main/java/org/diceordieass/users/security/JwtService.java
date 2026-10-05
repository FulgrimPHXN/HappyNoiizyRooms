package org.diceordieass.users.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;

    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        /*Сначала Decoders.BASE64.decode(secret) превращает строку обратно в 32 байта,
         затем Keys.hmacShaKeyFor(...) оборачивает эти байты в объект SecretKey, с которым работает криптография JDK.
         Секрет хранится в конфиге в виде Base64-строки не для защиты, а потому что исходные случайные байты нельзя надёжно записать как текст,
          Base64 просто превращает произвольные байты в печатаемые символы, а, затем
         помещаем его в объект, методом hmacShaKeyFor который предназначен для HMAC ключей
         */
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    @SuppressWarnings("java:S1149")
    //метод выпуска токена, параметр — логин пользователя (тот, что подтвердил AuthenticationManager при логине)
    public String generateToken(String username) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationMs);
        return Jwts.builder()
                .subject(username)//кладёт username в поле sub токена (стандартное поле JWT-спецификации — «кому принадлежит токен»)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();//сериализует всё в финальную строку вида xxxxx.yyyyy.zzzzz
        // (header.payload.signature, каждая часть — Base64URL) — это и есть готовый JWT, который уйдёт клиенту.
    }
    //публичный метод для JWT-фильтра: дай токен, получи username обратно
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();//достаёт обратно то самое значение, что положили через .subject(username)
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)//говорит парсеру, каким ключом проверять подпись
                .build()
                .parseSignedClaims(token)// разбирает строку токена, проверяет подпись и срок жизни за один вызов.
                .getPayload();
    }
}
