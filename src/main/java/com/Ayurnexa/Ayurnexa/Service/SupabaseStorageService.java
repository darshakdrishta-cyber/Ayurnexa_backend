package com.Ayurnexa.Ayurnexa.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SupabaseStorageService {

    private final RestTemplate restTemplate;
    private final String url;
    private final String serviceRoleKey;
    private final String anonKey;
    private final String bucket;

    public SupabaseStorageService(
            @Value("${supabase.url:}") String url,
            @Value("${supabase.service-role-key:}") String serviceRoleKey,
            @Value("${supabase.anon-key:}") String anonKey,
            @Value("${supabase.bucket:products}") String bucket) {
        this.url = url;
        this.serviceRoleKey = serviceRoleKey;
        this.anonKey = anonKey;
        this.bucket = bucket;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        this.restTemplate = new RestTemplate(factory);
    }

    public UploadResult upload(MultipartFile file) {
        if (url.isBlank() || serviceRoleKey.isBlank()) {
            throw new IllegalStateException("Supabase storage is not configured (SUPABASE_URL / SUPABASE_SECRET_KEY)");
        }

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String path = "products/" + java.util.UUID.randomUUID() + ext;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(serviceRoleKey);
        headers.set("apikey", anonKey);
        MediaType contentType = file.getContentType() != null
                ? MediaType.parseMediaType(file.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        headers.setContentType(contentType);
        headers.set("x-upsert", "true");
        headers.set("Cache-Control", "3600");

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not read uploaded file: " + exception.getMessage(), exception);
        }

        try {
            restTemplate.put(
                    url + "/storage/v1/object/" + bucket + "/" + path,
                    new HttpEntity<>(bytes, headers));
        } catch (HttpStatusCodeException exception) {
            throw new IllegalStateException(
                    "Supabase upload failed (" + exception.getStatusCode() + "): "
                            + exception.getResponseBodyAsString(),
                    exception);
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "Could not reach Supabase storage: " + exception.getMessage(),
                    exception);
        }

        String publicUrl = url + "/storage/v1/object/public/" + bucket + "/" + path;
        return new UploadResult(publicUrl, path, bucket);
    }

    public record UploadResult(String url, String path, String bucket) {}
}
