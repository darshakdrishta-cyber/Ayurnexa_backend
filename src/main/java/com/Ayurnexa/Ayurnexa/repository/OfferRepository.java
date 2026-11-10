package com.Ayurnexa.Ayurnexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Ayurnexa.Ayurnexa.model.offer.Offer;

public interface OfferRepository extends MongoRepository<Offer, String> {
    Optional<Offer> findByOfferId(String offerId);
    List<Offer> findByIsActiveTrueOrderByOffercreatedDesc();
    List<Offer> findAllByOrderByOffercreatedDesc();
}
