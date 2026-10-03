package org.diceordieass.users.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.diceordieass.users.dto.UserDto;
import org.diceordieass.users.dto.UserResponse;
import org.diceordieass.users.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")//Связывает HTTP-запрос с классом по URL-пути. Задаёт базовый префикс пути для всех методов внутри этого контроллера.
public class UserController {

    private final UserService userService;

    @PostMapping
    /*
    ResponseEntity<T> — это Java-представление всего HTTP-ответа целиком: код статуса, заголовки и тело — одним объектом.
    Не просто данные, которые нужно вернуть, а полное описание того, что уйдёт клиенту по протоколу HTTP.
    HTTP как протокол — это не только тело ответа, а именно триада "статус + заголовки + тело", и ResponseEntity — единственный способ управлять всеми тремя частями одновременно и явно из кода контроллера.
    Без него вы отдаёте эти решения Spring'у на откуп (дефолтный 200, дефолтные заголовки)
     */
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserDto userDto) {
        /*@Valid проверяет условия валидации полей объекта, в этом случае UserDto @NotBlank, @Size max
         @Valid триггерит Bean Validation (Hibernate Validator), который читает аннотации-ограничения (@NotBlank, @Size),
         развешанные на полях UserDto, и сверяет их с реальными значениями, которые пришли в теле конкретного HTTP-запроса.
         Если хоть одно условие нарушено — бросается MethodArgumentNotValidException (тот самый, что ловит ваш handleValidation).
         */
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(userService.createUser(userDto)));
    }
}
