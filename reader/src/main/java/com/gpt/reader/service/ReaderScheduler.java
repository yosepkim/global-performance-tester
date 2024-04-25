package com.gpt.reader.service;

import com.gpt.reader.model.Result;
import com.gpt.reader.model.RunInstruction;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ReaderScheduler extends TimerTask {
    private ExecutorService executorService;
    private final RunInstruction instruction;
    private final Result result;

    private int runCounter;

    public ReaderScheduler(RunInstruction instruction, Result result) {
        this.executorService = Executors.newFixedThreadPool(100);
        this.instruction = instruction;
        this.result = result;
        this.runCounter = 0;

        try {
            this.executorService.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        System.out.println("Number of active thread : " + ((ThreadPoolExecutor)this.executorService).getActiveCount());

        if (runCounter > 5000) {
            System.out.println("INCOMPLETE: Didn't get the data");
            this.cancel();
        } else if (result.isCompleted()) {
            String orchestratorResponse = reportResult(instruction);
            System.out.println("COMPLETE: " + orchestratorResponse);
            this.cancel();
        }
        this.executorService.execute(new ReaderWorker(instruction, result));
        runCounter++;
    }

    public void initiateShutdown(){
        this.executorService.shutdown();
    }

    private String reportResult(RunInstruction instruction) {
        String orchestratorUrl = instruction.getOrchestratorUrl() + result.getRunId();
        RestClient restClient = RestClient.create();
        return restClient
                .method(HttpMethod.POST)
                .uri(orchestratorUrl)
                .body(result)
                .retrieve()
                .body(String.class);
    }
}