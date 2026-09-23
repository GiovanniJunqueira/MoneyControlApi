package com.financeiro.api.service;

import com.financeiro.api.dto.auth.*;
import com.financeiro.api.entity.ModuleSettings;
import com.financeiro.api.entity.ModuleType;
import com.financeiro.api.entity.Tab;
import com.financeiro.api.entity.User;
import com.financeiro.api.exception.AppException;
import com.financeiro.api.repository.ModuleSettingsRepository;
import com.financeiro.api.repository.TabRepository;
import com.financeiro.api.repository.UserRepository;
import com.financeiro.api.security.CurrentUser;
import com.financeiro.api.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TabRepository tabRepository;
    private final ModuleSettingsRepository moduleSettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;

    public AuthService(UserRepository userRepository,
                        TabRepository tabRepository,
                        ModuleSettingsRepository moduleSettingsRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        MailService mailService) {
        this.userRepository = userRepository;
        this.tabRepository = tabRepository;
        this.moduleSettingsRepository = moduleSettingsRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
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

        // Toda conta nova começa com uma aba "Geral"
        Tab tab = new Tab();
        tab.setUser(user);
        tab.setName("Geral");
        tab.setColor("#007AFF");
        tabRepository.save(tab);

        // Cria as configurações padrão de período fiscal (fechamento dia 1) para os dois módulos
        moduleSettingsRepository.save(new ModuleSettings(user, tab, ModuleType.GASTOS, 1));
        moduleSettingsRepository.save(new ModuleSettings(user, tab, ModuleType.DEVEDORES, 1));

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

    /**
     * Sempre "sucede" do ponto de vista do chamador, exista ou não o e-mail - não revela se
     * uma conta existe pra quem não tá logado. Um token novo sobrescreve qualquer um anterior
     * (só um reset pendente por vez faz sentido pro tamanho desse app).
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiresAt(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
            mailService.sendPasswordReset(user.getEmail(), user.getName(), token);
        });
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.token())
                .filter(u -> u.getResetTokenExpiresAt() != null && u.getResetTokenExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new AppException("Link inválido ou expirado. Peça um novo.", HttpStatus.BAD_REQUEST));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        userRepository.save(user);
    }

    public UserResponse updateSettings(UpdateSettingsRequest request) {
        User user = userRepository.findById(CurrentUser.id()).orElseThrow();
        user.setBetsEnabled(request.betsEnabled());
        userRepository.save(user);
        return toResponse(user);
    }

    /** Liga (ou desliga, se phone vier vazio) o número que o bot do WhatsApp usa pra reconhecer essa
     * conta - normaliza pra só dígitos (com DDI) porque é assim que o WhatsApp manda o remetente no
     * webhook, e a pessoa pode ter digitado com "+", espaço ou traço. */
    public UserResponse updateWhatsAppPhone(WhatsAppSettingsRequest request) {
        User user = userRepository.findById(CurrentUser.id()).orElseThrow();
        String normalized = normalizePhone(request.phone());

        if (normalized == null) {
            user.setWhatsappPhone(null);
        } else {
            userRepository.findByWhatsappPhone(normalized).filter(u -> !u.getId().equals(user.getId()))
                    .ifPresent(u -> { throw new AppException("Esse número já está vinculado a outra conta.", HttpStatus.CONFLICT); });
            user.setWhatsappPhone(normalized);
        }
        userRepository.save(user);
        return toResponse(user);
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.isBlank() ? null : digits;
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt(), user.isBetsEnabled(),
                user.getWhatsappPhone());
    }
}
