package com.myciti.repository;

import com.myciti.model.Announcement;
import com.myciti.model.AnnouncementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByAuthorUser_IdOrderByCreatedAtDesc(Long authorUserId);
    List<Announcement> findAllByOrderByCreatedAtDesc();
    List<Announcement> findByStatusOrderByCreatedAtDesc(AnnouncementStatus status);
}
