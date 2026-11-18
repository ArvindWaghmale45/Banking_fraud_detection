package com.fraud.backend.controller;

import com.fraud.backend.dto.ManualPredictionRequest;
import com.fraud.backend.dto.ManualPredictionResponse;
import com.fraud.backend.service.ManualPredictionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {
    private final ManualPredictionService manualPredictionService;

    public PredictionController(ManualPredictionService manualPredictionService) {
        this.manualPredictionService = manualPredictionService;
    }

    @PostMapping("/manual")
    @ResponseStatus(HttpStatus.CREATED)
    public ManualPredictionResponse predict(Principal principal, @Valid @RequestBody ManualPredictionRequest request) {
        return manualPredictionService.predict(principal.getName(), request);
    }
}
