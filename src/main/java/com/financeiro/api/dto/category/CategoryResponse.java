package com.financeiro.api.dto.category;

import java.util.UUID;

public record CategoryResponse(UUID id, String name, String color, String icon) {
}
