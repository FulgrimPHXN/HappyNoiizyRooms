package org.diceordieass.gateway.filter;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/*
 * Reactive-аналог проверки подписи в сервисах, но здесь нет Spring Security
 * вообще (ни SecurityContextHolder, ни SecurityFilterChain): Gateway не
 * аутентифицирует запрос для себя, он просто решает "пропустить дальше или
 * оборвать здесь". Это первый, самый быстрый уровень defense-in-depth —
 * :users/:rooms всё равно проверяют тот же токен ещё раз сами, своим
 * собственным Resource Server-конфигом, независимо от этого решения.
 * Токен теперь выпускает Keycloak — отсюда JwtDecoder (NimbusJwtDecoder по
 * JWKS), а не самодельный HMAC-секрет. /api/auth/** больше нет: за токеном
 * клиент идёт прямо в Keycloak, не через наш gateway.
 */
@Component
@RequiredArgsConstructor
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;
    //та же MeterRegistry, через которую GatewayMetricsFilter пишет spring_cloud_gateway_requests_seconds —
    //но её метрика не видит эти отказы (см. ниже), поэтому ведём свой счётчик рядом
    private final MeterRegistry meterRegistry;

    @NullMarked
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        //Достаём заголовок Authorization. Если его нет или он не той схемы - в отличие от JWT фильтров в сервисах (где это молча пропускается дальше, а решение «нужна ли аутентификация» отдаётся Spring Security),
        // здесь Gateway сам обязан принять решение, потому что дальше в цепочке никто больше не проверит — отсюда сразу unauthorized(exchange).
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, path, "missing_token");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            jwtDecoder.decode(token);
        } catch (JwtException _) {
            return unauthorized(exchange, path, "invalid_token");
        }
        //заголовок Authorization проксируется дальше автоматически (Gateway по
        //умолчанию копирует входящие заголовки) — :users/:rooms увидят тот же
        //токен и проверят его заново, независимо от этого решения
        return chain.filter(exchange);
    }

    //Чем меньше число, тем раньше фильтр выполняется относительно других GlobalFilter-ов в приложении.
    // -1 - просто «пораньше», сейчас в проекте этот фильтр единственный, так что конкретное значение пока ни с чем не конкурирует.
    @Override
    public int getOrder() {
        return -1;
    }

    //Выставляем код 401 и заголовок Content-Type - ровно то же самое, что RestAuthenticationEntryPoint делает в сервисах, только здесь нет Spring MVC/Servlet API,
    // поэтому и там, и здесь приходится делать это руками, но через reactive-объект exchange, а не HttpServletResponse.
    private Mono<Void> unauthorized(ServerWebExchange exchange, String path, String reason) {
        //этот отказ короткого замыкания не попадает в встроенную spring_cloud_gateway_requests_seconds —
        //GatewayMetricsFilter стоит в цепочке ПОСЛЕ этого фильтра (эмпирически проверено: без
        //chain.filter(exchange) метрика вообще не записывается), поэтому считаем сами
        meterRegistry.counter("gateway.jwt.rejected", "reason", reason, "path", path).increment();
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        byte[] body = "{\"status\":401,\"title\":\"Unauthorized\",\"detail\":\"Требуется аутентификация\"}"
                .getBytes(StandardCharsets.UTF_8);
        //Оборачиваем байты в reactive-буфер (DataBuffer) через фабрику конкретного ответа и отдаём его writeWith(...). Mono.just(buffer) - «вот единственное значение, которое
        //нужно записать». Возвращаемый Mono<Void> — это и есть итог всего метода filter: он не
        //«пишет и возвращает void» как в servlet-мире, а возвращает описание будущей записи, которое Spring/Netty выполнят сами, асинхронно.
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
