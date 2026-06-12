package com.consulos.document;

import com.consulos.common.BusinessException;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class MinioStorageServiceTest {

    private static final String ACCESS_KEY = "minioadmin";
    private static final String SECRET_KEY = "minioadmin123";

    @Container
    static final GenericContainer<?> MINIO = new GenericContainer<>(DockerImageName.parse("minio/minio:RELEASE.2024-01-16T16-07-38Z"))
            .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
            .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
            .withCommand("server", "/data")
            .withExposedPorts(9000);

    private MinioStorageService storageService;
    private MinioClient minioClient;

    @BeforeEach
    void setUp() {
        String endpoint = "http://%s:%d".formatted(MINIO.getHost(), MINIO.getMappedPort(9000));
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(ACCESS_KEY, SECRET_KEY)
                .build();
        storageService = new MinioStorageService(minioClient);
        storageService.init();
    }

    @Test
    void uploadFile_validPdf_shouldReturnObjectKeyAndStoreInMinio() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID dossierId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "%PDF-1.4 test content".getBytes()
        );

        String objectKey = storageService.uploadFile(file, orgId, dossierId);

        assertThat(objectKey).contains(orgId.toString()).contains(dossierId.toString());
        minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(MinioStorageService.BUCKET_NAME)
                        .object(objectKey)
                        .build()
        );
    }

    @Test
    void uploadFile_invalidMime_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/x-msdownload",
                "bad".getBytes()
        );

        assertThatThrownBy(() -> storageService.uploadFile(file, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "MIME_NON_AUTORISE")
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadFile_tooLarge_shouldThrow() {
        byte[] large = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.pdf",
                "application/pdf",
                large
        );

        assertThatThrownBy(() -> storageService.uploadFile(file, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "FICHIER_TROP_GRAND")
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void generatePresignedDownloadUrl_shouldBeAccessible() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID dossierId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "doc.pdf",
                "application/pdf",
                "%PDF-1.4".getBytes()
        );
        String objectKey = storageService.uploadFile(file, orgId, dossierId);

        String presignedUrl = storageService.generatePresignedDownloadUrl(objectKey);
        assertThat(presignedUrl).isNotBlank();

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(presignedUrl)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void deleteFile_shouldRemoveFromMinio() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID dossierId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "delete-me.pdf",
                "application/pdf",
                "%PDF-1.4".getBytes()
        );
        String objectKey = storageService.uploadFile(file, orgId, dossierId);

        storageService.deleteFile(objectKey);

        assertThatThrownBy(() -> minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(MinioStorageService.BUCKET_NAME)
                        .object(objectKey)
                        .build()
        )).isNotNull();
    }
}
