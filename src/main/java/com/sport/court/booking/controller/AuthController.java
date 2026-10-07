package com.sport.court.booking.controller;

import com.sport.court.booking.domain.User;
import com.sport.court.booking.dto.*;
import com.sport.court.booking.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registro, inicio y cierre de sesión (HU13, HU14, HU15, HU19)")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Registrar usuario (HU13)")
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Iniciar sesión (HU14)")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Usuario autenticado actual")
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(UserDto.from(user));
    }

    @Operation(summary = "Cerrar sesión (HU15) — revoca el token actual")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(authorization.replaceFirst("^Bearer\\s+", ""));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reenviar correo de confirmación (HU19)")
    @PostMapping("/resend-confirmation")
    public ResponseEntity<Map<String, String>> resend(@Valid @RequestBody EmailRequest request) {
        authService.resendConfirmation(request.email());
        return ResponseEntity.ok(Map.of("message",
                "Si el correo está registrado, recibirás un nuevo mensaje de confirmación"));
    }
}
