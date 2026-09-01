package com.striim.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "execution_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionHistory {

    @Id
    @Column(length = 50)
    private String executionId;

    @Column(length = 20, nullable = false)
    private String triggerType;

    @Column(length = 20, nullable = false)
    private String status;

    @Column(name = "metrics_collected_count")
    private Integer metricsCollectedCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "published_to_splunk")
    private Boolean publishedToSplunk = false;
}
