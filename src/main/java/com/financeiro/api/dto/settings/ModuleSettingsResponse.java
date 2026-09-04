package com.financeiro.api.dto.settings;

import java.util.UUID;

public record ModuleSettingsResponse(UUID id, String module, Integer closingDay) {
}
