package org.example.flashsale.api;

import jakarta.validation.Valid;
import org.example.flashsale.api.dto.*;
import org.example.flashsale.model.User;
import org.example.flashsale.service.AuthService;
import org.example.flashsale.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.email(), request.password());
        return DtoMapper.toDto(user);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request.email(), request.password());
        String token = jwtService.generate(user);
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusNanos(jwtService.getExpirationMs() * 1_000_000L);

        return new LoginResponse(token, expiresAt, DtoMapper.toDto(user));
    }
}