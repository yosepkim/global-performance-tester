package com.gpt.reader.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Result {
    private String runId;
    private String location;
    private String workerType;
    private Instant initialStartTime;
    private boolean completed = false;
    private List<Run> runs = new ArrayList();
}
