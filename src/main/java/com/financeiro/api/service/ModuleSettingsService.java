package com.financeiro.api.service;

import com.financeiro.api.dto.settings.ModuleSettingsRequest;
import com.financeiro.api.dto.settings.ModuleSettingsResponse;
import com.financeiro.api.entity.ModuleSettings;
import com.financeiro.api.entity.ModuleType;
import com.financeiro.api.entity.Tab;
import com.financeiro.api.entity.User;
import com.financeiro.api.exception.AppException;
import com.financeiro.api.repository.ModuleSettingsRepository;
import com.financeiro.api.repository.TabRepository;
import com.financeiro.api.repository.UserRepository;
import com.financeiro.api.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ModuleSettingsService {

    private final ModuleSettingsRepository moduleSettingsRepository;
    private final UserRepository userRepository;
    private final TabRepository tabRepository;

    public ModuleSettingsService(ModuleSettingsRepository moduleSettingsRepository, UserRepository userRepository,
                                  TabRepository tabRepository) {
        this.moduleSettingsRepository = moduleSettingsRepository;
        this.userRepository = userRepository;
        this.tabRepository = tabRepository;
    }

    public ModuleSettingsResponse get(UUID tabId, ModuleType module) {
        ModuleSettings settings = moduleSettingsRepository.findByTabIdAndModule(findOwnedTab(tabId).getId(), module)
                .orElseGet(() -> createDefault(tabId, module));
        return toResponse(settings);
    }

    public ModuleSettingsResponse update(UUID tabId, ModuleType module, ModuleSettingsRequest request) {
        Tab tab = findOwnedTab(tabId);
        ModuleSettings settings = moduleSettingsRepository.findByTabIdAndModule(tab.getId(), module)
                .orElseGet(() -> createDefault(tabId, module));
        settings.setClosingDay(request.closingDay());
        moduleSettingsRepository.save(settings);
        return toResponse(settings);
    }

    /** Usado internamente por outros serviços (Expense/Dashboard) pra saber o dia de fechamento atual da aba. */
    public int getClosingDay(UUID tabId, ModuleType module) {
        return moduleSettingsRepository.findByTabIdAndModule(tabId, module)
                .map(ModuleSettings::getClosingDay)
                .orElse(1);
    }

    private ModuleSettings createDefault(UUID tabId, ModuleType module) {
        Tab tab = findOwnedTab(tabId);
        User user = userRepository.getReferenceById(CurrentUser.id());
        ModuleSettings settings = new ModuleSettings(user, tab, module, 1);
        return moduleSettingsRepository.save(settings);
    }

    private Tab findOwnedTab(UUID tabId) {
        return tabRepository.findByIdAndUserId(tabId, CurrentUser.id())
                .orElseThrow(() -> new AppException("Aba não encontrada.", HttpStatus.NOT_FOUND));
    }

    private ModuleSettingsResponse toResponse(ModuleSettings s) {
        return new ModuleSettingsResponse(s.getId(), s.getModule().name().toLowerCase(), s.getClosingDay());
    }
}
