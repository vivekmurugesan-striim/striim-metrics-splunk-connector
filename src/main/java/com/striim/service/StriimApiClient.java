package com.striim.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class StriimApiClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> callStriimApi(String striimUrl, String token, String command) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String endpoint = striimUrl.replaceFirst("/*$", "") + "/api/v2/tungsten";
            HttpPost httpPost = new HttpPost(endpoint);

            httpPost.setHeader("Authorization", "STRIIM-TOKEN " + token);
            httpPost.setHeader("Content-Type", "text/plain");
            httpPost.setEntity(new StringEntity(command));

            return httpClient.execute(httpPost, response -> {
                String responseBody = EntityUtils.toString(response.getEntity());
                log.debug("Striim API response: {}", responseBody);

                try {
                    return objectMapper.readValue(responseBody, Map.class);
                } catch (Exception e) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("raw", responseBody);
                    return result;
                }
            });
        } catch (Exception e) {
            log.error("Error calling Striim API", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return errorResponse;
        }
    }
}
