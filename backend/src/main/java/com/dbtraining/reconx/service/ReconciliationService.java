package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.repository.ReconResultRepository;

import java.util.List;

public class ReconciliationService {

    private final ReconResultRepository repository;

    public ReconciliationService(ReconResultRepository repository) {
        this.repository = repository;
    }

    public void saveResults(List<ReconResult> results) {

        for (ReconResult result: results) {
            repository.save(result);
        }
    }
}