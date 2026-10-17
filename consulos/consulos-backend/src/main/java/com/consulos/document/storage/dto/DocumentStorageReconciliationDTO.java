package com.consulos.document.storage.dto;

import java.util.List;

public record DocumentStorageReconciliationDTO(
        int totalActifs,
        int presentsDansMinio,
        int absentsDeMinio,
        int orphelinsMinio,
        List<DocumentStorageMissingItemDTO> fichiersManquants,
        List<String> clesOrphelines
) {
}
