package com.striim.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class StriimApiClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String cachedToken;
    private long tokenExpireTime;

    public Map<String, Object> callStriimApi(String striimUrl, String username, String password, String command) {
        try {
            String token = getOrRefreshToken(striimUrl, username, password);
            if (token == null || token.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to authenticate with Striim");
                return errorResponse;
            }

            return executeCommand(striimUrl, token, command);
        } catch (Exception e) {
            log.error("Error calling Striim API", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return errorResponse;
        }
    }

    private synchronized String getOrRefreshToken(String striimUrl, String username, String password) {
        long currentTime = System.currentTimeMillis();

        if (cachedToken != null && currentTime < tokenExpireTime) {
            log.debug("Using cached Striim token");
            return cachedToken;
        }

        log.info("Authenticating with Striim to get new token");
        return authenticateWithStriim(striimUrl, username, password);
    }

    private String authenticateWithStriim(String striimUrl, String username, String password) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String endpoint = striimUrl.replaceFirst("/*$", "") + "/security/authenticate";
            HttpPost httpPost = new HttpPost(endpoint);

            // Use form-encoded data (application/x-www-form-urlencoded)
            java.util.List<NameValuePair> params = new java.util.ArrayList<>();
            params.add(new BasicNameValuePair("username", username));
            params.add(new BasicNameValuePair("password", password));

            httpPost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
            httpPost.setHeader("Accept", "application/json");

            return httpClient.execute(httpPost, response -> {
                String responseBody = EntityUtils.toString(response.getEntity());
                log.debug("Striim authenticate response: {}", responseBody);

                if (response.getCode() == 200) {
                    Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                    String token = (String) responseMap.get("token");

                    if (token != null) {
                        cachedToken = token;
                        tokenExpireTime = System.currentTimeMillis() + (30 * 60 * 1000);
                        log.info("Successfully obtained Striim token, expires in 30 minutes");
                        return token;
                    }
                } else {
                    log.error("Failed to authenticate with Striim: status={}", response.getCode());
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error authenticating with Striim", e);
            return null;
        }
    }

    private Map<String, Object> executeCommand(String striimUrl, String token, String command) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String endpoint = striimUrl.replaceFirst("/*$", "") + "/api/v2/tungsten";
            HttpPost httpPost = new HttpPost(endpoint);

            httpPost.setHeader("Authorization", "STRIIM-TOKEN " + token);
            httpPost.setHeader("Content-Type", "text/plain");
            httpPost.setEntity(new StringEntity(command));

            return httpClient.execute(httpPost, response -> {
                String responseBody = EntityUtils.toString(response.getEntity());
                log.debug("Striim tungsten API response: {}", responseBody);

                try {
                    return objectMapper.readValue(responseBody, Map.class);
                } catch (Exception e) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("raw", responseBody);
                    return result;
                }
            });
        } catch (Exception e) {
            log.error("Error executing Striim command", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return errorResponse;
        }
    }
}
