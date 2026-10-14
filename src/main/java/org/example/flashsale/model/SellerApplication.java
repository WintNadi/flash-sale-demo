package org.example.flashsale.model;

import java.time.LocalDateTime;

public class SellerApplication {

    private final Long id;
    private final Long userId;
    private final String businessName;
    private final String description;
    private ApplicationStatus status;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String rejectionReason;
    private final LocalDateTime createdAt;

    // Constructor for new applications
    public SellerApplication(Long userId, String businessName, String description) {
        this.id = null;
        this.userId = userId;
        this.businessName = businessName;
        this.description = description;
        this.status = ApplicationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    // Full constructor for DB reads
    public SellerApplication(Long id, Long userId, String businessName, String description,
                             ApplicationStatus status, Long reviewedBy, LocalDateTime reviewedAt,
                             String rejectionReason, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.businessName = businessName;
        this.description = description;
        this.status = status;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt;
    }

    // Domain behavior — enforce the state machine
    public void approve(Long adminUserId) {
        if (status != ApplicationStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot approve application in status " + status);
        }
        this.status = ApplicationStatus.APPROVED;
        this.reviewedBy = adminUserId;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject(Long adminUserId, String reason) {
        if (status != ApplicationStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot reject application in status " + status);
        }
        this.status = ApplicationStatus.REJECTED;
        this.reviewedBy = adminUserId;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getBusinessName() { return businessName; }
    public String getDescription() { return description; }
    public ApplicationStatus getStatus() { return status; }
    public Long getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "SellerApplication{id=" + id + ", userId=" + userId
                + ", business=" + businessName + ", status=" + status + "}";
    }
}
