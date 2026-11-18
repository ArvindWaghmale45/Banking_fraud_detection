package com.fraud.backend.dto;

import com.fraud.backend.entity.FraudAlert;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FraudAlertStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private FraudAlert.Status status;

    @Size(max = 2000, message = "Review notes cannot exceed 2000 characters")
    private String reviewNotes;

    public FraudAlert.Status getStatus() {
        return status;
    }

    public void setStatus(FraudAlert.Status status) {
        this.status = status;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }
}
