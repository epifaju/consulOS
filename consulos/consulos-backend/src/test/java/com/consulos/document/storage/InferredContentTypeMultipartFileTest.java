package com.consulos.document.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

class InferredContentTypeMultipartFileTest {

    @ParameterizedTest
    @CsvSource({
            "scan.pdf,application/pdf",
            "SCAN.PDF,application/pdf",
            "photo.png,image/png",
            "photo.jpg,image/jpeg",
            "photo.jpeg,image/jpeg",
            "photo.webp,image/webp"
    })
    void withInferredType_shouldInferFromExtensionWhenTypeBlank(String filename, String expectedType) {
        MockMultipartFile raw = new MockMultipartFile("file", filename, "", "x".getBytes());

        MultipartFile wrapped = InferredContentTypeMultipartFile.withInferredType(raw);

        assertThat(wrapped.getContentType()).isEqualTo(expectedType);
        assertThat(wrapped.getOriginalFilename()).isEqualTo(filename);
        assertThat(wrapped.getName()).isEqualTo("file");
        assertThat(wrapped.getSize()).isEqualTo(1);
        assertThat(wrapped.isEmpty()).isFalse();
    }

    @Test
    void withInferredType_shouldKeepExistingType() {
        MockMultipartFile raw = new MockMultipartFile("file", "scan.bin", "application/octet-stream", "x".getBytes());

        MultipartFile result = InferredContentTypeMultipartFile.withInferredType(raw);

        assertThat(result).isSameAs(raw);
        assertThat(result.getContentType()).isEqualTo("application/octet-stream");
    }

    @Test
    void withInferredType_shouldReturnOriginalWhenExtensionUnknown() {
        MockMultipartFile raw = new MockMultipartFile("file", "notes.bin", "", "x".getBytes());

        MultipartFile result = InferredContentTypeMultipartFile.withInferredType(raw);

        assertThat(result).isSameAs(raw);
    }

    @Test
    void withInferredType_shouldReturnOriginalWhenFilenameMissing() {
        MockMultipartFile raw = new MockMultipartFile("file", "", "", "x".getBytes());

        MultipartFile result = InferredContentTypeMultipartFile.withInferredType(raw);

        assertThat(result).isSameAs(raw);
    }

    @Test
    void withInferredType_shouldReturnNullWhenFileNull() {
        assertThat(InferredContentTypeMultipartFile.withInferredType(null)).isNull();
    }
}
