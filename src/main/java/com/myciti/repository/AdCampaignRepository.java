package com.myciti.repository;

import com.myciti.model.AdCampaign;
import com.myciti.model.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AdCampaignRepository extends JpaRepository<AdCampaign, Long> {

    List<AdCampaign> findByClientUser_IdOrderByCreatedAtDesc(Long clientUserId);

    List<AdCampaign> findAllByOrderByCreatedAtDesc();

    List<AdCampaign> findByStatusOrderByCreatedAtDesc(CampaignStatus status);

    @Query("""
        SELECT DISTINCT c FROM AdCampaign c
        JOIN FETCH c.listing l
        LEFT JOIN FETCH l.images
        WHERE c.status = com.myciti.model.CampaignStatus.APPROVED
          AND :today BETWEEN c.startDate AND c.endDate
        ORDER BY c.createdAt DESC
    """)
    List<AdCampaign> findActiveApprovedCampaigns(@Param("today") LocalDate today);

    @Modifying
    @Query("UPDATE AdCampaign c SET c.impressionsCount = c.impressionsCount + :count WHERE c.id = :id")
    void incrementImpressions(@Param("id") Long id, @Param("count") long count);

    @Modifying
    @Query("UPDATE AdCampaign c SET c.clicksCount = c.clicksCount + 1 WHERE c.id = :id")
    void incrementClicks(@Param("id") Long id);
}
