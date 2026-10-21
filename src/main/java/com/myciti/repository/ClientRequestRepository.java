package com.myciti.repository;

import com.myciti.model.ClientRequest;
import com.myciti.model.ClientRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRequestRepository extends JpaRepository<ClientRequest, Long> {

    Optional<ClientRequest> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ClientRequest> findByUserIdAndStatus(Long userId, ClientRequestStatus status);

    boolean existsByUserIdAndStatus(Long userId, ClientRequestStatus status);

    long countByStatus(ClientRequestStatus status);

    Page<ClientRequest> findByStatusOrderByCreatedAtDesc(ClientRequestStatus status, Pageable pageable);

    Page<ClientRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
