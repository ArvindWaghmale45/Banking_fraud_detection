package com.fraud.backend.controller;

import com.fraud.backend.dto.ModelMetricImportRequest;
import com.fraud.backend.dto.ModelMetricResponse;
import com.fraud.backend.service.ModelMetricService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/model-metrics")
public class ModelMetricController {

    private final ModelMetricService service;

    public ModelMetricController(ModelMetricService service) {
        this.service = service;
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public ModelMetricResponse importMetrics(@Valid @RequestBody ModelMetricImportRequest request) {
        return service.importMetrics(request);
    }

    @GetMapping
    public List<ModelMetricResponse> getAllMetrics() {
        return service.getAllMetrics();
    }

    @GetMapping("/latest")
    public ResponseEntity<ModelMetricResponse> getLatestMetrics() {
        return service.getLatestMetrics()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
