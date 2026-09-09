package com.consulos.portal;

import com.consulos.common.BusinessException;
import com.consulos.document.DocumentService;
import com.consulos.document.dto.DocumentDTO;
import com.consulos.portal.dto.PortalDocumentDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalDocumentServiceMoreTest {

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private PortalDocumentService portalDocumentService;

    private PortalSession session;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        documentId = UUID.randomUUID();
        session = new PortalSession(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "DOS-2026-0001"
        );
    }

    @Test
    void upload_whenFileNull_shouldThrowFileRequired() {
        assertThatThrownBy(() -> portalDocumentService.upload(session, null, "PIECE_IDENTITE", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("FILE_REQUIRED");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(documentService, never()).upload(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void upload_whenTypeNull_shouldDefaultToAutre() {
        MockMultipartFile file = new MockMultipartFile("file", "x.pdf", "application/pdf", "data".getBytes());
        DocumentDTO created = new DocumentDTO(
                documentId,
                session.dossierId(),
                session.usagerId(),
                "stored.pdf",
                "x.pdf",
                "AUTRE",
                "application/pdf",
                4L,
                null,
                1,
                null,
                null
        );
        when(documentService.upload(
                eq(file),
                eq(session.dossierId()),
                eq(session.usagerId()),
                eq("AUTRE"),
                isNull(),
                isNull(),
                eq(session.organisationId())
        )).thenReturn(created);

        PortalDocumentDTO result = portalDocumentService.upload(session, file, null, null);

        assertThat(result.typeDocument()).isEqualTo("AUTRE");
        verify(documentService).upload(
                eq(file),
                eq(session.dossierId()),
                eq(session.usagerId()),
                eq("AUTRE"),
                isNull(),
                isNull(),
                eq(session.organisationId())
        );
    }

    @Test
    void upload_whenTypeBlank_shouldThrowTypeInvalid() {
        MockMultipartFile file = new MockMultipartFile("file", "x.pdf", "application/pdf", "data".getBytes());

        assertThatThrownBy(() -> portalDocumentService.upload(session, file, "   ", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("TYPE_INVALID");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(documentService, never()).upload(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void list_shouldPassSessionOrganisationId() {
        when(documentService.listByDossier(session.dossierId(), session.organisationId()))
                .thenReturn(List.of());

        portalDocumentService.list(session);

        verify(documentService).listByDossier(session.dossierId(), session.organisationId());
    }

    @Test
    void upload_mapsNullStatutTailleCreatedAt() {
        MockMultipartFile file = new MockMultipartFile("file", "x.pdf", "application/pdf", "data".getBytes());
        DocumentDTO created = new DocumentDTO(
                documentId,
                session.dossierId(),
                session.usagerId(),
                "stored.pdf",
                "x.pdf",
                "PHOTO",
                "application/pdf",
                null,
                null,
                1,
                null,
                null
        );
        when(documentService.upload(
                eq(file),
                eq(session.dossierId()),
                eq(session.usagerId()),
                eq("PHOTO"),
                isNull(),
                isNull(),
                eq(session.organisationId())
        )).thenReturn(created);

        PortalDocumentDTO result = portalDocumentService.upload(session, file, "PHOTO", null);

        assertThat(result.statutControle()).isNull();
        assertThat(result.tailleOctets()).isEqualTo(0L);
        assertThat(result.createdAt()).isNull();
    }
}
