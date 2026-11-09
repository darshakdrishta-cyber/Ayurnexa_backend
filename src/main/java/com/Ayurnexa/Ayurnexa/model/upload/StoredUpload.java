package com.Ayurnexa.Ayurnexa.model.upload;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "uploads")
public class StoredUpload {

    @Id
    private String id;

    private String bucket;
    private String objectKey;
    private String originalFilename;
    private String contentType;
    private long size;
    private String ownerEmail;
    private Instant createdAt;
    @Indexed(unique = true, sparse = true)
    private String publicAccessTokenHash;

    public StoredUpload() {}

    public StoredUpload(
            String bucket,
            String objectKey,
            String originalFilename,
            String contentType,
            long size,
            String ownerEmail,
            Instant createdAt,
            String publicAccessTokenHash) {
        this.bucket = bucket;
        this.objectKey = objectKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.size = size;
        this.ownerEmail = ownerEmail;
        this.createdAt = createdAt;
        this.publicAccessTokenHash = publicAccessTokenHash;
    }

    public String getId() { return id; }
    public String getBucket() { return bucket; }
    public String getObjectKey() { return objectKey; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public long getSize() { return size; }
    public String getOwnerEmail() { return ownerEmail; }
    public Instant getCreatedAt() { return createdAt; }
    public String getPublicAccessTokenHash() { return publicAccessTokenHash; }
}
