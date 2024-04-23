package com.gpt.orchestrator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class Result {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    private String runId;
    private String location;
    private String workerType;
    private Instant initialStartTime;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Run> runs = new ArrayList();
}

