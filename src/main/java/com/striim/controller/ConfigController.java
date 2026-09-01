package com.striim.controller;

import com.striim.dto.ConfigRequest;
import com.striim.dto.ConfigResponse;
import com.striim.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/config")
@Slf4j
public class ConfigController {

    @Autowired
    private ConfigService configService;

    @PostMapping
    public ResponseEntity<ConfigResponse> saveConfiguration(@RequestBody ConfigRequest request) {
        log.info("Saving configuration");
        ConfigResponse response = configService.saveConfiguration(request);
        HttpStatus status = "SUCCESS".equals(response.getStatus()) ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getConfiguration() {
        var config = configService.getConfiguration();
        if (config == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ConfigResponse("NOT_FOUND", "Configuration not found", null, null));
        }
        return ResponseEntity.ok(config);
    }
}
