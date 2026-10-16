package com.myciti.repository;

import com.myciti.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByGoogleSub(String googleSub);
    Optional<User> findByEmail(String email);

    @Query(value = """
        SELECT u FROM User u
        WHERE (CAST(:q AS string) IS NULL OR
               LOWER(u.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR
               LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR
               LOWER(u.businessName) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
    """)
    Page<User> searchUsers(@Param("q") String q, Pageable pageable);

    @Query("""
        SELECT u FROM User u
        WHERE u.coarseLat IS NOT NULL AND u.coarseLng IS NOT NULL
          AND u.coarseLat BETWEEN :minLat AND :maxLat
          AND u.coarseLng BETWEEN :minLng AND :maxLng
          AND u.status = com.myciti.model.UserStatus.ACTIVE
    """)
    java.util.List<User> findActiveUsersInBoundingBox(
        @Param("minLat") Double minLat,
        @Param("maxLat") Double maxLat,
        @Param("minLng") Double minLng,
        @Param("maxLng") Double maxLng
    );

    @Query("""
        SELECT u FROM User u
        WHERE u.status = com.myciti.model.UserStatus.ACTIVE
          AND (u.areaName IS NOT NULL AND LOWER(u.areaName) LIKE LOWER(CONCAT('%', :city, '%')))
    """)
    java.util.List<User> findActiveUsersByAreaName(@Param("city") String city);

    @Query("""
        SELECT u FROM User u
        WHERE u.status = com.myciti.model.UserStatus.ACTIVE
    """)
    java.util.List<User> findAllActiveUsers();
}
