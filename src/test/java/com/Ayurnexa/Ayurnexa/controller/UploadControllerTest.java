package com.Ayurnexa.Ayurnexa.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import com.Ayurnexa.Ayurnexa.Service.SupabaseStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class UploadControllerTest {

    @Test
    void storageFailureReturnsOriginalCauseInUploadResponse() {
        SupabaseStorageService storageService = failingStorageService(
                new IllegalStateException(
                        "Supabase S3 upload failed: Access Denied",
                        new IllegalArgumentException("Invalid access key")));
        UploadController controller = new UploadController(storageService);

        var response = controller.upload(validImage(), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("error")).isEqualTo("UPLOAD_FAILED");
        assertThat(body.get("message")).isEqualTo("Supabase S3 upload failed: Access Denied");
        assertThat(body.get("details")).isEqualTo("Invalid access key");
    }

    @Test
    void missingStorageConfigurationReturnsServiceUnavailable() {
        SupabaseStorageService storageService = failingStorageService(
                new IllegalStateException("Supabase S3 is not configured"));
        UploadController controller = new UploadController(storageService);

        var response = controller.upload(validImage(), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("error")).isEqualTo("UPLOAD_NOT_CONFIGURED");
        assertThat(body.get("message")).isEqualTo("Supabase S3 is not configured");
    }

    private static SupabaseStorageService failingStorageService(RuntimeException failure) {
        return new SupabaseStorageService(null, "", "", "", "", "") {
            @Override
            public UploadResult upload(MultipartFile file, String ownerEmail) {
                throw failure;
            }
        };
    }

    private static MockMultipartFile validImage() {
        return new MockMultipartFile("file", "image.png", "image/png", new byte[] {1});
    }
}
