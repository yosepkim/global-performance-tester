package com.gpt.orchestrator.controller;

import com.gpt.orchestrator.model.MultiRunInstruction;
import com.gpt.orchestrator.model.*;
import com.gpt.orchestrator.repository.ResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.SerializationUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
public class AdminController {

    @Autowired
    ResultRepository repository;

    @GetMapping("results/{runId}")
    public List<Result> getResults(@PathVariable String runId) {
        return repository.findByRunId(runId);
    }

    @GetMapping("run/all/{runId}")
    public String runAll(@PathVariable String runId, @RequestBody MultiRunInstruction multiRunInstruction) throws IOException, InterruptedException {
        int runSubId = 1;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Location writer : multiRunInstruction.getWriters()) {
            String eachRunId = runId + "-" + runSubId;
            RunInstruction eachInstruction = (RunInstruction) SerializationUtils.clone(multiRunInstruction);
            eachInstruction.setRunId(eachRunId);
            eachInstruction.getWriter().setUrl(writer.getUrl());
            eachInstruction.getWriter().setLocationName(writer.getLocationName());
            start(eachRunId, eachInstruction);
            TimeUnit.SECONDS.sleep(10);
            String result = analyze(eachRunId, i == 0);
            sb.append(result);
            runSubId++;
            i++;
        }
        return sb.toString();
    }

    @GetMapping("analyze/{runId}")
    public String analyze(@PathVariable String runId, boolean printHeader) {
        List<Result> records = repository.findByRunId(runId);

        if (records.size() > 0) {
            Map<String, Run> locationLatest = new HashMap<>();

            Instant writerTimestamp = Instant.now();
            String writerLocation = "N/A";
            for (Result record : records) {
                if ("writer".equals(record.getWorkerType())) {
                    writerTimestamp = record.getRuns().get(0).getExecutedTime();
                    writerLocation = record.getLocation();
                } else {
                    for (Run run : record.getRuns()) {
                        if (!locationLatest.containsKey(record.getLocation()) || run.getExecutedTime().isAfter(locationLatest.get(record.getLocation()).getExecutedTime())) {
                            locationLatest.put(record.getLocation(), run);
                        }
                    }
                }
            }

            StringBuilder sb = new StringBuilder();
            if (printHeader) {
                sb.append("runId,writerLocation,writerExecutedTime,readerLocation,readerStartTime,readerExecutedTime,readerRequestDuration,latencyByStartTime,latencyByExecutedTime");
                sb.append("\n");
            }
            for (Map.Entry<String, Run> entry : locationLatest.entrySet()) {
                sb.append(runId);
                sb.append(",");
                sb.append(writerLocation);
                sb.append(",");
                sb.append(writerTimestamp);
                sb.append(",");
                sb.append(entry.getKey());
                sb.append(",");
                sb.append(entry.getValue().getStartTime());
                sb.append(",");
                sb.append(entry.getValue().getExecutedTime());
                sb.append(",");
                sb.append(entry.getValue().getExecutedTime().toEpochMilli() - entry.getValue().getStartTime().toEpochMilli());
                sb.append(",");
                sb.append(entry.getValue().getStartTime().toEpochMilli() - writerTimestamp.toEpochMilli());
                sb.append(",");
                sb.append(entry.getValue().getExecutedTime().toEpochMilli() - writerTimestamp.toEpochMilli());
                sb.append("\n");
            }
            return sb.toString();
        }
        return "Job has not completed";
    }

    @PostMapping("/report/result/{runId}")
    public String reportResult(@PathVariable String runId, @RequestBody Result result) throws IOException {
        repository.save(result);
        System.out.println("Report Received: " + result.toString());
        return "Success";
    }

    @PostMapping("/start/{runId}")
    public String start(@PathVariable String runId, @RequestBody RunInstruction runInstruction) throws IOException {
        String message = "Started";

        runInstruction.setStartTime(Instant.now().atZone(ZoneOffset.UTC).toInstant().plusSeconds(5));
        runInstruction.setRunId(runId);

        KeyValue sample = new KeyValue();
        sample.setKey("brian1");
        sample.setValue(String.valueOf(Math.random()));
        runInstruction.setTestValue(sample);

        RestClient restClient = RestClient.create();

        try {
            runInstruction.getReaders().forEach(reader -> {
                System.out.println(runInstruction.getRunId() + ") Reader ================== " + reader);
                String result = restClient.post()
                        .uri(reader.getUrl())
                        .body(runInstruction)
                        .retrieve()
                        .body(String.class);
                System.out.println("Reader (" + reader.getLocationName() + ") returns " + result + " : " + runInstruction);
            });

            System.out.println(runInstruction.getRunId() + ") Writer ================== " + runInstruction);
            String result = restClient.post()
                    .uri(runInstruction.getWriter().getUrl())
                    .body(runInstruction)
                    .header("content-type", "application/JSON")
                    .retrieve()
                    .body(String.class);
            System.out.println("writer (" + runInstruction.getWriter().getLocationName() + ") returns " + result + " : " + runInstruction);
        } catch (Exception ex) {
            message = ex.getMessage();
            System.out.println(runId + ") ERROR: " + message);
        }

        return message;
    }
}
