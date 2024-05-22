package com.gpt.reader.service;

import com.gpt.reader.model.KeyValue;
import com.gpt.reader.model.Result;
import com.gpt.reader.model.Run;
import com.gpt.reader.model.RunInstruction;
import io.micrometer.common.util.StringUtils;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.ZoneOffset;
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

            KeyValue httpResult  = new KeyValue();
            Run run = new Run();
            try {
                run.setStartTime(Instant.now().atZone(ZoneOffset.UTC).toInstant());
                String response = restClient
                        .method(HttpMethod.valueOf(this.instruction.getReaderTargetMethod()))
                        .uri(url)
                        .headers(headers -> {
                            if (this.instruction.getReaderTargetHeader() != null) {
                                headers.set(this.instruction.getReaderTargetHeader().getKey(),
                                        this.instruction.getReaderTargetHeader().getValue());
                            }
                        })
                        .retrieve()
                        .body(String.class);

                run.setExecutedTime(Instant.now().atZone(ZoneOffset.UTC).toInstant());

                JSONObject jsonResponse = new JSONObject(response);
                if (this.instruction.getReaderTargetResponseEmbeddedAttributeName() != null) {
                    JSONObject jsonObject = jsonResponse.getJSONObject(this.instruction.getReaderTargetResponseEmbeddedAttributeName());
                    httpResult.setValue(jsonObject.getString("value"));
                } else {
                    httpResult.setValue(jsonResponse.getString("value"));
                }

                if (StringUtils.isNotBlank(this.instruction.getReaderDatabaseInsertTimeAttributeName())) {
                    run.setDatabaseInsertTime(Instant.ofEpochMilli(jsonResponse.getLong(this.instruction.getReaderDatabaseInsertTimeAttributeName())));
                    System.out.println("DB Insert time:    " + run.getDatabaseInsertTime().atZone(ZoneOffset.UTC).toInstant().toEpochMilli());
                }

                System.out.println(result.getRuns().size() + " TEST VALUE= " + instruction.getTestValue() + " - " + httpResult.getValue());
                if (Objects.equals(instruction.getTestValue().getValue(), httpResult.getValue())) {
                    result.getRuns().add(run);
                    result.setCompleted(true);
                }

            } catch (Exception ex) {
                System.out.println("RUN FAILED: " + instruction.getTestValue() + " - " + ex.getMessage());
            }
        } catch (Exception ex) {
            System.out.println(instruction.getRunId() + ") ERROR: " + ex.getMessage());
        }
    }
}
