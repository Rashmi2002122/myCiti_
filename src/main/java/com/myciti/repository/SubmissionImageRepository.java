package com.myciti.repository;

import com.myciti.model.SubmissionImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionImageRepository extends JpaRepository<SubmissionImage, Long> {
    List<SubmissionImage> findBySubmissionIdOrderBySortOrderAsc(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
