package com.gpt.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RunInstruction implements Serializable {
    private String runId;
    private String orchestratorUrl;
    private String readerTargetUrl;
    private String readerTargetMethod;
    private String readerTargetPathType;
    private KeyValue readerTargetHeader;
    private String writerTargetUrl;
    private String writerTargetMethod;
    private String writerTargetPathType;
    private KeyValue writerTargetHeader;
    private Instant startTime;
    private KeyValue testValue;
    private String testKeyOverride;
    private Location writer;
    private List<Location> readers = new ArrayList<>();
}
