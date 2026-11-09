package com.Ayurnexa.Ayurnexa.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Ayurnexa.Ayurnexa.model.upload.StoredUpload;

public interface UploadRepository extends MongoRepository<StoredUpload, String> {
    Optional<StoredUpload> findByIdAndOwnerEmail(String id, String ownerEmail);
    Optional<StoredUpload> findByPublicAccessTokenHash(String publicAccessTokenHash);
}
