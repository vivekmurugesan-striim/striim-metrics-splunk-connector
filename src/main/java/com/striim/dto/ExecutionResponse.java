package com.striim.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionResponse {
    private String executionId;
    private String status;
    private Instant startTime;
    private Instant endTime;
    private Integer metricsCollected;
    private Boolean publishedToSplunk;
}
