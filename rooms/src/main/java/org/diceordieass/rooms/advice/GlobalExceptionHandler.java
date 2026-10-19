package org.diceordieass.rooms.advice;

import org.diceordieass.rooms.exception.RoomAccessDeniedException;
import org.diceordieass.rooms.exception.RoomAlreadyExistsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail handleNotFound(NoSuchElementException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Комната не найдена");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation (MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, fe ->
                        Objects.requireNonNullElse(fe.getDefaultMessage(), "Некорректное значение"), (first, second) -> first));
        ProblemDetail problemDetails = ProblemDetail
                .forStatusAndDetail(HttpStatus.BAD_REQUEST, "Ошибка Валидации");
        problemDetails.setProperty("errors", errors);
        return problemDetails;
    }

    @ExceptionHandler(RoomAlreadyExistsException.class)
    public ProblemDetail handleRoomAlreadyExists (RoomAlreadyExistsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(RoomAccessDeniedException.class)
    public ProblemDetail handleRoomAccessDenied(RoomAccessDeniedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    //сюда попадаем, когда предварительный findRoomByName не успел поймать гонку —
    //UNIQUE на name в БД сработал первым; сообщение нейтральное, не e.getMessage(),
    //чтобы не светить клиенту имя constraint'а/колонки из текста драйвера
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Комната с таким названием уже существует");
    }
}
