package com.fraud.backend.controller;

import com.fraud.backend.dto.FraudAlertResponse;
import com.fraud.backend.dto.FraudAlertStatusUpdateRequest;
import com.fraud.backend.entity.FraudAlert;
import com.fraud.backend.service.FraudAlertService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/fraud-alerts")
public class FraudAlertController {

    private final FraudAlertService fraudAlertService;

    public FraudAlertController(FraudAlertService fraudAlertService) {
        this.fraudAlertService = fraudAlertService;
    }

    @GetMapping
    public ResponseEntity<Page<FraudAlertResponse>> getAlerts(
            @RequestParam(required = false) FraudAlert.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<FraudAlertResponse> response = fraudAlertService
                .getAlerts(status, pageable)
                .map(FraudAlertResponse::new);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FraudAlertResponse> getAlertById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new FraudAlertResponse(fraudAlertService.getAlertById(id))
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<FraudAlertResponse> updateAlertStatus(
            @PathVariable Long id,
            @Valid @RequestBody FraudAlertStatusUpdateRequest request
    ) {
        FraudAlert updatedAlert = fraudAlertService.updateAlert(
                id,
                request.getStatus(),
                request.getReviewNotes()
        );

        return ResponseEntity.ok(new FraudAlertResponse(updatedAlert));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Long>> getAlertSummary() {
        Map<String, Long> summary = new HashMap<>();
        summary.put("totalOpen", fraudAlertService.countByStatus(FraudAlert.Status.OPEN));
        summary.put("totalReviewing", fraudAlertService.countByStatus(FraudAlert.Status.REVIEWING));
        summary.put("totalResolved", fraudAlertService.countByStatus(FraudAlert.Status.RESOLVED));
        summary.put("totalDismissed", fraudAlertService.countByStatus(FraudAlert.Status.DISMISSED));
        summary.put("critical", fraudAlertService.countBySeverity(FraudAlert.Severity.CRITICAL));
        summary.put("high", fraudAlertService.countBySeverity(FraudAlert.Severity.HIGH));
        summary.put("medium", fraudAlertService.countBySeverity(FraudAlert.Severity.MEDIUM));
        summary.put("low", fraudAlertService.countBySeverity(FraudAlert.Severity.LOW));

        return ResponseEntity.ok(summary);
    }
}
