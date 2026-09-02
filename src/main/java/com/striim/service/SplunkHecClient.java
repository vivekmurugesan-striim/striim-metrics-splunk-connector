package com.striim.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class SplunkHecClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean publishMetrics(String splunkHecUrl, String token, String index, Map<String, Object> metrics) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String endpoint = splunkHecUrl.replaceFirst("/*$", "");
            HttpPost httpPost = new HttpPost(endpoint);

            httpPost.setHeader("Authorization", "Splunk " + token);
            httpPost.setHeader("Content-Type", "application/json");

            Map<String, Object> event = new HashMap<>();
            event.put("event", metrics);
            event.put("sourcetype", "_json");
            event.put("index", index);
            event.put("source", "striim-connector");
            event.put("time", Instant.now().getEpochSecond());

            String payload = objectMapper.writeValueAsString(event);
            httpPost.setEntity(new StringEntity(payload));

            return httpClient.execute(httpPost, response -> {
                int statusCode = response.getCode();
                String responseBody = org.apache.hc.core5.http.io.entity.EntityUtils.toString(response.getEntity());
                log.debug("Splunk HEC response status: {}", statusCode);

                if (statusCode < 200 || statusCode >= 300) {
                    log.error("Splunk HEC error ({}): {}", statusCode, responseBody);
                }

                return statusCode >= 200 && statusCode < 300;
            });
        } catch (Exception e) {
            log.error("Error publishing metrics to Splunk", e);
            return false;
        }
    }
}
