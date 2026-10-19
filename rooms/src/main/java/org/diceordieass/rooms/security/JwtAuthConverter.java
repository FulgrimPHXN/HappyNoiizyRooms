package org.diceordieass.rooms.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/*
 * Spring Security по умолчанию делает principal'ом сам Jwt-объект. :rooms исторически
 * работает с голым UUID как principal'ом (так же делал старый JwtAuthenticationFilter) —
 * этот конвертер сохраняет тот же контракт, чтобы RoomController не трогать
 * (@AuthenticationPrincipal UUID ownerId продолжает работать как раньше). authorities
 * — пустой список, как и было: авторизация по ролям здесь не используется, только
 * факт валидного токена.
 */
@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String subject = jwt.getSubject();
        try {
            UUID userId = UUID.fromString(subject);
            return new UsernamePasswordAuthenticationToken(userId, null, List.of());
        } catch (IllegalArgumentException e) {
            //подпись и срок жизни токена Spring Security уже проверил сам (иначе до
            //конвертера дело не дошло бы) — но sub не UUID, такого токена Keycloak-realm
            //этого проекта никогда не выпускал, это тот же случай "токену нельзя доверять"
            throw new InvalidBearerTokenException("sub токена не является UUID: " + subject, e);
        }
    }
}
