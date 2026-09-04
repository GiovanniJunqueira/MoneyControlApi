package com.financeiro.api.service;

import com.financeiro.api.dto.auth.*;
import com.financeiro.api.entity.ModuleSettings;
import com.financeiro.api.entity.ModuleType;
import com.financeiro.api.entity.User;
import com.financeiro.api.exception.AppException;
import com.financeiro.api.repository.ModuleSettingsRepository;
import com.financeiro.api.repository.UserRepository;
import com.financeiro.api.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ModuleSettingsRepository moduleSettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                        ModuleSettingsRepository moduleSettingsRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.userRepository = userRepository;
        this.moduleSettingsRepository = moduleSettingsRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new AppException("Já existe uma conta com esse e-mail.", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        // Cria as configurações padrão de período fiscal (fechamento dia 1) para os dois módulos
        moduleSettingsRepository.save(new ModuleSettings(user, ModuleType.GASTOS, 1));
        moduleSettingsRepository.save(new ModuleSettings(user, ModuleType.DEVEDORES, 1));

        String token = jwtService.generateToken(user.getId());
        return new AuthResponse(toResponse(user), token);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("E-mail ou senha inválidos.");
        }

        String token = jwtService.generateToken(user.getId());
        return new AuthResponse(toResponse(user), token);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
