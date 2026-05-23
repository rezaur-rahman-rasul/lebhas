package com.lebhas.creativesaas.common.exception;

import com.lebhas.creativesaas.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void shouldMapBusinessExceptionToStandardFailureEnvelope() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/workspaces/test/storage-files/test");

        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleBusinessException(
                new BusinessException(ErrorCode.STORAGE_FILE_NOT_FOUND),
                request);

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.STORAGE_FILE_NOT_FOUND.httpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("Storage file not found");
        assertThat(response.getBody().errors())
                .singleElement()
                .satisfies(error -> {
                    assertThat(error.code()).isEqualTo("STORAGE-404-01");
                    assertThat(error.message()).isEqualTo("Storage file not found");
                });
    }

    @Test
    void shouldMapKafkaPublishingExceptionToStandardFailureEnvelope() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/events/test");

        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleBusinessException(
                new KafkaPublishingException("Failed to publish Kafka event to topic test"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.KAFKA_PUBLISH_FAILED.httpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors().getFirst().code()).isEqualTo("KAFKA-503-01");
    }

    @Test
    void shouldMapTypeMismatchToValidationFailureEnvelope() {
        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleMethodArgumentTypeMismatch(
                new MethodArgumentTypeMismatchException("not-a-uuid", java.util.UUID.class, "assetId", null, new IllegalArgumentException("bad uuid")));

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.httpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().errors())
                .singleElement()
                .satisfies(error -> {
                    assertThat(error.code()).isEqualTo(ErrorCode.VALIDATION_FAILED.code());
                    assertThat(error.field()).isEqualTo("assetId");
                });
    }
}
