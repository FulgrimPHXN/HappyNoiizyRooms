package org.diceordieass.users.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.diceordieass.users.dto.ChangeUserNameRequest;
import org.diceordieass.users.dto.ChangeUserPasswordRequest;
import org.diceordieass.users.dto.UserResponse;
import org.diceordieass.users.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;


    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(Authentication authentication,
                                               @Valid @RequestBody ChangeUserPasswordRequest dto) {
        userService.changePassword(authentication, dto);
      return ResponseEntity.noContent().build();
    }

    @PatchMapping("/username")
    public ResponseEntity<UserResponse> changeUserName(Authentication authentication,
            @Valid @RequestBody ChangeUserNameRequest dto) {
       return ResponseEntity.ok(UserResponse.from(userService.changeUserName(authentication, dto))) ;
    }

}
