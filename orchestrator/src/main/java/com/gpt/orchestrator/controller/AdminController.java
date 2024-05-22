package com.gpt.orchestrator.controller;

import com.gpt.orchestrator.model.*;
import com.gpt.orchestrator.repository.ResultRepository;
import com.gpt.orchestrator.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.SerializationUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
public class AdminController {

    @Autowired
    ResultRepository repository;

    @Autowired
    ReportService reportService;

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
            String result = reportService.analyze(eachRunId, i == 0);
            sb.append(result);
            runSubId++;
            i++;
        }
        return sb.toString();
    }

    @GetMapping("analyze/{runId}")
    public String analyze(@PathVariable String runId) {
        return reportService.analyze(runId);
    }

    @PostMapping("/report/result/{runId}")
    public String reportResult(@PathVariable String runId, @RequestBody Result result) throws IOException {
        repository.save(result);
        System.out.println("Report Received: " + result);
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
