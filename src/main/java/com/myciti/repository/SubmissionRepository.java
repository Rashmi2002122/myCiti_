package com.myciti.repository;

import com.myciti.model.Submission;
import com.myciti.model.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Submission> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, SubmissionStatus status);

    Optional<Submission> findByIdAndUserId(Long id, Long userId);

    Optional<Submission> findByListingId(Long listingId);

    long countByUserIdAndStatus(Long userId, SubmissionStatus status);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.user.id = :userId AND s.createdAt >= :after")
    long countSubmissionsSince(@Param("userId") Long userId, @Param("after") Instant after);

    List<Submission> findByStatusOrderByCreatedAtAsc(SubmissionStatus status);

    List<Submission> findAllByOrderByCreatedAtAsc();

    long countByStatus(SubmissionStatus status);

    List<Submission> findByEndDateBefore(java.time.LocalDate date);
}
