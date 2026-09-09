package com.consulos.document.content;

import com.consulos.common.BusinessException;
import com.consulos.document.DocumentEntity;
import com.consulos.document.DocumentRepository;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentContentServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private DocumentContentService documentContentService;

    private UUID documentId;
    private UUID organisationId;
    private DocumentEntity document;

    @BeforeEach
    void setUp() {
        documentId = UUID.randomUUID();
        organisationId = UUID.randomUUID();
        document = new DocumentEntity();
        document.setId(documentId);
        document.setOrganisationId(organisationId);
        document.setNomOriginal("passeport.pdf");
        document.setMimeType("application/pdf");
        document.setCheminStockage("org/" + organisationId + "/passeport.pdf");
    }

    @Test
    void open_whenDocumentOtherOrg_shouldThrowNotFound() {
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, organisationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentContentService.open(documentId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("DOCUMENT_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void open_whenObjectPresent_shouldReturnBytes() throws Exception {
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, organisationId))
                .thenReturn(Optional.of(document));
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);
        GetObjectResponse object = mock(GetObjectResponse.class);
        when(object.readAllBytes()).thenReturn("pdf-bytes".getBytes());
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(object);

        DocumentContentPayload payload = documentContentService.open(documentId, organisationId);

        assertThat(payload.fileName()).isEqualTo("passeport.pdf");
        assertThat(payload.mimeType()).isEqualTo("application/pdf");
        assertThat(new String(payload.bytes())).isEqualTo("pdf-bytes");
    }

    @Test
    void open_whenEmptyObject_shouldThrowFileMissing() throws Exception {
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, organisationId))
                .thenReturn(Optional.of(document));
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);
        GetObjectResponse object = mock(GetObjectResponse.class);
        when(object.readAllBytes()).thenReturn(new byte[0]);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(object);

        assertThatThrownBy(() -> documentContentService.open(documentId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("DOCUMENT_FILE_MISSING"));
    }

    @Test
    void open_whenNoSuchKey_shouldThrowFileMissing() throws Exception {
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, organisationId))
                .thenReturn(Optional.of(document));
        ErrorResponseException ex = mock(ErrorResponseException.class);
        ErrorResponse errorResponse = mock(ErrorResponse.class);
        when(errorResponse.code()).thenReturn("NoSuchKey");
        when(ex.errorResponse()).thenReturn(errorResponse);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(ex);

        assertThatThrownBy(() -> documentContentService.open(documentId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(err -> {
                    BusinessException be = (BusinessException) err;
                    assertThat(be.getErrorCode()).isEqualTo("DOCUMENT_FILE_MISSING");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void open_whenMinioFails_shouldThrowStorageError() throws Exception {
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, organisationId))
                .thenReturn(Optional.of(document));
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(new RuntimeException("down"));

        assertThatThrownBy(() -> documentContentService.open(documentId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("DOCUMENT_STORAGE_ERROR");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                });
    }
}
