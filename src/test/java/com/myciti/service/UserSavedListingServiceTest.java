package com.myciti.service;

import com.myciti.dto.ListingResponse;
import com.myciti.exception.ResourceNotFoundException;
import com.myciti.model.Category;
import com.myciti.model.Listing;
import com.myciti.model.Status;
import com.myciti.model.User;
import com.myciti.model.UserSavedListing;
import com.myciti.repository.ListingRepository;
import com.myciti.repository.UserRepository;
import com.myciti.repository.UserSavedListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserSavedListingServiceTest {

    @Mock
    private UserSavedListingRepository userSavedListingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ListingService listingService;

    private UserSavedListingService userSavedListingService;

    private User testUser;
    private Listing testListing;

    @BeforeEach
    void setUp() {
        userSavedListingService = new UserSavedListingService(
            userSavedListingRepository,
            userRepository,
            listingRepository,
            listingService
        );

        testUser = new User("sub-1", "user@test.com", "Test User", null);
        testUser.setId(1L);

        testListing = new Listing();
        testListing.setId(10L);
        testListing.setTitle("Park Street Pandal");
        testListing.setCategory(Category.PANDAL);
        testListing.setStatus(Status.PUBLISHED);
    }

    @Test
    void saveListing_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(listingRepository.findById(10L)).thenReturn(Optional.of(testListing));
        when(userSavedListingRepository.existsByUserIdAndListingId(1L, 10L)).thenReturn(false);

        boolean result = userSavedListingService.saveListing(1L, 10L);

        assertTrue(result);
        verify(userSavedListingRepository).save(any(UserSavedListing.class));
    }

    @Test
    void saveListing_alreadySaved_doesNotDuplicate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(listingRepository.findById(10L)).thenReturn(Optional.of(testListing));
        when(userSavedListingRepository.existsByUserIdAndListingId(1L, 10L)).thenReturn(true);

        boolean result = userSavedListingService.saveListing(1L, 10L);

        assertTrue(result);
        verify(userSavedListingRepository, never()).save(any(UserSavedListing.class));
    }

    @Test
    void saveListing_listingNotFound_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(listingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            userSavedListingService.saveListing(1L, 999L)
        );
    }

    @Test
    void unsaveListing_callsDelete() {
        boolean result = userSavedListingService.unsaveListing(1L, 10L);
        assertFalse(result);
        verify(userSavedListingRepository).deleteByUserIdAndListingId(1L, 10L);
    }

    @Test
    void isSaved_returnsRepositoryStatus() {
        when(userSavedListingRepository.existsByUserIdAndListingId(1L, 10L)).thenReturn(true);
        assertTrue(userSavedListingService.isSaved(1L, 10L));

        when(userSavedListingRepository.existsByUserIdAndListingId(1L, 20L)).thenReturn(false);
        assertFalse(userSavedListingService.isSaved(1L, 20L));
    }

    @Test
    void getSavedListings_returnsMappedListings() {
        when(userSavedListingRepository.findSavedListingsByUserId(1L)).thenReturn(List.of(testListing));

        ListingResponse response = new ListingResponse();
        response.setId(10L);
        response.setTitle("Park Street Pandal");
        when(listingService.toResponse(testListing)).thenReturn(response);

        List<ListingResponse> saved = userSavedListingService.getSavedListings(1L);

        assertEquals(1, saved.size());
        assertEquals(10L, saved.get(0).getId());
        assertEquals("Park Street Pandal", saved.get(0).getTitle());
    }

    @Test
    void countSaved_returnsCount() {
        when(userSavedListingRepository.countByUserId(1L)).thenReturn(7L);
        assertEquals(7L, userSavedListingService.countSaved(1L));
    }
}
