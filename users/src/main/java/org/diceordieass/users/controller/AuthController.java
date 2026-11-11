package org.diceordieass.users.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.diceordieass.users.dto.AuthResponse;
import org.diceordieass.users.dto.RefreshTokenDto;
import org.diceordieass.users.dto.UserDto;
import org.diceordieass.users.dto.UserResponse;
import org.diceordieass.users.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")

    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserDto userDto) {

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(authService.createUser(userDto)));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody UserDto userDto) {
        AuthService.LoginResult result = authService.login(userDto);
        return ResponseEntity.ok(AuthResponse.from(result.user(), result.token(), result.refreshToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenDto dto) {
        AuthService.LoginResult result = authService.refresh(dto.refreshToken());
        return ResponseEntity.ok(AuthResponse.from(result.user(), result.token(), result.refreshToken()));
    }

    @PostMapping("/logout")

    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenDto dto) {
        authService.logout(dto.refreshToken());

        return ResponseEntity.noContent().build();
    }
}
