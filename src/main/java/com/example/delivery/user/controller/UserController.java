package com.example.delivery.user.controller;

import com.example.delivery.user.dto.request.SignupRequest;
import com.example.delivery.user.dto.response.UserResponse;
import com.example.delivery.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/users")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {
        UserResponse userResponse = userService.signup(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }


}
