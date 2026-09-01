package com.striim.service;

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

        List<String> defaultCommands = Arrays.asList("mon system", "mon apps");
        collectAndPublishMetrics(defaultCommands, "SCHEDULED");
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
            String striimToken = EncryptionUtil.decrypt(config.getStriimTokenEnc());
            String splunkHecUrl = config.getSplunkHecUrl();
            String splunkToken = EncryptionUtil.decrypt(config.getSplunkTokenEnc());
            String splunkIndex = config.getSplunkIndex();

            Map<String, Object> allMetrics = new HashMap<>();
            int metricsCount = 0;

            for (String command : commands) {
                log.info("Executing command: {}", command);
                Map<String, Object> metrics = striimApiClient.callStriimApi(striimUrl, striimToken, command);
                allMetrics.putAll(metrics);
                metricsCount++;
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

    private String generateExecutionId() {
        return "exec_" + System.currentTimeMillis();
    }
}
