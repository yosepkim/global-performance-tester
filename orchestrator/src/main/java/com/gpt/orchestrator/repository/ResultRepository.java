package com.gpt.orchestrator.repository;

import com.gpt.orchestrator.model.Result;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ResultRepository extends CrudRepository<Result, Long> {
    List<Result> findByRunId(String runId);
}