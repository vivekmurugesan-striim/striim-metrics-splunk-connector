package com.striim.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        List<Map<String, Object>> applications = new ArrayList<>();

        try {
            com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(response);

            if (rootNode.isObject()) {
                com.fasterxml.jackson.databind.JsonNode outputNode = rootNode.get("output");
                if (outputNode != null && outputNode.has("striimApplications")) {
                    com.fasterxml.jackson.databind.JsonNode appsNode = outputNode.get("striimApplications");
                    if (appsNode.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode appNode : appsNode) {
                            Map<String, Object> app = new HashMap<>();
                            app.put("name", appNode.has("fullName") ? appNode.get("fullName").asText() : "");
                            app.put("status", appNode.has("statusChange") ? appNode.get("statusChange").asText() : "UNKNOWN");
                            app.put("rate", appNode.has("rate") ? appNode.get("rate").asText() : "0");
                            app.put("numServers", appNode.has("numServers") ? appNode.get("numServers").asText() : "0");
                            applications.add(app);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse JSON mon response, attempting text parse: {}", e.getMessage());
            parseAsPlainText(response, applications);
        }

        parsedData.put("applications", applications);
        parsedData.put("totalApplications", applications.size());
        parsedData.put("runningApplications",
            applications.stream().filter(a -> "RUNNING".equalsIgnoreCase(a.get("status").toString())).count());
        parsedData.put("stoppedApplications",
            applications.stream().filter(a -> "STOPPED".equalsIgnoreCase(a.get("status").toString())).count());
        parsedData.put("createdApplications",
            applications.stream().filter(a -> "CREATED".equalsIgnoreCase(a.get("status").toString())).count());

        return parsedData;
    }

    private void parseAsPlainText(String response, List<Map<String, Object>> applications) {
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
                Map<String, Object> app = new HashMap<>();
                app.put("name", parts[0].trim());
                app.put("status", parts[1].trim());
                if (parts.length > 2) {
                    app.put("details", parts[2].trim());
                }
                applications.add(app);
            }
        }
    }

    private String generateExecutionId() {
        return "exec_" + System.currentTimeMillis();
    }
}
