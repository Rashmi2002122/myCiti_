package com.myciti.repository;

import com.myciti.model.AdReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdReportRepository extends JpaRepository<AdReport, Long> {
    List<AdReport> findAllByOrderByCreatedAtDesc();
    List<AdReport> findByCampaign_IdOrderByCreatedAtDesc(Long campaignId);
}
