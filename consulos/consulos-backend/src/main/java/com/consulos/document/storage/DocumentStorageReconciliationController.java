package com.consulos.document.storage;

import com.consulos.common.ApiResponse;
import com.consulos.document.DocumentSecurityUtils;
import com.consulos.document.storage.dto.DocumentStorageReconciliationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/document-storage")
@RequiredArgsConstructor
public class DocumentStorageReconciliationController {

    private final DocumentStorageReconciliationService reconciliationService;

    @GetMapping("/reconciliation")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DocumentStorageReconciliationDTO> reconcile() {
        return ApiResponse.ok(reconciliationService.reconcile(DocumentSecurityUtils.getOrganisationId()));
    }
}
