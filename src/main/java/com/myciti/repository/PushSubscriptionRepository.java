package com.myciti.repository;

import com.myciti.model.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    Optional<PushSubscription> findByEndpoint(String endpoint);
    List<PushSubscription> findByUser_Id(Long userId);
    List<PushSubscription> findByUser_IdIn(Collection<Long> userIds);
    void deleteByUser_Id(Long userId);
    void deleteByEndpoint(String endpoint);
}
