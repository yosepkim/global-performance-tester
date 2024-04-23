package com.gpt.orchestrator.controller;

import com.gpt.orchestrator.model.KeyValue;
import com.gpt.orchestrator.model.Result;
import com.gpt.orchestrator.model.Run;
import com.gpt.orchestrator.model.RunInstruction;
import com.gpt.orchestrator.repository.ResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AdminController {

    @Autowired
    ResultRepository repository;

    @GetMapping("results/{runId}")
    public List<Result> getResults(@PathVariable String runId) {
        return repository.findByRunId(runId);
    }

    @GetMapping("analyze/{runId}")
    public String analyze(@PathVariable String runId) {
        List<Result> records = repository.findByRunId(runId);
            Map<String, Instant> locationLatest = new HashMap<>();
        Instant writerTimestamp = Instant.now();
        for (Result record : records) {
            if ("writer".equals(record.getWorkerType())) {
                writerTimestamp = record.getRuns().get(0).getExecutedTime();
            } else {
                for (Run run : record.getRuns()) {
                    if (!locationLatest.containsKey(record.getLocation()) || run.getExecutedTime().isAfter(locationLatest.get(record.getLocation()))) {
                        locationLatest.put(record.getLocation(), run.getExecutedTime());
                    }
                }
            }
        };

        StringBuilder sb =  new StringBuilder();
        sb.append("Writer executed time= ");
        sb.append(writerTimestamp);
        sb.append("\n");
        for (Map.Entry<String,Instant> entry : locationLatest.entrySet()) {
            sb.append(entry.getKey());
            sb.append("= ");
            sb.append(entry.getValue());
            sb.append(" ----> ");
            sb.append(entry.getValue().toEpochMilli() - writerTimestamp.toEpochMilli());
            sb.append(" ms latency");
            sb.append("\n");
        }
        return sb.toString();
    }

    @PostMapping("/report/result/{runId}")
    public String reportResult(@PathVariable String runId, @RequestBody Result result) throws IOException {
        repository.save(result);
        System.out.println("DATA: " + result.toString());
        System.out.println("Time: " + result.getRuns().get(0).getExecutedTime().atZone(ZoneOffset.UTC).toInstant().toEpochMilli());
        return "Success";
    }

    @PostMapping("/start/{runId}")
    public String start(@PathVariable String runId, @RequestBody RunInstruction runInstruction) throws IOException {
        String message = "Started";

        runInstruction.setStartTime(Instant.now().plusSeconds(10));
        runInstruction.setRunId(runId);

        KeyValue sample = new KeyValue();
        sample.setKey("brian1");
        sample.setValue(String.valueOf(Math.random()));
        runInstruction.setTestValue(sample);

        RestClient restClient = RestClient.create();

        try {
            runInstruction.getReaders().forEach(reader -> {
                String result = restClient.post()
                        .uri(reader.getUrl())
                        .body(runInstruction)
                        .retrieve()
                        .body(String.class);
                System.out.println(result);
            });

            String result = restClient.post()
                    .uri(runInstruction.getWriter().getUrl())
                    .body(runInstruction)
                    .header("content-type", "application/JSON")
                    .retrieve()
                    .body(String.class);
            System.out.println(result);
        } catch (Exception ex) {
            message = ex.getMessage();
        }

        return message;
    }
}
