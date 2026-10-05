package org.diceordieass.users.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * У этого интерфейса ровно один метод. «Entry point» в его названии значит буквально «точка входа» — это то место,
 * куда Spring Security идёт, когда понимает: «на защищённый ресурс пришёл запрос без валидной аутентификации, нужно решить, как ответить клиенту».
 * До сих пор это решение принималось дефолтной реализацией Spring Security — теперь будет приниматься этой.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    //Эта же зависимость сереализует ответы от ВСЕХ контроллеров в spring в JSON формат
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        //отличие от @RestControllerAdvice: там достаточно было вернуть ProblemDetail из метода,
        // и Spring MVC сам доставал из него код статуса и выставлял его в ответ. Здесь мы не внутри MVC -
        // DispatcherServlet в эту точку вообще не задействован, поэтому статус приходится выставлять в response вручную, явным вызовом.
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Требуется аутентификация");

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        //MVC сам ставит заголовок Content-Type: application/problem+json для ProblemDetail-ответов,
        // здесь это тоже нужно сделать руками, иначе клиент получит JSON без заголовка, который говорит, как его интерпретировать.
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        //Тело ответа - сырой поток байт, в который можно писать напрямую (это и есть HTTP response body на самом низком доступном в Servlet API уровне).
        // objectMapper.writeValue(...) сериализует problemDetail в JSON и пишет этот JSON прямо в этот поток — ровно то, что при обычном контроллере сделал
        // бы HttpMessageConverter за кулисами, здесь делаем своими руками.
        objectMapper.writeValue(response.getOutputStream(), problemDetail);
    }
}
