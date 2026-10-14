package org.example.flashsale.api;

import jakarta.validation.Valid;
import org.example.flashsale.api.dto.CreateApplicationRequest;
import org.example.flashsale.api.dto.SellerApplicationDto;
import org.example.flashsale.model.SellerApplication;
import org.example.flashsale.security.UserPrincipal;
import org.example.flashsale.service.SellerApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller-applications")
public class SellerApplicationController {

    private final SellerApplicationService service;

    public SellerApplicationController(SellerApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SellerApplicationDto submit(@Valid @RequestBody CreateApplicationRequest request,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        SellerApplication app = service.submit(
                principal.getUserId(),
                request.businessName(),
                request.description()
        );
        return toDto(app);
    }

    @GetMapping("/me")
    public List<SellerApplicationDto> myApplications(@AuthenticationPrincipal UserPrincipal principal) {
        return service.getApplicationsForUser(principal.getUserId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    private SellerApplicationDto toDto(SellerApplication app) {
        return new SellerApplicationDto(
                app.getId(),
                app.getUserId(),
                null, // email — filled by admin queue, not needed here
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