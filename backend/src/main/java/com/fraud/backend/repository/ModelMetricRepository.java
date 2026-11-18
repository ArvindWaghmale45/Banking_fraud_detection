package com.fraud.backend.repository;

import com.fraud.backend.entity.ModelMetric;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelMetricRepository extends JpaRepository<ModelMetric, Long> {

    Page<ModelMetric> findByModelNameAndModelVersion(String modelName, String modelVersion, Pageable pageable);
}
