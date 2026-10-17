package com.consulos.document.storage;

import com.consulos.common.BusinessException;
import com.consulos.document.DocumentEntity;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentStorageReconciliationServiceTest {

    @Mock
    private DocumentStorageReconciliationRepository repository;
    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private DocumentStorageReconciliationService service;

    private UUID organisationId;

    @BeforeEach
    void setUp() {
        organisationId = UUID.fromString("ae106c40-69e1-438b-ab7c-605bc85c9f56");
    }

    @Test
    void reconcile_shouldSplitPresentMissingAndOrphans() throws Exception {
        String presentKey = "orgs/" + organisationId + "/dossiers/d1/ok.pdf";
        String missingKey = "orgs/" + organisationId + "/dossiers/d2/gone.pdf";
        String orphanKey = "orgs/" + organisationId + "/dossiers/d3/orphan.pdf";

        DocumentEntity present = document("doc-present", "ok.pdf", presentKey);
        DocumentEntity missing = document("doc-missing", "gone.pdf", missingKey);
        when(repository.findByOrganisationIdAndActifTrue(organisationId))
                .thenReturn(List.of(present, missing));
        Result<Item> presentResult = result(presentKey);
        Result<Item> orphanResult = result(orphanKey);
        when(minioClient.listObjects(any(ListObjectsArgs.class)))
                .thenReturn(List.of(presentResult, orphanResult));

        var report = service.reconcile(organisationId);

        assertThat(report.totalActifs()).isEqualTo(2);
        assertThat(report.presentsDansMinio()).isEqualTo(1);
        assertThat(report.absentsDeMinio()).isEqualTo(1);
        assertThat(report.orphelinsMinio()).isEqualTo(1);
        assertThat(report.fichiersManquants()).hasSize(1);
        assertThat(report.fichiersManquants().getFirst().nomOriginal()).isEqualTo("gone.pdf");
        assertThat(report.clesOrphelines()).containsExactly(orphanKey);
    }

    @Test
    void reconcile_whenMinioFails_shouldThrowStorageError() {
        when(repository.findByOrganisationIdAndActifTrue(organisationId)).thenReturn(List.of());
        when(minioClient.listObjects(any(ListObjectsArgs.class)))
                .thenThrow(new RuntimeException("minio down"));

        assertThatThrownBy(() -> service.reconcile(organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("DOCUMENT_STORAGE_ERROR");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                });
    }

    private static DocumentEntity document(String idSeed, String name, String key) {
        DocumentEntity entity = new DocumentEntity();
        entity.setId(UUID.nameUUIDFromBytes(idSeed.getBytes()));
        entity.setNomOriginal(name);
        entity.setCheminStockage(key);
        entity.setActif(true);
        return entity;
    }

    @SuppressWarnings("unchecked")
    private static Result<Item> result(String objectName) throws Exception {
        Item item = mock(Item.class);
        doReturn(objectName).when(item).objectName();
        doReturn(false).when(item).isDir();
        Result<Item> result = mock(Result.class);
        doReturn(item).when(result).get();
        return result;
    }
}
