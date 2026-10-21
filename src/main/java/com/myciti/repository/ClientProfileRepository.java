package com.myciti.repository;

import com.myciti.model.ClientProfile;
import com.myciti.model.ClientProfileStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientProfileRepository extends JpaRepository<ClientProfile, Long> {

    Optional<ClientProfile> findBySlug(String slug);

    Optional<ClientProfile> findByUserId(Long userId);

    boolean existsBySlug(String slug);

    boolean existsByUserId(Long userId);

    @Query("SELECT cp FROM ClientProfile cp WHERE " +
           "(:verified IS NULL OR cp.verified = :verified) AND " +
           "(:q IS NULL OR :q = '' OR " +
           "LOWER(cp.businessName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(cp.slug) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<ClientProfile> searchAdminProfiles(
        @Param("q") String query,
        @Param("verified") Boolean verified,
        Pageable pageable
    );
}
