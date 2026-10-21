package com.myciti.repository;

import com.myciti.model.ProfileReport;
import com.myciti.model.ProfileReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface ProfileReportRepository extends JpaRepository<ProfileReport, Long> {

    Page<ProfileReport> findByStatusOrderByCreatedAtDesc(ProfileReportStatus status, Pageable pageable);

    long countByProfileIdAndCreatedAtAfter(Long profileId, Instant after);

    long countByReporterUserIdAndProfileIdAndCreatedAtAfter(Long reporterUserId, Long profileId, Instant after);
}
