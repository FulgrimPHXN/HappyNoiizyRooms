package org.diceordieass.rooms.advice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.diceordieass.rooms.exception.InvalidInviteCodeException;
import org.diceordieass.rooms.exception.PlayerAlreadyJoinedException;
import org.diceordieass.rooms.exception.PlayerNotInRoomException;
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

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException e) {
        Map<String, String> errors = e.getConstraintViolations().stream()
                .collect(Collectors.toMap(v -> lastSegment(v.getPropertyPath().toString()),
                        ConstraintViolation::getMessage, (first, second) -> first));
        ProblemDetail problemDetails = ProblemDetail
                .forStatusAndDetail(HttpStatus.BAD_REQUEST, "Ошибка валидации");
        problemDetails.setProperty("errors", errors);
        return problemDetails;
    }

    private static String lastSegment(String propertyPath) {
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot == -1 ? propertyPath : propertyPath.substring(lastDot + 1);
    }

    @ExceptionHandler(RoomAlreadyExistsException.class)
    public ProblemDetail handleRoomAlreadyExists (RoomAlreadyExistsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(RoomAccessDeniedException.class)
    public ProblemDetail handleRoomAccessDenied(RoomAccessDeniedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(InvalidInviteCodeException.class)
    public ProblemDetail handleInvalidInviteCode(InvalidInviteCodeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(PlayerAlreadyJoinedException.class)
    public ProblemDetail handlePlayerAlreadyJoined(PlayerAlreadyJoinedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(PlayerNotInRoomException.class)
    public ProblemDetail handlePlayerNotInRoom(PlayerNotInRoomException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Комната с таким названием уже существует");
    }
}
