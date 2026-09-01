package com.striim.service;

import com.striim.dto.ConfigRequest;
import com.striim.dto.ConfigResponse;
import com.striim.entity.SystemConfig;
import com.striim.repository.SystemConfigRepository;
import com.striim.util.EncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
@Slf4j
public class ConfigService {

    @Autowired
    private SystemConfigRepository configRepository;

    public ConfigResponse saveConfiguration(ConfigRequest request) {
        try {
            SystemConfig config = new SystemConfig();
            config.setId("default");
            config.setStriimUrl(request.getStriimUrl());
            config.setStriimUser(request.getStriimUser());
            config.setStriimPasswordEnc(EncryptionUtil.encrypt(request.getStriimPassword()));
            config.setSplunkHecUrl(request.getSplunkHecUrl());
            config.setSplunkTokenEnc(EncryptionUtil.encrypt(request.getSplunkToken()));
            config.setSplunkIndex(request.getSplunkIndex());
            config.setIntervalSeconds(request.getCollectionIntervalSeconds() != null ?
                    request.getCollectionIntervalSeconds() : 60);

            SystemConfig saved = configRepository.save(config);

            log.info("Configuration saved successfully: id={}", saved.getId());

            return ConfigResponse.builder()
                    .status("SUCCESS")
                    .message("Configuration saved successfully")
                    .configId(saved.getId())
                    .updatedAt(saved.getUpdatedAt())
                    .build();
        } catch (Exception e) {
            log.error("Error saving configuration", e);
            return ConfigResponse.builder()
                    .status("ERROR")
                    .message("Failed to save configuration: " + e.getMessage())
                    .build();
        }
    }

    public SystemConfig getConfiguration() {
        return configRepository.findById("default").orElse(null);
    }
}
