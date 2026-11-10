package com.Ayurnexa.Ayurnexa.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import com.Ayurnexa.Ayurnexa.dto.offer.OfferDTO;
import com.Ayurnexa.Ayurnexa.model.offer.Offer;
import com.Ayurnexa.Ayurnexa.repository.OfferRepository;
import org.springframework.stereotype.Service;

@Service
public class OfferService {

    private final OfferRepository offerRepository;

    public OfferService(OfferRepository offerRepository) {
        this.offerRepository = offerRepository;
    }

    public OfferDTO.Response createOffer(OfferDTO.CreateRequest request) {
        Offer offer = new Offer();
        offer.setOffername(request.offername().trim());
        offer.setOfferlink(request.offerlink().trim());
        if (request.additionallink() != null) {
            offer.setAdditionallink(request.additionallink().trim());
        }
        if (request.isActive() != null) {
            offer.setIsActive(request.isActive());
        } else {
            offer.setIsActive(true);
        }

        Offer saved = offerRepository.save(offer);
        return toResponse(saved);
    }

    public List<OfferDTO.Response> getAllActiveOffers() {
        return offerRepository.findByIsActiveTrueOrderByOffercreatedDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OfferDTO.Response> getAllOffers() {
        return offerRepository.findAllByOrderByOffercreatedDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public OfferDTO.Response getOfferById(String offerId) {
        Offer offer = offerRepository.findByOfferId(offerId)
                .or(() -> offerRepository.findById(offerId))
                .orElseThrow(() -> new NoSuchElementException("Offer not found: " + offerId));
        return toResponse(offer);
    }

    public OfferDTO.Response updateOffer(String offerId, OfferDTO.UpdateRequest request) {
        Offer offer = offerRepository.findByOfferId(offerId)
                .or(() -> offerRepository.findById(offerId))
                .orElseThrow(() -> new NoSuchElementException("Offer not found: " + offerId));

        if (request.offername() != null && !request.offername().isBlank()) {
            offer.setOffername(request.offername().trim());
        }
        if (request.offerlink() != null && !request.offerlink().isBlank()) {
            offer.setOfferlink(request.offerlink().trim());
        }
        if (request.additionallink() != null) {
            offer.setAdditionallink(request.additionallink().trim());
        }
        if (request.isActive() != null) {
            offer.setIsActive(request.isActive());
            if (!request.isActive() && offer.getOfferdeleted() == null) {
                offer.setOfferdeleted(Instant.now());
            } else if (request.isActive()) {
                offer.setOfferdeleted(null);
            }
        }

        Offer saved = offerRepository.save(offer);
        return toResponse(saved);
    }

    public void deleteOffer(String offerId) {
        Offer offer = offerRepository.findByOfferId(offerId)
                .or(() -> offerRepository.findById(offerId))
                .orElseThrow(() -> new NoSuchElementException("Offer not found: " + offerId));

        // Soft delete: toggle active state to false and stamp offerdeleted
        offer.setIsActive(false);
        offer.setOfferdeleted(Instant.now());
        offerRepository.save(offer);
    }

    private OfferDTO.Response toResponse(Offer offer) {
        return new OfferDTO.Response(
                offer.getOfferId(),
                offer.getOffername(),
                offer.getOfferlink(),
                offer.getAdditionallink(),
                offer.isActive(),
                offer.getOffercreated(),
                offer.getOfferdeleted(),
                offer.getUpdatedAt()
        );
    }
}
