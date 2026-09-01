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
public class ConfigResponse {
    private String status;
    private String message;
    private String configId;
    private Instant updatedAt;
}
