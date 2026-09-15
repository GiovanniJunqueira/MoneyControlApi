package com.financeiro.api.controller;

import com.financeiro.api.dto.settings.ModuleSettingsRequest;
import com.financeiro.api.dto.settings.ModuleSettingsResponse;
import com.financeiro.api.entity.ModuleType;
import com.financeiro.api.service.ModuleSettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tabs/{tabId}/module-settings")
public class ModuleSettingsController {

    private final ModuleSettingsService moduleSettingsService;

    public ModuleSettingsController(ModuleSettingsService moduleSettingsService) {
        this.moduleSettingsService = moduleSettingsService;
    }

    @GetMapping("/{module}")
    public ModuleSettingsResponse get(@PathVariable UUID tabId, @PathVariable String module) {
        return moduleSettingsService.get(tabId, parseModule(module));
    }

    @PutMapping("/{module}")
    public ModuleSettingsResponse update(@PathVariable UUID tabId, @PathVariable String module,
                                          @Valid @RequestBody ModuleSettingsRequest request) {
        return moduleSettingsService.update(tabId, parseModule(module), request);
    }

    private ModuleType parseModule(String module) {
        return ModuleType.valueOf(module.toUpperCase());
    }
}
