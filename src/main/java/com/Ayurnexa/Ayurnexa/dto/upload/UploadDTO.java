package com.Ayurnexa.Ayurnexa.dto.upload;

import java.time.Instant;

public final class UploadDTO {

    private UploadDTO() {}

    public record UploadResponse(
            String id,
            String url,
            String path,
            String bucket,
            String contentType,
            long size,
            Instant createdAt) {}
}
