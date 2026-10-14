package org.example.flashsale.api;

import jakarta.validation.Valid;
import org.example.flashsale.api.dto.RejectApplicationRequest;
import org.example.flashsale.api.dto.SellerApplicationDto;
import org.example.flashsale.model.ApplicationStatus;
import org.example.flashsale.model.SellerApplication;
import org.example.flashsale.model.User;
import org.example.flashsale.repository.UserRepository;
import org.example.flashsale.security.UserPrincipal;
import org.example.flashsale.service.SellerApplicationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/seller-applications")
public class AdminSellerApplicationController {

    private final SellerApplicationService service;
    private final UserRepository userRepo;

    public AdminSellerApplicationController(SellerApplicationService service,
                                            UserRepository userRepo) {
        this.service = service;
        this.userRepo = userRepo;
    }

    @GetMapping
    public List<SellerApplicationDto> list(@RequestParam(required = false) String status) {
        ApplicationStatus filter = status == null
                ? null
                : ApplicationStatus.valueOf(status.toUpperCase());

        return service.listApplications(filter)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping("/{id}/approve")
    public SellerApplicationDto approve(@PathVariable long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        SellerApplication app = service.approve(id, principal.getUserId());
        return toDto(app);
    }

    @PostMapping("/{id}/reject")
    public SellerApplicationDto reject(@PathVariable long id,
                                       @Valid @RequestBody RejectApplicationRequest request,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        SellerApplication app = service.reject(id, principal.getUserId(), request.reason());
        return toDto(app);
    }

    private SellerApplicationDto toDto(SellerApplication app) {
        // Fetch user email for admin context
        String email = userRepo.findById(app.getUserId())
                .map(User::getEmail)
                .orElse("(deleted user)");

        return new SellerApplicationDto(
                app.getId(),
                app.getUserId(),
                email,
                app.getBusinessName(),
                app.getDescription(),
                app.getStatus().name(),
                app.getReviewedBy(),
                app.getReviewedAt(),
                app.getRejectionReason(),
                app.getCreatedAt()
        );
    }
}