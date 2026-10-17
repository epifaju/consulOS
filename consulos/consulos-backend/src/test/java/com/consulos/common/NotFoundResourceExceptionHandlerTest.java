package com.consulos.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class NotFoundResourceExceptionHandlerTest {

    @Test
    void handleNoResource_shouldReturnNotFoundNotInternalError() {
        NotFoundResourceExceptionHandler handler = new NotFoundResourceExceptionHandler();
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.POST, "/documents/x/replace");

        ResponseEntity<ApiResponse<Void>> response = handler.handleNoResource(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("Ressource introuvable");
    }

    @Test
    void handleUnsupportedMediaType_shouldReturn415NotInternalError() {
        NotFoundResourceExceptionHandler handler = new NotFoundResourceExceptionHandler();
        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(
                MediaType.APPLICATION_JSON,
                java.util.List.of(MediaType.MULTIPART_FORM_DATA));

        ResponseEntity<ApiResponse<Void>> response = handler.handleUnsupportedMediaType(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("UNSUPPORTED_MEDIA_TYPE");
    }
}
