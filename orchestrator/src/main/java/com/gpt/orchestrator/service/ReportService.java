package com.gpt.orchestrator.service;

import com.gpt.orchestrator.model.Result;
import com.gpt.orchestrator.model.Run;
import com.gpt.orchestrator.repository.ResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {
    @Autowired
    ResultRepository repository;

    public String analyze(String runId) {
        return analyze(runId, true);
    }

    public String analyze(String runId, boolean printHeader) {
        List<Result> records = repository.findByRunId(runId);
        if (records.size() > 0) {
            Map<String, Run> locationLatest = new HashMap<>();

            Instant writerTimestamp = Instant.now();
            Instant writerStartTime = Instant.now();
            String writerLocation = "N/A";
            long medianWriterExecutedTime = 0;
            long writerRequestDuration = 0;
            for (Result record : records) {
                if ("writer".equals(record.getWorkerType())) {
                    writerStartTime = record.getRuns().get(0).getStartTime();
                    writerTimestamp = record.getRuns().get(0).getExecutedTime();
                    writerLocation = record.getLocation();
                    writerRequestDuration = record.getRuns().get(0).getExecutedTime().toEpochMilli() - record.getRuns().get(0).getStartTime().toEpochMilli();
                    medianWriterExecutedTime = calculateMedianExecuteTime(record.getRuns().get(0).getStartTime(), record.getRuns().get(0).getExecutedTime());
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
                sb.append("runId,writerLocation,readerLocation,readerRequestDuration,writerRequestDuration,latencyByStartTime,latencyByExecutedTime,normalizedLatency,latencyByDbInsertTime,writerStartTime,writerExecutedTime,readerStartTime,readerExecutedTime,databaseInsertTime");
                sb.append("\n");
            }
            for (Map.Entry<String, Run> entry : locationLatest.entrySet()) {
                sb.append(runId);
                sb.append(",");
                sb.append(writerLocation);
                sb.append(",");
                sb.append(entry.getKey());
                sb.append(",");
                long readerRequestDuration = entry.getValue().getExecutedTime().toEpochMilli() - entry.getValue().getStartTime().toEpochMilli();
                sb.append(readerRequestDuration);
                sb.append(",");
                sb.append(writerRequestDuration);
                sb.append(",");
                sb.append(entry.getValue().getStartTime().toEpochMilli() - writerTimestamp.toEpochMilli());
                sb.append(",");
                sb.append(entry.getValue().getExecutedTime().toEpochMilli() - writerTimestamp.toEpochMilli());
                sb.append(",");
                long medianReaderExecuteTime = calculateMedianExecuteTime(entry.getValue().getStartTime(), entry.getValue().getExecutedTime());
                sb.append(medianReaderExecuteTime - medianWriterExecutedTime);
                sb.append(",");
                if (entry.getValue().getDatabaseInsertTime() != null) {
                    sb.append(medianReaderExecuteTime - entry.getValue().getDatabaseInsertTime().toEpochMilli());
                }
                sb.append(",");
                sb.append(writerStartTime);
                sb.append(",");
                sb.append(writerTimestamp);
                sb.append(",");
                sb.append(entry.getValue().getStartTime());
                sb.append(",");
                sb.append(entry.getValue().getExecutedTime());
                sb.append(",");
                sb.append(entry.getValue().getDatabaseInsertTime());
                sb.append("\n");
            }
            return sb.toString();
        }
        return "Job has not completed";
    }

    private long calculateMedianExecuteTime(Instant startTime, Instant endTime) {
        long requestDuration = endTime.toEpochMilli() - startTime.toEpochMilli();
        return endTime.toEpochMilli() - (requestDuration / 2);
    }
}
