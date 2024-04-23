package com.gpt.reader.service;

import com.gpt.reader.model.KeyValue;
import com.gpt.reader.model.Result;
import com.gpt.reader.model.Run;
import com.gpt.reader.model.RunInstruction;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Objects;
import java.util.TimerTask;

public class ReaderWorker extends TimerTask {
    private final RunInstruction instruction;
    private final Result result;
    private RestClient restClient;
    private int counter;

    public ReaderWorker(RunInstruction instruction, Result result) {
        this.instruction = instruction;
        this.result = result;
        this.counter = 0;
        this.restClient = RestClient.create();
    }

    public void run() {
        try {
            String url = this.instruction.getReaderTargetUrl() + instruction.getTestValue().getKey();
            KeyValue httpResult = restClient
                    .method(HttpMethod.valueOf(this.instruction.getReaderTargetMethod()))
                    .uri(url)
                    .retrieve()
                    .body(KeyValue.class);

            Run run = new Run();
            run.setExecutedTime(Instant.now());
            result.getRuns().add(run);
            counter++;

            System.out.println("RUN: " + instruction.getTestValue() + " - " + result);

            if (counter > 100) {
                counter = 0;
                System.out.println("INCOMPLETE: Didn't get the data");
                this.cancel();
            } else if (Objects.equals(instruction.getTestValue().getKey(), httpResult.getKey())
                    && Objects.equals(instruction.getTestValue().getValue(), httpResult.getValue())) {
                String orchestratorResponse = reportResult(instruction);
                System.out.println("COMPLETE: " + orchestratorResponse);
                this.cancel();
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    private String reportResult(RunInstruction instruction) {
        String orchestratorUrl = instruction.getOrchestratorUrl() + result.getRunId();
        return restClient
                .method(HttpMethod.POST)
                .uri(orchestratorUrl)
                .body(result)
                .retrieve()
                .body(String.class);
    }
}
