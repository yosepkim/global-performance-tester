package com.gpt.reader.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RunInstruction {
    private String runId;
    private String orchestratorUrl;
    private String readerTargetUrl;
    private String readerTargetMethod;
    private String readerTargetPathType;
    private KeyValue readerTargetHeader;
    private String writerTargetUrl;
    private String writerTargetMethod;
    private Instant startTime;
    private KeyValue testValue;
}

