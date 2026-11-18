package com.fraud.backend.controller;

import com.fraud.backend.dto.DatasetResponse;
import com.fraud.backend.service.DatasetService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {

    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DatasetResponse upload(Principal principal, @RequestParam("file") MultipartFile file) {
        return datasetService.upload(principal.getName(), file);
    }

    @GetMapping
    public Page<DatasetResponse> list(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return datasetService.list(
                principal.getName(),
                PageRequest.of(page, Math.min(Math.max(size, 1), 100))
        );
    }

    @GetMapping("/{id}")
    public DatasetResponse get(Principal principal, @PathVariable Long id) {
        return datasetService.get(principal.getName(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable Long id) {
        datasetService.delete(principal.getName(), id);
    }
}
