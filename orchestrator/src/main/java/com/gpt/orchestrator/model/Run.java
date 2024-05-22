package com.gpt.orchestrator.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class Run {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    private Instant startTime;
    private Instant executedTime;
    private Instant databaseInsertTime;
}
