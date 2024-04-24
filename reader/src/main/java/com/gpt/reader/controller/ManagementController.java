package com.gpt.reader.controller;

import com.gpt.reader.model.Result;
import com.gpt.reader.model.RunInstruction;
import com.gpt.reader.service.ReaderWorker;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

@RestController
public class ManagementController {
    @PostMapping("/start")
    public String start(@RequestBody RunInstruction instruction) {
        Result result = new Result();
        result.setRunId(instruction.getRunId());
        result.setLocation(System.getProperty("dcLocation"));
        result.setWorkerType("reader");
        result.setInitialStartTime(instruction.getStartTime());

        Timer timer = new Timer();
        TimerTask task = new ReaderWorker(instruction, result);
        var startTime = Date.from(instruction.getStartTime());;
        timer.scheduleAtFixedRate(task, startTime, 5);

        return "Started";
    }
}

