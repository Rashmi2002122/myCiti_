package com.myciti.repository;

import com.myciti.model.Category;
import com.myciti.model.Listing;
import com.myciti.model.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {

    @Query("SELECT l FROM Listing l LEFT JOIN FETCH l.images WHERE l.id = :id")
    Optional<Listing> findByIdWithImages(@Param("id") Long id);

    /**
     * Find all listings where the end date is strictly before the given date (event has ended).
     */
    List<Listing> findByEndDateBefore(LocalDate date);

    /**
     * Native query to search active listings with Haversine distance calculation.
     * Earth radius = 6371.0 km
     * Haversine formula: 2 * 6371 * asin(sqrt(sin^2(dlat/2) + cos(lat1)*cos(lat2)*sin^2(dlng/2)))
     */
    @Query(value = """
        SELECT 
            l.id,
            l.title,
            l.category,
            l.description,
            l.address,
            l.landmark,
            l.city,
            l.latitude,
            l.longitude,
            l.start_date,
            l.end_date,
            l.daily_open_time,
            l.daily_close_time,
            l.offer_text,
            l.contact_phone,
            l.status,
            l.created_at,
            l.updated_at,
            (6371.0 * 2.0 * ASIN(SQRT(
                POWER(SIN(RADIANS(l.latitude - :userLat) / 2.0), 2) +
                COS(RADIANS(:userLat)) * COS(RADIANS(l.latitude)) *
                POWER(SIN(RADIANS(l.longitude - :userLng) / 2.0), 2)
            ))) AS distance_km
        FROM listings l
        WHERE l.status = 'PUBLISHED'
          AND :currentDate BETWEEN l.start_date AND l.end_date
          AND (CAST(:city AS VARCHAR) IS NULL OR LOWER(l.city) = LOWER(CAST(:city AS VARCHAR)))
          AND (CAST(:category AS VARCHAR) IS NULL OR l.category = CAST(:category AS VARCHAR))
          AND (
            CAST(:q AS VARCHAR) IS NULL OR
            LOWER(l.title) LIKE LOWER(CONCAT('%', CAST(:q AS VARCHAR), '%')) OR
            LOWER(l.description) LIKE LOWER(CONCAT('%', CAST(:q AS VARCHAR), '%')) OR
            LOWER(l.address) LIKE LOWER(CONCAT('%', CAST(:q AS VARCHAR), '%')) OR
            LOWER(l.landmark) LIKE LOWER(CONCAT('%', CAST(:q AS VARCHAR), '%'))
          )
          AND (
            CAST(:radiusKm AS DOUBLE PRECISION) IS NULL OR
            (6371.0 * 2.0 * ASIN(SQRT(
                POWER(SIN(RADIANS(l.latitude - :userLat) / 2.0), 2) +
                COS(RADIANS(:userLat)) * COS(RADIANS(l.latitude)) *
                POWER(SIN(RADIANS(l.longitude - :userLng) / 2.0), 2)
            ))) <= CAST(:radiusKm AS DOUBLE PRECISION)
          )
        ORDER BY distance_km ASC
        """, nativeQuery = true)
    List<Object[]> findActiveWithDistance(
        @Param("currentDate") LocalDate currentDate,
        @Param("userLat") double userLat,
        @Param("userLng") double userLng,
        @Param("radiusKm") Double radiusKm,
        @Param("category") String category,
        @Param("city") String city,
        @Param("q") String q
    );

    /**
     * Native query when user coordinates are not provided.
     */
    @Query(value = """
        SELECT l.*
        FROM listings l
        WHERE l.status = 'PUBLISHED'
          AND :currentDate BETWEEN l.start_date AND l.end_date
          AND (CAST(:city AS VARCHAR) IS NULL OR LOWER(l.city) = LOWER(CAST(:city AS VARCHAR)))
          AND (CAST(:category AS VARCHAR) IS NULL OR l.category = CAST(:category AS VARCHAR))
          AND (
            CAST(:q AS VARCHAR) IS NULL OR
            LOWER(l.title) LIKE LOWER(CONCAT('%', CAST(:q AS VARCHAR), '%')) OR
            LOWER(l.description) LIKE LOWER(CONCAT('%', CAST(:q AS VARCHAR), '%')) OR
            LOWER(l.address) LIKE LOWER(CONCAT('%', CAST(:q AS VARCHAR), '%')) OR
            LOWER(l.landmark) LIKE LOWER(CONCAT('%', CAST(:q AS VARCHAR), '%'))
          )
        ORDER BY l.start_date ASC, l.title ASC
        """, nativeQuery = true)
    List<Listing> findActiveWithoutDistance(
        @Param("currentDate") LocalDate currentDate,
        @Param("category") String category,
        @Param("city") String city,
        @Param("q") String q
    );

    @Query("SELECT l FROM Listing l WHERE l.clientProfileId = :clientProfileId " +
           "AND l.status = com.myciti.model.Status.PUBLISHED " +
           "AND :currentDate BETWEEN l.startDate AND l.endDate " +
           "ORDER BY l.startDate ASC")
    Page<Listing> findActiveClientListings(
        @Param("clientProfileId") Long clientProfileId,
        @Param("currentDate") LocalDate currentDate,
        Pageable pageable
    );

    @Query("SELECT l FROM Listing l WHERE l.clientProfileId = :clientProfileId " +
           "AND l.status = com.myciti.model.Status.PUBLISHED " +
           "AND l.startDate > :currentDate " +
           "ORDER BY l.startDate ASC")
    Page<Listing> findUpcomingClientListings(
        @Param("clientProfileId") Long clientProfileId,
        @Param("currentDate") LocalDate currentDate,
        Pageable pageable
    );

    long countByClientProfileIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        Long clientProfileId, Status status, LocalDate d1, LocalDate d2
    );

    long countByClientProfileIdAndStatusAndStartDateAfter(
        Long clientProfileId, Status status, LocalDate date
    );
}
