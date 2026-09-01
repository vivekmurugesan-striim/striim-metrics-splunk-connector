package com.striim.controller;

import com.striim.dto.ExecutionResponse;
import com.striim.dto.HistoryResponse;
import com.striim.entity.ExecutionHistory;
import com.striim.repository.ExecutionHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/history")
@Slf4j
public class HistoryController {

    @Autowired
    private ExecutionHistoryRepository historyRepository;

    @GetMapping
    public ResponseEntity<HistoryResponse> getExecutionHistory() {
        List<ExecutionHistory> executions = historyRepository.findRecentExecutions();

        List<ExecutionResponse> runs = executions.stream()
                .map(e -> ExecutionResponse.builder()
                        .executionId(e.getExecutionId())
                        .status(e.getStatus())
                        .startTime(e.getStartTime())
                        .endTime(e.getEndTime())
                        .metricsCollected(e.getMetricsCollectedCount())
                        .publishedToSplunk(e.getPublishedToSplunk())
                        .build())
                .collect(Collectors.toList());

        HistoryResponse response = HistoryResponse.builder()
                .totalRecords(runs.size())
                .runs(runs)
                .build();

        return ResponseEntity.ok(response);
    }
}
