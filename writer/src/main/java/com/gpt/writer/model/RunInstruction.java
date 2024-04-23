package com.gpt.writer.model;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class RunInstruction {
    private String runId;
    private String orchestratorUrl;
    private String readerTargetUrl;
    private String readerTargetMethod;
    private String writerTargetUrl;
    private String writerTargetMethod;
    private String writerTargetPathType;
    private KeyValue writerTargetHeader;
    private Instant startTime;
    private KeyValue testValue;
}
