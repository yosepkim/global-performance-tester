package com.gpt.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RunInstruction {
    private String runId;
    private String orchestratorUrl;
    private String readerTargetUrl;
    private String readerTargetMethod;
    private String writerTargetUrl;
    private String writerTargetMethod;
    private Instant startTime;
    private KeyValue testValue;
    private Location writer;
    private List<Location> readers = new ArrayList<>();
}
