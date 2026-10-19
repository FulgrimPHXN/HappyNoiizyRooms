package org.diceordieass.gateway.filter;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/*
 * JwtGlobalFilter (GlobalFilter) видит только запросы, которые Gateway проксирует по
 * сконфигурированным маршрутам (application.yml) — /actuator/** обслуживается
 * отдельным WebFlux-хендлером самого Actuator'а и через GlobalFilter-цепочку не
 * проходит вообще. WebFilter — более низкий уровень реактивного стека, общий для
 * вообще всех запросов на этот сервер (включая actuator), поэтому закрывать
 * /actuator/metrics и /actuator/prometheus ролью ADMIN нужно именно здесь, а не в
 * JwtGlobalFilter. Роль берём из того же claim'а realm_access.roles токена Keycloak
 * (CLAUDE.md, раздел 4). /actuator/health остаётся открытым — его читают внешние
 * health-check'и, не люди.
 */
@Component
@RequiredArgsConstructor
public class ActuatorAuthFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ADMIN_ROLE = "ADMIN";

    private final JwtDecoder jwtDecoder;
    private final MeterRegistry meterRegistry;

    @NullMarked
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!isProtectedActuatorPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return reject(exchange, path, HttpStatus.UNAUTHORIZED, "missing_token");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(token);
        } catch (JwtException _) {
            return reject(exchange, path, HttpStatus.UNAUTHORIZED, "invalid_token");
        }

        if (!hasAdminRole(jwt)) {
            return reject(exchange, path, HttpStatus.FORBIDDEN, "missing_admin_role");
        }
        return chain.filter(exchange);
    }

    private boolean isProtectedActuatorPath(String path) {
        return path.startsWith("/actuator/metrics") || path.equals("/actuator/prometheus");
    }

    @SuppressWarnings("unchecked")
    private boolean hasAdminRole(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return false;
        }
        List<String> roles = (List<String>) realmAccess.getOrDefault("roles", List.of());
        return roles.contains(ADMIN_ROLE);
    }

    //Тот же счётчик-подход, что и в JwtGlobalFilter (gateway.jwt.rejected) — отдельное
    //имя метрики, потому что это другая причина отказа (роль, не подпись токена)
    private Mono<Void> reject(ServerWebExchange exchange, String path, HttpStatus status, String reason) {
        meterRegistry.counter("gateway.actuator.rejected", "reason", reason, "path", path).increment();
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        byte[] body = ("{\"status\":" + status.value() + ",\"title\":\"" + status.getReasonPhrase()
                + "\",\"detail\":\"Требуется роль ADMIN\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
