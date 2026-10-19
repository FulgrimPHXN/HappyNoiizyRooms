package org.diceordieass.users.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/*
 * Смена пароля/имени ушла в Keycloak целиком (Account Console/Admin API) — это его
 * credentials, не наши. Единственное, что осталось в :users после переноса логина на
 * Keycloak — smoke-test того, что Resource Server правильно проверяет подпись и
 * прокидывает claims токена. Настоящие профильные данные появятся здесь, когда они
 * реально понадобятся домену (см. CLAUDE.md, раздел 4) — пока их нет, не городим DTO
 * под то, чего не существует.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of(
                "id", jwt.getSubject(),
                "claims", jwt.getClaims()));
    }
}
