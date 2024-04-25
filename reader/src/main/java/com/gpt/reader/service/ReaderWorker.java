package com.gpt.reader.service;

import com.gpt.reader.model.KeyValue;
import com.gpt.reader.model.Result;
import com.gpt.reader.model.Run;
import com.gpt.reader.model.RunInstruction;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Objects;

public class ReaderWorker implements Runnable {
    private final RunInstruction instruction;
    private final Result result;
    private final RestClient restClient;

    public ReaderWorker(RunInstruction instruction, Result result) {
        this.instruction = instruction;
        this.result = result;
        this.restClient = RestClient.create();
    }

    @Override
    public void run() {
        try {
            String url;
            if ("append-key".equals(instruction.getReaderTargetPathType())) {
                url = this.instruction.getReaderTargetUrl() + instruction.getTestValue().getKey();
            } else {
                url = this.instruction.getReaderTargetUrl();
            }

            KeyValue httpResult;
            Run run = new Run();
            try {
                run.setStartTime(Instant.now());
                httpResult = restClient
                        .method(HttpMethod.valueOf(this.instruction.getReaderTargetMethod()))
                        .uri(url)
                        .headers(headers -> {
                            if (this.instruction.getReaderTargetHeader() != null) {
                                headers.set(this.instruction.getReaderTargetHeader().getKey(),
                                        this.instruction.getReaderTargetHeader().getValue());
                            }
                        })
                        .retrieve()
                        .body(KeyValue.class);
            } catch (Exception ex) {
                httpResult = new KeyValue();
                System.out.println("RUN FAILED: " + instruction.getTestValue() + " - " + ex.getMessage());
            }

            run.setExecutedTime(Instant.now());
            result.getRuns().add(run);

            if (Objects.equals(instruction.getTestValue().getValue(), httpResult.getValue())) {
                result.setCompleted(true);
            }
        } catch (Exception ex) {
            System.out.println(instruction.getRunId() + ") ERROR: " + ex.getMessage());
        }
    }
}
