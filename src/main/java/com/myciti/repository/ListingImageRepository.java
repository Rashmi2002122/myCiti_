package com.myciti.repository;

import com.myciti.model.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingImageRepository extends JpaRepository<ListingImage, Long> {
    List<ListingImage> findByListingIdOrderBySortOrderAsc(Long listingId);
    List<ListingImage> findByListingIdInOrderBySortOrderAsc(List<Long> listingIds);
}
