package org.diceordieass.users.advice;

import org.diceordieass.users.exception.IncorrectPasswordException;
import org.diceordieass.users.exception.InvalidRefreshTokenException;
import org.diceordieass.users.exception.UserAlreadyExistsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * {@code @ControllerAdvice} + {@code @Responsebody}. {@code @ControllerAdvice} сам по себе превращает класс в "глобальный помощник" для всех контроллеров приложения
 * <p>
 * Когда любой контроллер бросает исключение, которое не поймано внутри самого метода контроллера, Spring MVC идёт искать подходящий {@code @ExceptionHandler} сначала в самом контроллере,
 * а если там нет — по всем классам-{@code @ControllerAdvice}, сопоставляя тип исключения с тем, что указано в {@code @ExceptionHandler(КлассИсключения.class)}
 */
//подписан на исключения, которые бросает код внутри контроллеров.
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * "если где-то в контроллере вылетит исключение типа MethodArgumentNotValidException — не роняй приложение 500-й ошибкой, а вызови этот метод"
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation (MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getBindingResult().getFieldErrors().stream() //e.getBindingResult() — у исключения внутри лежит объект BindingResult — это "отчёт" о том, что именно пошло не так при валидации UserDto.
                .collect(Collectors.toMap(FieldError::getField, fe ->
                        Objects.requireNonNullElse(fe.getDefaultMessage(), "Некорректное значение"), (first, second) -> first));
                        /*(first, second) -> first — лямбда на случай, если у двух разных FieldError оказался одинаковый ключ (например, на одно поле сработали сразу @NotBlank и @Size).
                         Без неё Collectors.toMap бросил бы исключение при дубликате ключа; здесь просто говорим "оставь первое найденное сообщение, второе выбрось".
                         Итог этой строки: Map<String, String> errors вида {"userName": "must not be blank", "rawPassword": "size must be between 0 and 50"} — ключ-поле → сообщение-об-ошибке.*/
        ProblemDetail problemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Ошибка Валидации");
        //ProblemDetail — готовый класс из Spring, реализующий стандартную структуру JSON для ошибок
        problemDetails.setProperty("errors", errors);
        //Стандарт разрешает добавлять в тело ответа произвольные дополнительные поля сверх стандартных — setProperty("ключ", значение): добавляем в итоговый JSON поле "errors" со значением — той самой Map, что собрали выше.
        return problemDetails;
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists (UserAlreadyExistsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Пользователь с таким именем уже существует");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticateException(AuthenticationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Неверный логин или пароль");
    }

    @ExceptionHandler(IncorrectPasswordException.class)
    public ProblemDetail handleIncorrectPasswordException(IncorrectPasswordException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Ошибка валидации");
        problemDetail.setProperty("errors", Map.of("oldPassword", e.getMessage()));
        return problemDetail;
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ProblemDetail handleInvalidRefreshTokenException(InvalidRefreshTokenException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Неверный рефреш токен");
    }
    /*
    С такими валидаторами ошибка будет прилетать такого вида:
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Ошибка валидации",
  "errors": {"userName": "must not be blank"}
  Без этого хендлера такими:
  "timestamp": "2026-10-02T14:32:10.512+00:00",
  "status": 400,
  "error": "Bad Request",
  "path": "/api/user"
     */
}
