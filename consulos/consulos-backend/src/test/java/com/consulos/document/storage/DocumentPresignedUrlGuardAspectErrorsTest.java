package com.consulos.document.storage;

import com.consulos.common.BusinessException;
import com.consulos.document.DocumentEntity;
import com.consulos.document.DocumentRepository;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import org.aspectj.lang.ProceedingJoinPoint;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentPresignedUrlGuardAspectErrorsTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private MinioClient minioClient;
    @Mock
    private ProceedingJoinPoint joinPoint;

    @InjectMocks
    private DocumentPresignedUrlGuardAspect aspect;

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
        document.setCheminStockage("orgs/" + organisationId + "/file.pdf");
        when(joinPoint.getArgs()).thenReturn(new Object[]{documentId, organisationId});
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, organisationId))
                .thenReturn(Optional.of(document));
    }

    @Test
    void ensureObjectExists_whenMinioCodeOtherThanNoSuchKey_shouldBeStorageError() throws Throwable {
        ErrorResponseException ex = mock(ErrorResponseException.class);
        ErrorResponse errorResponse = mock(ErrorResponse.class);
        when(errorResponse.code()).thenReturn("AccessDenied");
        when(ex.errorResponse()).thenReturn(errorResponse);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(ex);

        assertThatThrownBy(() -> aspect.ensureObjectExists(joinPoint))
                .isInstanceOf(BusinessException.class)
                .satisfies(err -> {
                    BusinessException be = (BusinessException) err;
                    assertThat(be.getErrorCode()).isEqualTo("DOCUMENT_STORAGE_ERROR");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                });
        verify(joinPoint, never()).proceed();
    }

    @Test
    void ensureObjectExists_whenErrorResponseNull_shouldBeStorageError() throws Throwable {
        ErrorResponseException ex = mock(ErrorResponseException.class);
        when(ex.errorResponse()).thenReturn(null);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(ex);

        assertThatThrownBy(() -> aspect.ensureObjectExists(joinPoint))
                .isInstanceOf(BusinessException.class)
                .satisfies(err -> assertThat(((BusinessException) err).getErrorCode())
                        .isEqualTo("DOCUMENT_STORAGE_ERROR"));
    }

    @Test
    void ensureObjectExists_whenBusinessException_shouldRethrow() throws Throwable {
        BusinessException original = new BusinessException(
                "DOCUMENT_FILE_MISSING", "Fichier absent", HttpStatus.NOT_FOUND);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(original);

        assertThatThrownBy(() -> aspect.ensureObjectExists(joinPoint)).isSameAs(original);
        verify(joinPoint, never()).proceed();
    }

    @Test
    void ensureObjectExists_whenGenericException_shouldBeBadGateway() throws Throwable {
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> aspect.ensureObjectExists(joinPoint))
                .isInstanceOf(BusinessException.class)
                .satisfies(err -> {
                    BusinessException be = (BusinessException) err;
                    assertThat(be.getErrorCode()).isEqualTo("DOCUMENT_STORAGE_ERROR");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(be.getMessage()).contains("Impossible de vérifier");
                });
        verify(joinPoint, never()).proceed();
    }
}
