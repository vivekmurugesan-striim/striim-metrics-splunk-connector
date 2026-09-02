package com.striim.service;

import com.striim.config.StriimMonCommands;
import com.striim.entity.ExecutionHistory;
import com.striim.entity.SystemConfig;
import com.striim.repository.ExecutionHistoryRepository;
import com.striim.repository.SystemConfigRepository;
import com.striim.util.EncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
public class MetricsCollectionService {

    @Autowired
    private StriimApiClient striimApiClient;

    @Autowired
    private SplunkHecClient splunkHecClient;

    @Autowired
    private SystemConfigRepository configRepository;

    @Autowired
    private ExecutionHistoryRepository historyRepository;

    @Scheduled(fixedRateString = "${striim.metrics.collection-interval-seconds:60}000")
    public void collectMetricsScheduled() {
        SystemConfig config = configRepository.findById("default").orElse(null);
        if (config == null) {
            log.debug("No configuration found, skipping scheduled collection");
            return;
        }

        collectAndPublishMetrics(StriimMonCommands.DEFAULT_COMMANDS, "SCHEDULED");
    }

    public String collectAndPublishMetrics(List<String> commands, String triggerType) {
        String executionId = generateExecutionId();
        ExecutionHistory history = new ExecutionHistory();
        history.setExecutionId(executionId);
        history.setTriggerType(triggerType);
        history.setStatus("RUNNING");
        history.setStartTime(Instant.now());

        try {
            SystemConfig config = configRepository.findById("default")
                    .orElseThrow(() -> new IllegalStateException("Configuration not found"));

            String striimUrl = config.getStriimUrl();
            String striimUser = config.getStriimUser();
            String striimPassword = EncryptionUtil.decrypt(config.getStriimPasswordEnc());
            String splunkHecUrl = config.getSplunkHecUrl();
            String splunkToken = EncryptionUtil.decrypt(config.getSplunkTokenEnc());
            String splunkIndex = config.getSplunkIndex();

            Map<String, Object> allMetrics = new HashMap<>();
            int metricsCount = 0;

            for (String command : commands) {
                log.info("Executing command: {}", command);
                String response = striimApiClient.executeMonCommand(striimUrl, striimUser, striimPassword, command);

                if (response != null && !response.isEmpty()) {
                    Map<String, Object> parsedMetrics = parseMonResponse(response);
                    parsedMetrics.put("command", command);
                    parsedMetrics.put("timestamp", System.currentTimeMillis());
                    allMetrics.put("metric_" + metricsCount, parsedMetrics);
                    metricsCount++;
                    log.debug("Command executed successfully, parsed {} applications",
                        ((List<?>) parsedMetrics.getOrDefault("applications", new ArrayList<>())).size());
                } else {
                    log.warn("No response from command: {}", command);
                }
            }

            boolean published = splunkHecClient.publishMetrics(splunkHecUrl, splunkToken, splunkIndex, allMetrics);

            history.setStatus("COMPLETED");
            history.setMetricsCollectedCount(metricsCount);
            history.setPublishedToSplunk(published);

            log.info("Metrics collection completed: executionId={}, status={}, metricsCount={}, published={}",
                    executionId, history.getStatus(), metricsCount, published);
        } catch (Exception e) {
            log.error("Error during metrics collection", e);
            history.setStatus("FAILED");
            history.setErrorMessage(e.getMessage());
        } finally {
            history.setEndTime(Instant.now());
            historyRepository.save(history);
        }

        return executionId;
    }

    private Map<String, Object> parseMonResponse(String response) {
        Map<String, Object> parsedData = new HashMap<>();
        List<Map<String, String>> applications = new ArrayList<>();

        String[] lines = response.split("\n");
        boolean headerPassed = false;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.toLowerCase().contains("application") && line.toLowerCase().contains("status")) {
                headerPassed = true;
                continue;
            }

            if (!headerPassed) continue;

            String[] parts = line.split("\\s{2,}");
            if (parts.length >= 2) {
                Map<String, String> app = new HashMap<>();
                app.put("name", parts[0].trim());
                app.put("status", parts[1].trim());
                if (parts.length > 2) {
                    app.put("details", parts[2].trim());
                }
                applications.add(app);
            }
        }

        parsedData.put("applications", applications);
        parsedData.put("totalApplications", applications.size());
        parsedData.put("runningApplications",
            applications.stream().filter(a -> a.get("status").equalsIgnoreCase("RUNNING")).count());
        parsedData.put("stoppedApplications",
            applications.stream().filter(a -> a.get("status").equalsIgnoreCase("STOPPED")).count());
        parsedData.put("rawResponse", response);

        return parsedData;
    }

    private String generateExecutionId() {
        return "exec_" + System.currentTimeMillis();
    }
}
