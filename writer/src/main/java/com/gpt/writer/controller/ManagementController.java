package com.gpt.writer.controller;

import com.gpt.writer.model.Result;
import com.gpt.writer.model.RunInstruction;
import com.gpt.writer.service.WriterWorker;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

@RestController
public class ManagementController {

    @PostMapping("/start")
    public String start(@RequestBody RunInstruction instruction) {
        System.out.println("Request received: " + instruction);

        Result result = new Result();
        result.setRunId(instruction.getRunId());
        result.setLocation(System.getProperty("dcLocation"));
        result.setWorkerType("writer");
        result.setInitialStartTime(instruction.getStartTime());

        Timer timer = new Timer();
        TimerTask task = new WriterWorker(instruction, result);

        System.out.println("Initial Start Time: " + instruction.getStartTime().atZone(ZoneOffset.UTC).toInstant().toEpochMilli());
        var startTime = Date.from(instruction.getStartTime().plusMillis(10));
        System.out.println("Processing time:    " + startTime.toInstant().toEpochMilli());
        timer.schedule(task, startTime);
        return "Started";
    }
}
