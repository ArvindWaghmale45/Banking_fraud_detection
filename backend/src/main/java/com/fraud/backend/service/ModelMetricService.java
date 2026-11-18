package com.fraud.backend.service;

import com.fraud.backend.dto.ModelMetricImportRequest;
import com.fraud.backend.dto.ModelMetricResponse;
import com.fraud.backend.entity.ModelMetric;
import com.fraud.backend.exception.InvalidModelMetricException;
import com.fraud.backend.repository.ModelMetricRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ModelMetricService {

    private final ModelMetricRepository repository;

    public ModelMetricService(ModelMetricRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ModelMetricResponse importMetrics(ModelMetricImportRequest request) {
        if (request == null
                || request.getModelName() == null
                || request.getModelName().isBlank()
                || request.getModelName().trim().length() > 100
                || request.getAccuracy() == null
                || request.getPrecision() == null
                || request.getRecall() == null
                || request.getF1Score() == null
                || request.getRocAuc() == null
                || request.getPrAuc() == null) {
            throw new InvalidModelMetricException("Required model metrics are missing or invalid.");
        }

        validateMetric(request.getAccuracy());
        validateMetric(request.getPrecision());
        validateMetric(request.getRecall());
        validateMetric(request.getF1Score());
        validateMetric(request.getRocAuc());
        validateMetric(request.getPrAuc());

        ModelMetric metric = new ModelMetric(
                request.getModelName().trim(),
                "1.0",
                LocalDateTime.now()
        );
        metric.setAccuracy(request.getAccuracy());
        metric.setPrecision(request.getPrecision());
        metric.setRecall(request.getRecall());
        metric.setF1Score(request.getF1Score());
        metric.setRocAuc(request.getRocAuc());
        metric.setPrAuc(request.getPrAuc());

        return ModelMetricResponse.from(repository.save(metric));
    }

    @Transactional(readOnly = true)
    public List<ModelMetricResponse> getAllMetrics() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "trainingDate"))
                .stream()
                .map(ModelMetricResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ModelMetricResponse> getLatestMetrics() {
        return repository.findAll(
                        PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "trainingDate"))
                )
                .stream()
                .findFirst()
                .map(ModelMetricResponse::from);
    }

    private void validateMetric(BigDecimal value) {
        if (value.compareTo(java.math.BigDecimal.ZERO) < 0
                || value.compareTo(BigDecimal.ONE) > 0
                || value.scale() > 18
                || value.precision() - value.scale() > 1) {
            throw new InvalidModelMetricException(
                    "Model metrics must be between 0 and 1 with at most 18 decimal places."
            );
        }
    }
}
