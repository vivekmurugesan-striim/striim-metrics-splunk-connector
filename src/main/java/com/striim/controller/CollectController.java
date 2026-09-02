package com.striim.controller;

import com.striim.config.StriimMonCommands;
import com.striim.dto.CollectTriggerRequest;
import com.striim.dto.ExecutionResponse;
import com.striim.entity.ExecutionHistory;
import com.striim.repository.ExecutionHistoryRepository;
import com.striim.service.MetricsCollectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/collect")
@Slf4j
public class CollectController {

    @Autowired
    private MetricsCollectionService metricsCollectionService;

    @Autowired
    private ExecutionHistoryRepository historyRepository;

    @GetMapping("/commands")
    public ResponseEntity<Map<String, Object>> getAvailableCommands() {
        Map<String, Object> response = new HashMap<>();
        response.put("defaultCommands", StriimMonCommands.DEFAULT_COMMANDS);
        response.put("availableCommands", StriimMonCommands.AVAILABLE_COMMANDS_WITH_DESC);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/trigger")
    public ResponseEntity<ExecutionResponse> triggerCollection(@RequestBody(required = false) CollectTriggerRequest request) {
        List<String> commands = (request != null && request.getTargetCommands() != null && !request.getTargetCommands().isEmpty())
                ? request.getTargetCommands()
                : StriimMonCommands.DEFAULT_COMMANDS;

        log.info("Triggering manual collection with commands: {}", commands);
        String executionId = metricsCollectionService.collectAndPublishMetrics(commands, "MANUAL");

        ExecutionHistory execution = historyRepository.findById(executionId).orElse(null);
        if (execution == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        ExecutionResponse response = ExecutionResponse.builder()
                .executionId(execution.getExecutionId())
                .status(execution.getStatus())
                .startTime(execution.getStartTime())
                .endTime(execution.getEndTime())
                .metricsCollected(execution.getMetricsCollectedCount())
                .publishedToSplunk(execution.getPublishedToSplunk())
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/status/{executionId}")
    public ResponseEntity<ExecutionResponse> getExecutionStatus(@PathVariable String executionId) {
        ExecutionHistory execution = historyRepository.findById(executionId).orElse(null);
        if (execution == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        ExecutionResponse response = ExecutionResponse.builder()
                .executionId(execution.getExecutionId())
                .status(execution.getStatus())
                .startTime(execution.getStartTime())
                .endTime(execution.getEndTime())
                .metricsCollected(execution.getMetricsCollectedCount())
                .publishedToSplunk(execution.getPublishedToSplunk())
                .build();

        return ResponseEntity.ok(response);
    }
}
