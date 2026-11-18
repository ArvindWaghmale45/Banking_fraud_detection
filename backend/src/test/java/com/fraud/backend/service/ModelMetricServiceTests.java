package com.fraud.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraud.backend.dto.ModelMetricImportRequest;
import com.fraud.backend.entity.ModelMetric;
import com.fraud.backend.exception.InvalidModelMetricException;
import com.fraud.backend.repository.ModelMetricRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModelMetricServiceTests {

    private final ModelMetricRepository repository = mock(ModelMetricRepository.class);
    private final ModelMetricService service = new ModelMetricService(repository);

    @Test
    void importRequestBindsNotebookSnakeCaseJson() throws Exception {
        String json = """
                {
                  "model_name": "Random Forest",
                  "accuracy": 0.987654321098765432,
                  "precision": 0.876543210987654321,
                  "recall": 0.765432109876543210,
                  "f1_score": 0.812345678901234567,
                  "roc_auc": 0.923456789012345678,
                  "pr_auc": 0.654321098765432109
                }
                """;

        ModelMetricImportRequest request = new ObjectMapper().readValue(json, ModelMetricImportRequest.class);

        assertEquals("Random Forest", request.getModelName());
        assertEquals(new BigDecimal("0.987654321098765432"), request.getAccuracy());
        assertEquals(new BigDecimal("0.812345678901234567"), request.getF1Score());
        assertEquals(new BigDecimal("0.923456789012345678"), request.getRocAuc());
        assertEquals(new BigDecimal("0.654321098765432109"), request.getPrAuc());
    }

    @Test
    void importMetricsPersistsAndReturnsModelMetrics() {
        ModelMetricImportRequest request = validImportRequest();
        when(repository.save(org.mockito.ArgumentMatchers.any(ModelMetric.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.importMetrics(request);

        assertEquals("Random Forest", response.modelName());
        assertEquals("1.0", response.modelVersion());
        assertEquals(new BigDecimal("0.98765"), response.accuracy());
        assertEquals(new BigDecimal("0.87654"), response.precision());
        assertEquals(new BigDecimal("0.76543"), response.recall());
        assertTrue(response.trainingDate().isBefore(LocalDateTime.now().plusSeconds(1)));
        verify(repository).save(org.mockito.ArgumentMatchers.any(ModelMetric.class));
    }

    @Test
    void importMetricsRejectsMissingRequiredValues() {
        ModelMetricImportRequest request = validImportRequest();
        request.setPrAuc(null);

        assertThrows(InvalidModelMetricException.class, () -> service.importMetrics(request));

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any(ModelMetric.class));
    }

    @Test
    void importMetricsRejectsValuesOutsideDatabaseMetricRange() {
        ModelMetricImportRequest request = validImportRequest();
        request.setAccuracy(new BigDecimal("1.00001"));

        assertThrows(InvalidModelMetricException.class, () -> service.importMetrics(request));

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any(ModelMetric.class));
    }

    @Test
    void importMetricsAcceptsEighteenFractionalDigits() {
        ModelMetricImportRequest request = validImportRequest();
        request.setAccuracy(new BigDecimal("0.987654321098765432"));
        when(repository.save(org.mockito.ArgumentMatchers.any(ModelMetric.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.importMetrics(request);

        assertEquals(new BigDecimal("0.987654321098765432"), response.accuracy());
    }

    @Test
    void importMetricsRejectsMoreThanEighteenFractionalDigits() {
        ModelMetricImportRequest request = validImportRequest();
        request.setAccuracy(new BigDecimal("0.9876543210987654321"));

        assertThrows(InvalidModelMetricException.class, () -> service.importMetrics(request));

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any(ModelMetric.class));
    }

    @Test
    void getAllMetricsReturnsDtoResultsOrderedByTrainingDateDescending() {
        ModelMetric metric = new ModelMetric("Notebook model", "1.0", LocalDateTime.of(2026, 1, 1, 10, 0));
        when(repository.findAll(Sort.by(Sort.Direction.DESC, "trainingDate"))).thenReturn(List.of(metric));

        var response = service.getAllMetrics();

        assertEquals(1, response.size());
        assertEquals("Notebook model", response.getFirst().modelName());
        assertEquals("1.0", response.getFirst().modelVersion());
        assertEquals(metric.getTrainingDate(), response.getFirst().trainingDate());
        verify(repository).findAll(Sort.by(Sort.Direction.DESC, "trainingDate"));
    }

    @Test
    void getLatestMetricsReturnsMostRecentMetricWhenAvailable() {
        ModelMetric metric = new ModelMetric("Notebook model", "1.0", LocalDateTime.of(2026, 1, 1, 10, 0));
        var pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "trainingDate"));
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(metric)));

        var response = service.getLatestMetrics();

        assertTrue(response.isPresent());
        assertEquals("Notebook model", response.orElseThrow().modelName());
        verify(repository).findAll(pageable);
    }

    @Test
    void getLatestMetricsReturnsEmptyWhenNoMetricsExist() {
        var pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "trainingDate"));
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

        assertFalse(service.getLatestMetrics().isPresent());
    }

    private ModelMetricImportRequest validImportRequest() {
        ModelMetricImportRequest request = new ModelMetricImportRequest();
        request.setModelName("  Random Forest  ");
        request.setAccuracy(new BigDecimal("0.98765"));
        request.setPrecision(new BigDecimal("0.87654"));
        request.setRecall(new BigDecimal("0.76543"));
        request.setF1Score(new BigDecimal("0.81234"));
        request.setRocAuc(new BigDecimal("0.92345"));
        request.setPrAuc(new BigDecimal("0.65432"));
        return request;
    }
}
