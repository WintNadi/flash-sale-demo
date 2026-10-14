package org.example.flashsale.service;

import org.example.flashsale.model.ApplicationStatus;
import org.example.flashsale.model.Role;
import org.example.flashsale.model.SellerApplication;
import org.example.flashsale.model.User;
import org.example.flashsale.repository.SellerApplicationRepository;
import org.example.flashsale.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SellerApplicationService {

    private static final Logger log = LoggerFactory.getLogger(SellerApplicationService.class);

    private final SellerApplicationRepository applicationRepo;
    private final UserRepository userRepo;

    public SellerApplicationService(SellerApplicationRepository applicationRepo,
                                    UserRepository userRepo) {
        this.applicationRepo = applicationRepo;
        this.userRepo = userRepo;
    }

    /**
     * Submit a new seller application.
     * Enforces: user must not already be SELLER/ADMIN, and must not have a pending application.
     */
    public SellerApplication submit(long userId, String businessName, String description) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getRole() == Role.SELLER || user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("You are already a seller");
        }

        if (applicationRepo.findPendingByUserId(userId).isPresent()) {
            throw new IllegalArgumentException("You already have a pending application");
        }

        SellerApplication app = new SellerApplication(userId, businessName, description);
        long id = applicationRepo.insert(app);

        log.info("New seller application submitted: id={}, userId={}, business={}",
                id, userId, businessName);

        return applicationRepo.findById(id)
                .orElseThrow(() -> new IllegalStateException("Application not found after insert"));
    }

    public List<SellerApplication> getApplicationsForUser(long userId) {
        return applicationRepo.findByUserId(userId);
    }

    public SellerApplication getApplication(long id) {
        return applicationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));
    }

    public List<SellerApplication> listApplications(ApplicationStatus statusFilter) {
        return applicationRepo.findAll(statusFilter);
    }

    /**
     * Admin approves an application.
     * Side effect: promotes the applicant's role to SELLER.
     */
    @Transactional
    public SellerApplication approve(long applicationId, long adminUserId) {
        SellerApplication app = getApplication(applicationId);
        app.approve(adminUserId);
        applicationRepo.update(app);

        // Promote the user
        userRepo.updateRole(app.getUserId(), Role.SELLER);

        log.info("Seller application {} APPROVED by admin {}", applicationId, adminUserId);
        return app;
    }

    /**
     * Admin rejects an application with a reason.
     * The user's role stays BUYER; they may re-apply.
     */
    @Transactional
    public SellerApplication reject(long applicationId, long adminUserId, String reason) {
        SellerApplication app = getApplication(applicationId);
        app.reject(adminUserId, reason);
        applicationRepo.update(app);

        log.info("Seller application {} REJECTED by admin {}: {}",
                applicationId, adminUserId, reason);
        return app;
    }
}