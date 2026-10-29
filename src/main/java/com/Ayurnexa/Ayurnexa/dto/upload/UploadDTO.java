package com.Ayurnexa.Ayurnexa.dto.upload;

public final class UploadDTO {

    private UploadDTO() {}

    public record UploadResponse(String url, String path, String bucket) {}
}
