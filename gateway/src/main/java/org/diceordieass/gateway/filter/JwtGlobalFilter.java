package org.diceordieass.gateway.filter;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.diceordieass.gateway.security.JwtService;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/*
 * Reactive-аналог JwtAuthenticationFilter, но здесь нет
 * Spring Security вообще (ни SecurityContextHolder, ни SecurityFilterChain):
 * Gateway не аутентифицирует запрос для себя, он просто решает "пропустить
 * дальше или оборвать здесь". Это первый, самый быстрый уровень
 * defense-in-depth — сервисы всё равно проверяют токен ещё раз сами,
 * своим собственным JwtAuthenticationFilter, независимо от этого решения.
 */
@Component
@RequiredArgsConstructor
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    //эти пути существуют именно для получения токена — требовать токен,
    //чтобы получить токен, лишает их смысла
    private static final List<String> SKIP_PREFIXES = List.of("/api/auth/");

    private final JwtService jwtService;

    @NullMarked
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //Достаём путь запроса (/api/rooms, /api/auth/login и т.п.). Если он начинается с одного из путей-исключений - сразу зовём chain.filter(exchange) (аналог filterChain.doFilter(...)
        // в servlet-мире: «я закончил, передаю дальше, не вмешиваясь») и выходим — без проверки токена.
        String path = exchange.getRequest().getURI().getPath();
        if (SKIP_PREFIXES.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }
        //Достаём заголовок Authorization. Если его нет или он не той схемы - в отличие от JWT фильтров в сервисах (где это молча пропускается дальше, а решение «нужна ли аутентификация» отдаётся Spring Security),
        // здесь Gateway сам обязан принять решение, потому что дальше в цепочке никто больше не проверит — отсюда сразу unauthorized(exchange).
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            jwtService.extractUserId(token);
        } catch (JwtException _) {
            return unauthorized(exchange);
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
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
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
