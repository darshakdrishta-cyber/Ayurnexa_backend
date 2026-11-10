package com.Ayurnexa.Ayurnexa.controller;

import java.util.List;
import java.util.NoSuchElementException;

import com.Ayurnexa.Ayurnexa.Service.OfferService;
import com.Ayurnexa.Ayurnexa.dto.offer.OfferDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Offer Controller - Offers CRUD without JWT requirement.
 */
@RestController
@RequestMapping("/api/offers")
public class OfferController {

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    /**
     * Create offer (no JWT required).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OfferDTO.Response create(@Valid @RequestBody OfferDTO.CreateRequest request) {
        try {
            return offerService.createOffer(request);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    /**
     * Read all offers (defaults to active offers; pass ?all=true to retrieve all).
     */
    @GetMapping
    public List<OfferDTO.Response> list(@RequestParam(value = "all", defaultValue = "false") boolean all) {
        if (all) {
            return offerService.getAllOffers();
        }
        return offerService.getAllActiveOffers();
    }

    /**
     * Read single offer by offerId.
     */
    @GetMapping("/{offerId}")
    public OfferDTO.Response get(@PathVariable String offerId) {
        try {
            return offerService.getOfferById(offerId);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    /**
     * Update offer by offerId.
     */
    @PutMapping("/{offerId}")
    public OfferDTO.Response update(@PathVariable String offerId,
                                    @RequestBody OfferDTO.UpdateRequest request) {
        try {
            return offerService.updateOffer(offerId, request);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    /**
     * Soft delete offer by offerId (changes is_active to false and sets offerdeleted timestamp).
     */
    @DeleteMapping("/{offerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String offerId) {
        try {
            offerService.deleteOffer(offerId);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }
}
