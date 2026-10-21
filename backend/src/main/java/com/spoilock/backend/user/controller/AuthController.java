package com.spoilock.backend.user.controller;

import com.spoilock.backend.user.dto.SignUpRequest;
import com.spoilock.backend.user.dto.SignUpResponse;
import com.spoilock.backend.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignUpResponse signUp(
            @Valid @RequestBody SignUpRequest request
            )
    {
        return authService.signUp(request);
    }

}
