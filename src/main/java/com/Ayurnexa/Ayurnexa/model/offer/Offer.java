package com.Ayurnexa.Ayurnexa.model.offer;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "offers")
public class Offer {

    @Id
    private String id;

    private String offerId = UUID.randomUUID().toString();

    @Field("offername")
    private String offername;

    @Field("offerlink")
    private String offerlink;

    @Field("additionallink")
    private String additionallink;

    @Field("is_active")
    private boolean isActive = true;

    @Field("offercreated")
    @CreatedDate
    private Instant offercreated;

    @Field("offerdeleted")
    private Instant offerdeleted;

    @LastModifiedDate
    private Instant updatedAt;

    public Offer() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOfferId() { return offerId; }
    public void setOfferId(String offerId) { this.offerId = offerId; }

    public String getOffername() { return offername; }
    public void setOffername(String offername) { this.offername = offername; }

    public String getOfferlink() { return offerlink; }
    public void setOfferlink(String offerlink) { this.offerlink = offerlink; }

    public String getAdditionallink() { return additionallink; }
    public void setAdditionallink(String additionallink) { this.additionallink = additionallink; }

    public boolean isActive() { return isActive; }
    public void setIsActive(boolean isActive) { this.isActive = isActive; }
    public void setActive(boolean active) { this.isActive = active; }

    public Instant getOffercreated() { return offercreated; }
    public void setOffercreated(Instant offercreated) { this.offercreated = offercreated; }

    public Instant getOfferdeleted() { return offerdeleted; }
    public void setOfferdeleted(Instant offerdeleted) { this.offerdeleted = offerdeleted; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
