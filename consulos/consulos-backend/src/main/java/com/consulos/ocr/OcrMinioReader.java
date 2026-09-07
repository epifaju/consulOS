package com.consulos.ocr;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.ocr", name = "enabled", havingValue = "true")
public class OcrMinioReader {

    private final MinioClient minioClient;

    @Value("${consulos.minio.bucket:consulos}")
    private String bucket;

    public byte[] readObject(String objectKey) {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) {
            return stream.readAllBytes();
        } catch (Exception ex) {
            log.warn("Lecture MinIO OCR échouée pour {}: {}", objectKey, ex.getMessage());
            return new byte[0];
        }
    }
}
