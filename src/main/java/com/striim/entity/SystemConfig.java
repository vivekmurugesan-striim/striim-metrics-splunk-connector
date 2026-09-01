package com.striim.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "system_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig {

    @Id
    @Column(length = 50)
    private String id;

    @Column(length = 255, nullable = false)
    private String striimUrl;

    @Column(name = "striim_token_enc", columnDefinition = "TEXT", nullable = false)
    private String striimTokenEnc;

    @Column(length = 255, nullable = false)
    private String splunkHecUrl;

    @Column(name = "splunk_token_enc", columnDefinition = "TEXT", nullable = false)
    private String splunkTokenEnc;

    @Column(length = 100, nullable = false)
    private String splunkIndex;

    @Column(name = "interval_seconds")
    private Integer intervalSeconds = 60;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
