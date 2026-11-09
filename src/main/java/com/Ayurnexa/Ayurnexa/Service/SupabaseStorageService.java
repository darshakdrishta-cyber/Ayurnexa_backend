package com.Ayurnexa.Ayurnexa.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import com.Ayurnexa.Ayurnexa.model.upload.StoredUpload;
import com.Ayurnexa.Ayurnexa.repository.UploadRepository;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class SupabaseStorageService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final UploadRepository uploadRepository;
    private final String endpoint;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String region;
    private final String bucket;
    private volatile S3Client s3Client;

    public SupabaseStorageService(
            UploadRepository uploadRepository,
            @Value("${supabase.s3.endpoint:}") String endpoint,
            @Value("${supabase.s3.access-key-id:}") String accessKeyId,
            @Value("${supabase.s3.secret-access-key:}") String secretAccessKey,
            @Value("${supabase.s3.region:}") String region,
            @Value("${supabase.bucket:products}") String bucket) {
        this.uploadRepository = uploadRepository;
        this.endpoint = endpoint;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.region = region;
        this.bucket = bucket;
    }

    public UploadResult upload(MultipartFile file, String ownerEmail) {
        S3Client client = getS3Client();
        String objectKey = "Products/" + UUID.randomUUID();
        String publicAccessToken = createPublicAccessToken();
        String contentType = file.getContentType() == null || file.getContentType().isBlank()
                ? "application/octet-stream"
                : file.getContentType();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read uploaded file", exception);
        }

        try {
            client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(bytes));
        } catch (S3Exception | SdkClientException exception) {
            throw new IllegalStateException("Supabase S3 upload failed: " + exception.getMessage(), exception);
        }

        StoredUpload upload = new StoredUpload(
                bucket,
                objectKey,
                file.getOriginalFilename(),
                contentType,
                bytes.length,
                ownerEmail,
                Instant.now(),
                hashPublicAccessToken(publicAccessToken));
        try {
            StoredUpload saved = uploadRepository.save(upload);
            return new UploadResult(saved, publicAccessToken);
        } catch (RuntimeException databaseException) {
            try {
                client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
            } catch (S3Exception | SdkClientException cleanupException) {
                databaseException.addSuppressed(cleanupException);
            }
            throw new IllegalStateException("File uploaded but its metadata could not be saved to MongoDB",
                    databaseException);
        }
    }

    public DownloadResult download(String id, String ownerEmail) {
        StoredUpload upload = uploadRepository.findByIdAndOwnerEmail(id, ownerEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Uploaded file not found"));
        return download(upload);
    }

    public DownloadResult downloadPublic(String publicAccessToken) {
        StoredUpload upload = uploadRepository.findByPublicAccessTokenHash(hashPublicAccessToken(publicAccessToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Uploaded file not found"));
        return download(upload);
    }

    private static String hashPublicAccessToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private DownloadResult download(StoredUpload upload) {
        try {
            ResponseBytes<GetObjectResponse> response = getS3Client().getObjectAsBytes(
                    GetObjectRequest.builder().bucket(upload.getBucket()).key(upload.getObjectKey()).build());
            return new DownloadResult(upload, response.asByteArray());
        } catch (S3Exception | SdkClientException exception) {
            throw new IllegalStateException("Supabase S3 download failed: " + exception.getMessage(), exception);
        }
    }

    private static String createPublicAccessToken() {
        byte[] token = new byte[32];
        SECURE_RANDOM.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    private S3Client getS3Client() {
        if (endpoint.isBlank() || accessKeyId.isBlank() || secretAccessKey.isBlank() || region.isBlank()) {
            throw new IllegalStateException(
                    "Supabase S3 is not configured (SUPABASE_S3_ACCESS_KEY_ID, "
                            + "SUPABASE_S3_SECRET_ACCESS_KEY, SUPABASE_S3_REGION, SUPABASE_URL)");
        }

        S3Client client = s3Client;
        if (client == null) {
            synchronized (this) {
                client = s3Client;
                if (client == null) {
                    client = S3Client.builder()
                            .endpointOverride(URI.create(endpoint))
                            .region(Region.of(region))
                            .credentialsProvider(StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                            .serviceConfiguration(S3Configuration.builder()
                                    .pathStyleAccessEnabled(true)
                                    .build())
                            .build();
                    s3Client = client;
                }
            }
        }
        return client;
    }

    @PreDestroy
    public void closeS3Client() {
        S3Client client = s3Client;
        if (client != null) {
            client.close();
        }
    }

    public record UploadResult(StoredUpload upload, String publicAccessToken) {}

    public record DownloadResult(StoredUpload upload, byte[] bytes) {}
}
