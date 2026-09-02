package com.striim.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Striim API Client for executing mon commands and managing authentication.
 * Authenticates via /security/authenticate endpoint and executes commands via /api/v2/tungsten.
 */
@Service
@Slf4j
public class StriimApiClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String cachedToken;
    private long tokenExpireTime;

    /**
     * Execute a mon command against Striim and return the raw response
     * Handles authentication, token caching, and command execution
     */
    public String executeMonCommand(String striimUrl, String username, String password, String command) {
        try {
            String token = getOrRefreshToken(striimUrl, username, password);
            if (token == null || token.isEmpty()) {
                log.error("Failed to obtain authentication token");
                return null;
            }

            return executeCommandViaAPI(striimUrl, token, command);
        } catch (Exception e) {
            log.error("Error executing mon command: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get or refresh the cached token
     */
    private synchronized String getOrRefreshToken(String striimUrl, String username, String password) {
        long currentTime = System.currentTimeMillis();

        // Use cached token if still valid
        if (cachedToken != null && currentTime < tokenExpireTime) {
            log.debug("Using cached Striim token");
            return cachedToken;
        }

        log.info("Authenticating with Striim to obtain new token");
        return authenticateWithStriim(striimUrl, username, password);
    }

    /**
     * Authenticate with Striim using /security/authenticate endpoint
     * Sends form-encoded credentials (username and password)
     * Returns the session token from the response
     */
    private String authenticateWithStriim(String striimUrl, String username, String password) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String endpoint = striimUrl.replaceFirst("/*$", "") + "/security/authenticate";
            HttpPost httpPost = new HttpPost(endpoint);

            // Build form-encoded body with username and password
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("username", username));
            params.add(new BasicNameValuePair("password", password));

            httpPost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
            httpPost.setHeader("Accept", "application/json");

            return httpClient.execute(httpPost, response -> {
                String responseBody = EntityUtils.toString(response.getEntity());
                log.debug("Striim authenticate response: {}", responseBody);

                if (response.getCode() == 200) {
                    String token = extractTokenFromResponse(responseBody);
                    if (token != null && !token.isEmpty()) {
                        // Cache token for 55 minutes (tokens expire in 60 minutes)
                        cachedToken = token;
                        tokenExpireTime = System.currentTimeMillis() + (55 * 60 * 1000);
                        log.info("Successfully authenticated with Striim, token obtained and cached");
                        return token;
                    } else {
                        log.error("No token found in authentication response");
                    }
                } else {
                    log.error("Failed to authenticate with Striim: HTTP {}", response.getCode());
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error authenticating with Striim: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extract token from authentication response JSON
     * Looks for "token" field which is returned by Striim /security/authenticate endpoint
     */
    private String extractTokenFromResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // Striim returns "token" field
            if (jsonNode.has("token") && !jsonNode.get("token").isNull()) {
                String token = jsonNode.get("token").asText();
                if (!token.isEmpty()) {
                    return token;
                }
            }

            // Fallback to other possible field names
            if (jsonNode.has("sessionToken")) {
                return jsonNode.get("sessionToken").asText();
            }
            if (jsonNode.has("accessToken")) {
                return jsonNode.get("accessToken").asText();
            }

            log.warn("No token field found in response");
        } catch (Exception e) {
            log.error("Failed to parse authentication response: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Execute mon command via /api/v2/tungsten endpoint
     * Sends the command as plain text with Authorization: STRIIM-TOKEN header
     * Returns the raw response text from Striim
     */
    private String executeCommandViaAPI(String striimUrl, String token, String command) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String endpoint = striimUrl.replaceFirst("/*$", "") + "/api/v2/tungsten";
            HttpPost httpPost = new HttpPost(endpoint);

            // Set headers for mon command execution
            httpPost.setHeader("Authorization", "STRIIM-TOKEN " + token);
            httpPost.setHeader("Content-Type", "text/plain");

            // Send command as plain text body
            httpPost.setEntity(new StringEntity(command));

            log.info("Executing mon command: {}", command);

            return httpClient.execute(httpPost, response -> {
                String responseBody = EntityUtils.toString(response.getEntity());
                log.debug("Striim API response status: {}", response.getCode());

                if (response.getCode() == 200 || response.getCode() == 201) {
                    log.debug("Mon command executed successfully, response length: {} chars", responseBody.length());
                    return responseBody;
                } else {
                    log.error("Mon command failed with status {}: {}", response.getCode(), responseBody);
                    return null;
                }
            });
        } catch (Exception e) {
            log.error("Error executing mon command via API: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Clear cached token (call after 401 or token expiry)
     */
    public void clearCachedToken() {
        log.debug("Clearing cached Striim token");
        this.cachedToken = null;
        this.tokenExpireTime = 0;
    }
}
