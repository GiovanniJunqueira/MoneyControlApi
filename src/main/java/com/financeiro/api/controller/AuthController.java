package com.financeiro.api.controller;

import com.financeiro.api.dto.auth.*;
import com.financeiro.api.entity.User;
import com.financeiro.api.repository.UserRepository;
import com.financeiro.api.security.CurrentUser;
import com.financeiro.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        User user = userRepository.findById(CurrentUser.id()).orElseThrow();
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt(), user.isBetsEnabled(), user.getWhatsappPhone()));
    }

    @PutMapping("/settings")
    public UserResponse updateSettings(@Valid @RequestBody UpdateSettingsRequest request) {
        return authService.updateSettings(request);
    }

    @PutMapping("/whatsapp")
    public UserResponse updateWhatsAppPhone(@Valid @RequestBody WhatsAppSettingsRequest request) {
        return authService.updateWhatsAppPhone(request);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}
