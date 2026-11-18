package com.fraud.backend.repository;

import com.fraud.backend.entity.Dataset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DatasetRepository extends JpaRepository<Dataset, Long> {

    Page<Dataset> findByUploadedById(Long userId, Pageable pageable);

    Page<Dataset> findByStatus(Dataset.Status status, Pageable pageable);

    Page<Dataset> findByUploadedByEmailIgnoreCaseOrderByUploadDateDesc(String email, Pageable pageable);

    Optional<Dataset> findByIdAndUploadedByEmailIgnoreCase(Long id, String email);

    Optional<Dataset> findFirstByUploadedByEmailIgnoreCaseAndFileNameOrderByIdDesc(
            String email,
            String fileName
    );
}
