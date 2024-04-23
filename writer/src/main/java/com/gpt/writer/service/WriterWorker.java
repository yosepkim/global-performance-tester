package com.gpt.writer.service;

import com.gpt.writer.model.Result;
import com.gpt.writer.model.Run;
import com.gpt.writer.model.RunInstruction;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.TimerTask;

public class WriterWorker extends TimerTask {
    private final RunInstruction instruction;
    private final Result result;

    public WriterWorker(RunInstruction instruction, Result result) {
        this.instruction = instruction;
        this.result = result;
    }

    public void run() {
        try {
            RestClient restClient = RestClient.create();
            String url = this.instruction.getWriterTargetUrl() + instruction.getTestValue().getKey();
            String httpResult = restClient
                    .method(HttpMethod.valueOf(this.instruction.getWriterTargetMethod()))
                    .uri(url)
                    .body(instruction.getTestValue())
                    .retrieve()
                    .body(String.class);

            System.out.println("RUN: " + instruction.getTestValue() + " - " + httpResult);

            Run run = new Run();
            run.setExecutedTime(Instant.now());

            System.out.println("Processing time:    " + run.getExecutedTime().atZone(ZoneOffset.UTC).toInstant().toEpochMilli());

            result.getRuns().add(run);

            String orchestratorUrl = instruction.getOrchestratorUrl() + result.getRunId();
            String orchestratorResponse = restClient
                    .method(HttpMethod.POST)
                    .uri(orchestratorUrl)
                    .body(result)
                    .retrieve()
                    .body(String.class);

            System.out.println("COMPLETE: " + orchestratorResponse);
            this.cancel();

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}