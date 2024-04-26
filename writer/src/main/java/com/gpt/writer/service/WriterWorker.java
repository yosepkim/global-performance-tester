package com.gpt.writer.service;

import com.gpt.writer.model.Result;
import com.gpt.writer.model.Run;
import com.gpt.writer.model.RunInstruction;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;

import java.time.Instant;
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

            String url;
            if ("append-key".equals(instruction.getWriterTargetPathType())) {
                url = this.instruction.getWriterTargetUrl() + instruction.getTestValue().getKey();
            } else {
                url = this.instruction.getWriterTargetUrl();
            }

            String keyName;
            if (instruction.getTestKeyOverride() != null && !instruction.getTestKeyOverride().trim().isEmpty()) {
                keyName = instruction.getTestKeyOverride();
            } else {
                keyName = "key";
            }

            String requestBody = "{\"" + keyName + "\": \"" + instruction.getTestValue().getKey() + "\", \"value\": \""
                    + instruction.getTestValue().getValue() + "\"}";

            Run run = new Run();
            run.setStartTime(Instant.now().atZone(ZoneOffset.UTC).toInstant());
            String httpResult = restClient
                    .method(HttpMethod.valueOf(this.instruction.getWriterTargetMethod()))
                    .uri(url)
                    .headers(headers -> {
                        if (this.instruction.getWriterTargetHeader() != null) {
                            headers.set(this.instruction.getWriterTargetHeader().getKey(),
                                    this.instruction.getWriterTargetHeader().getValue());
                        }
                        headers.set("content-type", "application/JSON");
                    })
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            System.out.println("RUN: " + instruction.getTestValue() + " - " + httpResult);


            run.setExecutedTime(Instant.now().atZone(ZoneOffset.UTC).toInstant());

            System.out.println("Writing time:    " + run.getExecutedTime().atZone(ZoneOffset.UTC).toInstant().toEpochMilli());

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
