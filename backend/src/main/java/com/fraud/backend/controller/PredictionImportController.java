package com.fraud.backend.controller;

import com.fraud.backend.service.PredictionImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/datasets")
public class PredictionImportController {

    private final PredictionImportService predictionImportService;

    public PredictionImportController(PredictionImportService predictionImportService) {
        this.predictionImportService = predictionImportService;
    }

    @PostMapping(
            value = "/{datasetId}/predictions/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, Object>> importPredictions(
            @PathVariable Long datasetId,
            @RequestParam("file") MultipartFile file,
            Principal principal
    ) throws IOException {
        Map<String, Object> result = predictionImportService.importPredictions(
                datasetId,
                principal.getName(),
                file
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
