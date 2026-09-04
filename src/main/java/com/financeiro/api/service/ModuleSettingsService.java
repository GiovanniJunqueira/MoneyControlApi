package com.financeiro.api.service;

import com.financeiro.api.dto.settings.ModuleSettingsRequest;
import com.financeiro.api.dto.settings.ModuleSettingsResponse;
import com.financeiro.api.entity.ModuleSettings;
import com.financeiro.api.entity.ModuleType;
import com.financeiro.api.entity.User;
import com.financeiro.api.repository.ModuleSettingsRepository;
import com.financeiro.api.repository.UserRepository;
import com.financeiro.api.security.CurrentUser;
import org.springframework.stereotype.Service;

@Service
public class ModuleSettingsService {

    private final ModuleSettingsRepository moduleSettingsRepository;
    private final UserRepository userRepository;

    public ModuleSettingsService(ModuleSettingsRepository moduleSettingsRepository, UserRepository userRepository) {
        this.moduleSettingsRepository = moduleSettingsRepository;
        this.userRepository = userRepository;
    }

    public ModuleSettingsResponse get(ModuleType module) {
        ModuleSettings settings = moduleSettingsRepository.findByUserIdAndModule(CurrentUser.id(), module)
                .orElseGet(() -> createDefault(module));
        return toResponse(settings);
    }

    public ModuleSettingsResponse update(ModuleType module, ModuleSettingsRequest request) {
        ModuleSettings settings = moduleSettingsRepository.findByUserIdAndModule(CurrentUser.id(), module)
                .orElseGet(() -> createDefault(module));
        settings.setClosingDay(request.closingDay());
        moduleSettingsRepository.save(settings);
        return toResponse(settings);
    }

    /** Usado internamente por outros serviços (Expense/Dashboard) pra saber o dia de fechamento atual. */
    public int getClosingDay(ModuleType module) {
        return moduleSettingsRepository.findByUserIdAndModule(CurrentUser.id(), module)
                .map(ModuleSettings::getClosingDay)
                .orElse(1);
    }

    private ModuleSettings createDefault(ModuleType module) {
        User user = userRepository.getReferenceById(CurrentUser.id());
        ModuleSettings settings = new ModuleSettings(user, module, 1);
        return moduleSettingsRepository.save(settings);
    }

    private ModuleSettingsResponse toResponse(ModuleSettings s) {
        return new ModuleSettingsResponse(s.getId(), s.getModule().name().toLowerCase(), s.getClosingDay());
    }
}
