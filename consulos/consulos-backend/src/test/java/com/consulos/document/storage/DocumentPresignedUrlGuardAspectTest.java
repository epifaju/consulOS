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
class DocumentPresignedUrlGuardAspectTest {

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
        document.setCheminStockage("orgs/" + organisationId + "/gone.pdf");
        when(joinPoint.getArgs()).thenReturn(new Object[]{documentId, organisationId});
    }

    @Test
    void ensureObjectExists_whenPresent_shouldProceed() throws Throwable {
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, organisationId))
                .thenReturn(Optional.of(document));
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(aspect.ensureObjectExists(joinPoint)).isEqualTo("ok");
        verify(joinPoint).proceed();
    }

    @Test
    void ensureObjectExists_whenNoSuchKey_shouldNotProceed() throws Throwable {
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, organisationId))
                .thenReturn(Optional.of(document));
        ErrorResponseException ex = mock(ErrorResponseException.class);
        ErrorResponse errorResponse = mock(ErrorResponse.class);
        when(errorResponse.code()).thenReturn("NoSuchKey");
        when(ex.errorResponse()).thenReturn(errorResponse);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(ex);

        assertThatThrownBy(() -> aspect.ensureObjectExists(joinPoint))
                .isInstanceOf(BusinessException.class)
                .satisfies(err -> {
                    BusinessException be = (BusinessException) err;
                    assertThat(be.getErrorCode()).isEqualTo("DOCUMENT_FILE_MISSING");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
        verify(joinPoint, never()).proceed();
    }

    @Test
    void ensureObjectExists_whenDocumentMissing_shouldThrowNotFound() throws Throwable {
        when(documentRepository.findByIdAndOrganisationIdAndActifTrue(documentId, organisationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> aspect.ensureObjectExists(joinPoint))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("DOCUMENT_NOT_FOUND"));
        verify(joinPoint, never()).proceed();
    }
}
