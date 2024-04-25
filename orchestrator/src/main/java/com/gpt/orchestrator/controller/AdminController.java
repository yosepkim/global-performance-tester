package com.gpt.orchestrator.controller;

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
    public String runAll(@PathVariable String runId, @RequestBody RunInstruction runInstruction) throws IOException, InterruptedException {
        int runSubId = 1;
        StringBuilder sb = new StringBuilder();
        for (Location writer : runInstruction.getReaders()) {
            String eachRunId = runId + "-" + runSubId;

            RunInstruction eachInstruction = (RunInstruction) SerializationUtils.clone(runInstruction);
            eachInstruction.setRunId(eachRunId);
            eachInstruction.getWriter().setUrl(writer.getUrl().replace("8091", "8090"));
            start(eachRunId, eachInstruction);
            TimeUnit.SECONDS.sleep(10);
            String result = analyze(eachRunId);
            sb.append(result);
            sb.append("\n");
            runSubId++;
        }
        return sb.toString();
    }

    @GetMapping("analyze/{runId}")
    public String analyze(@PathVariable String runId) {
        List<Result> records = repository.findByRunId(runId);

        if (records.size() > 0) {
            Map<String, Instant> locationLatest = new HashMap<>();

            Instant writerTimestamp = Instant.now();
            String writerLocation = "N/A";
            for (Result record : records) {
                if ("writer".equals(record.getWorkerType())) {
                    writerTimestamp = record.getRuns().get(0).getStartTime();
                    writerLocation = record.getLocation();
                } else {
                    for (Run run : record.getRuns()) {
                        if (!locationLatest.containsKey(record.getLocation()) || run.getStartTime().isAfter(locationLatest.get(record.getLocation()))) {
                            locationLatest.put(record.getLocation(), run.getStartTime());
                        }
                    }
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("[" + runId + "] " );
            sb.append(writerLocation);
            sb.append(" writer executed time= ");
            sb.append(writerTimestamp);
            sb.append("\n");
            for (Map.Entry<String, Instant> entry : locationLatest.entrySet()) {
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
        return "Job has not completed";
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

        runInstruction.setStartTime(Instant.now().plusSeconds(5));
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
