package com.consulos.portal;

import com.consulos.common.BusinessException;
import com.consulos.document.DocumentService;
import com.consulos.document.StatutControle;
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

import java.time.OffsetDateTime;
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
class PortalDocumentServiceTest {

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
    void list_shouldMapDocuments() {
        DocumentDTO dto = sampleDoc("PIECE_IDENTITE");
        when(documentService.listByDossier(session.dossierId(), session.organisationId()))
                .thenReturn(List.of(dto));

        List<PortalDocumentDTO> result = portalDocumentService.list(session);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(documentId);
        assertThat(result.getFirst().typeDocument()).isEqualTo("PIECE_IDENTITE");
        assertThat(result.getFirst().statutControle()).isEqualTo("RECU");
    }

    @Test
    void upload_whenFileEmpty_shouldThrow() {
        MockMultipartFile empty = new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> portalDocumentService.upload(session, empty, "PIECE_IDENTITE", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("FILE_REQUIRED");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(documentService, never()).upload(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void upload_whenTypeInvalid_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile("file", "x.pdf", "application/pdf", "data".getBytes());

        assertThatThrownBy(() -> portalDocumentService.upload(session, file, "INVOICE", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("TYPE_INVALID"));
    }

    @Test
    void upload_whenValid_shouldDelegateAndMap() {
        MockMultipartFile file = new MockMultipartFile("file", "passeport.pdf", "application/pdf", "pdf".getBytes());
        DocumentDTO created = sampleDoc("JUSTIFICATIF");
        when(documentService.upload(
                eq(file),
                eq(session.dossierId()),
                eq(session.usagerId()),
                eq("JUSTIFICATIF"),
                eq("scan"),
                isNull(),
                eq(session.organisationId())
        )).thenReturn(created);

        PortalDocumentDTO result = portalDocumentService.upload(session, file, "justificatif", "scan");

        assertThat(result.nomOriginal()).isEqualTo("passeport.pdf");
        assertThat(result.tailleOctets()).isEqualTo(1024L);
    }

    private DocumentDTO sampleDoc(String type) {
        return new DocumentDTO(
                documentId,
                session.dossierId(),
                session.usagerId(),
                "stored.pdf",
                "passeport.pdf",
                type,
                "application/pdf",
                1024L,
                StatutControle.RECU,
                1,
                null,
                OffsetDateTime.parse("2026-09-09T10:00:00Z")
        );
    }
}
