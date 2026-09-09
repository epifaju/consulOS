package com.consulos.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(handler, "activeProfile", "dev");
    }

    @Test
    void handleBusinessException_shouldPropagateStatusAndErrorCode() {
        BusinessException ex = new BusinessException(
                "DOSSIER_NOT_FOUND", "Dossier introuvable", HttpStatus.NOT_FOUND);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = handler.handleBusinessException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().errorCode()).isEqualTo("DOSSIER_NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("Dossier introuvable");
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    void handleValidationException_shouldMapFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "ne doit pas être vide"));
        bindingResult.addError(new FieldError("request", "nom", "taille invalide"));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiResponse<Map<String, String>>> response = handler.handleValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Erreur de validation");
        assertThat(response.getBody().data())
                .containsEntry("email", "ne doit pas être vide")
                .containsEntry("nom", "taille invalide");
    }

    @Test
    void handleAccessDeniedException_shouldReturnForbidden() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAccessDeniedException(new AccessDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("ACCESS_DENIED");
        assertThat(response.getBody().message()).isEqualTo("Accès refusé");
    }

    @Test
    void handleGenericException_shouldReturnInternalError() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleGenericException(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Une erreur interne est survenue");
    }

    @Test
    void handleGenericException_inProd_shouldStillHideDetails() {
        ReflectionTestUtils.setField(handler, "activeProfile", "prod");

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleGenericException(new IllegalStateException("secret stack"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).doesNotContain("secret");
        assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_ERROR");
    }
}
