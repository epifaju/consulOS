package com.consulos.document.storage.dto;

import java.util.UUID;

public record DocumentStorageMissingItemDTO(
        UUID id,
        String nomOriginal,
        String cheminStockage
) {
}
