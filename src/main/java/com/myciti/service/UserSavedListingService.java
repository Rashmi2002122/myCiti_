package com.myciti.service;

import com.myciti.dto.ListingResponse;
import com.myciti.exception.ResourceNotFoundException;
import com.myciti.model.Listing;
import com.myciti.model.User;
import com.myciti.model.UserSavedListing;
import com.myciti.repository.ListingRepository;
import com.myciti.repository.UserRepository;
import com.myciti.repository.UserSavedListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserSavedListingService {

    private final UserSavedListingRepository userSavedListingRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final ListingService listingService;

    public UserSavedListingService(
        UserSavedListingRepository userSavedListingRepository,
        UserRepository userRepository,
        ListingRepository listingRepository,
        ListingService listingService
    ) {
        this.userSavedListingRepository = userSavedListingRepository;
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.listingService = listingService;
    }

    @Transactional
    public boolean saveListing(Long userId, Long listingId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User #" + userId + " not found"));

        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new ResourceNotFoundException("Listing #" + listingId + " not found"));

        if (!userSavedListingRepository.existsByUserIdAndListingId(userId, listingId)) {
            UserSavedListing savedListing = new UserSavedListing(user, listing);
            userSavedListingRepository.save(savedListing);
        }
        return true;
    }

    @Transactional
    public boolean unsaveListing(Long userId, Long listingId) {
        userSavedListingRepository.deleteByUserIdAndListingId(userId, listingId);
        return false;
    }

    @Transactional(readOnly = true)
    public boolean isSaved(Long userId, Long listingId) {
        return userSavedListingRepository.existsByUserIdAndListingId(userId, listingId);
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> getSavedListings(Long userId) {
        List<Listing> listings = userSavedListingRepository.findSavedListingsByUserId(userId);
        return listings.stream()
            .map(listingService::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countSaved(Long userId) {
        return userSavedListingRepository.countByUserId(userId);
    }
}
