package com.myciti.repository;

import com.myciti.model.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Modifying
    @Query(value = """
        INSERT INTO push_subscriptions (user_id, endpoint, p256dh, auth, created_at)
        VALUES (:userId, :endpoint, :p256dh, :auth, CURRENT_TIMESTAMP)
        ON CONFLICT (endpoint)
        DO UPDATE SET
            user_id = EXCLUDED.user_id,
            p256dh = EXCLUDED.p256dh,
            auth = EXCLUDED.auth
        """, nativeQuery = true)
    void upsertSubscription(
        @Param("userId") Long userId,
        @Param("endpoint") String endpoint,
        @Param("p256dh") String p256dh,
        @Param("auth") String auth
    );
}
