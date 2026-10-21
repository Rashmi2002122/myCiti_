package com.myciti.repository;

import com.myciti.model.Listing;
import com.myciti.model.UserSavedListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSavedListingRepository extends JpaRepository<UserSavedListing, Long> {

    long countByUserId(Long userId);

    boolean existsByUserIdAndListingId(Long userId, Long listingId);

    Optional<UserSavedListing> findByUserIdAndListingId(Long userId, Long listingId);

    void deleteByUserIdAndListingId(Long userId, Long listingId);

    List<UserSavedListing> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT s.listing FROM UserSavedListing s LEFT JOIN FETCH s.listing.images WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    List<Listing> findSavedListingsByUserId(@Param("userId") Long userId);
}
