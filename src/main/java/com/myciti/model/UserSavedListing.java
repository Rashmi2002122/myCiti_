package com.myciti.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "user_saved_listings",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_saved_listing", columnNames = {"user_id", "listing_id"})
    }
)
public class UserSavedListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UserSavedListing() {}

    public UserSavedListing(User user, Listing listing) {
        this.user = user;
        this.listing = listing;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Listing getListing() {
        return listing;
    }

    public void setListing(Listing listing) {
        this.listing = listing;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
