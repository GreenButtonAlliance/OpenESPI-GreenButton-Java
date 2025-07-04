/*
 *
 *    Copyright (c) 2018-2025 Green Button Alliance, Inc.
 *
 *    Portions (c) 2013-2018 EnergyOS.org
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package org.greenbuttonalliance.espi.thirdparty.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Modern WebClient service to replace legacy RestTemplate usage.
 * 
 * Provides blocking operations for synchronous code compatibility while
 * using the modern reactive WebClient underneath.
 */
@Service
public class WebClientService {

    private static final Logger logger = LoggerFactory.getLogger(WebClientService.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final WebClient webClient;

    public WebClientService() {
        this.webClient = WebClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.USER_AGENT, "OpenESPI-ThirdParty/3.5")
            .build();
    }

    /**
     * Creates a WebClient with basic authentication credentials.
     *
     * @param username the username
     * @param password the password
     * @return configured WebClient
     */
    public WebClient createAuthenticatedClient(String username, String password) {
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        
        return WebClient.builder()
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.USER_AGENT, "OpenESPI-ThirdParty/3.5")
            .build();
    }

    /**
     * Creates a WebClient with OAuth2 Bearer token.
     *
     * @param accessToken the OAuth2 access token
     * @return configured WebClient
     */
    public WebClient createOAuth2Client(String accessToken) {
        return WebClient.builder()
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.USER_AGENT, "OpenESPI-ThirdParty/3.5")
            .build();
    }

    /**
     * Creates a WebClient with Bearer token authentication (alias for createOAuth2Client).
     *
     * @param accessToken the OAuth2 access token
     * @return configured WebClient
     */
    public WebClient createAuthenticatedWebClient(String accessToken) {
        return createOAuth2Client(accessToken);
    }

    /**
     * Performs a blocking GET request.
     *
     * @param url the URL to request
     * @param responseType the expected response type
     * @param <T> the response type
     * @return the response body
     */
    public <T> T getForObject(String url, Class<T> responseType) {
        return getForObject(webClient, url, responseType);
    }

    /**
     * Performs a blocking GET request with authentication.
     *
     * @param client the configured WebClient (with auth)
     * @param url the URL to request
     * @param responseType the expected response type
     * @param <T> the response type
     * @return the response body
     */
    public <T> T getForObject(WebClient client, String url, Class<T> responseType) {
        try {
            logger.debug("Making GET request to: {}", url);
            
            return client.get()
                .uri(url)
                .retrieve()
                .bodyToMono(responseType)
                .timeout(DEFAULT_TIMEOUT)
                .block();
                
        } catch (WebClientResponseException e) {
            logger.error("HTTP error {} when calling {}: {}", 
                e.getStatusCode(), url, e.getResponseBodyAsString());
            handleHttpError(e, url);
            throw e;
        } catch (Exception e) {
            logger.error("Error making GET request to {}: {}", url, e.getMessage(), e);
            throw new RuntimeException("Failed to execute GET request to " + url, e);
        }
    }

    /**
     * Performs a blocking POST request.
     *
     * @param url the URL to post to
     * @param request the request body
     * @param responseType the expected response type
     * @param <T> the response type
     * @return the response body
     */
    public <T> T postForObject(String url, Object request, Class<T> responseType) {
        return postForObject(webClient, url, request, responseType);
    }

    /**
     * Performs a blocking POST request with authentication.
     *
     * @param client the configured WebClient (with auth)
     * @param url the URL to post to
     * @param request the request body
     * @param responseType the expected response type
     * @param <T> the response type
     * @return the response body
     */
    public <T> T postForObject(WebClient client, String url, Object request, Class<T> responseType) {
        try {
            logger.debug("Making POST request to: {}", url);
            
            return client.post()
                .uri(url)
                .bodyValue(request != null ? request : "")
                .retrieve()
                .bodyToMono(responseType)
                .timeout(DEFAULT_TIMEOUT)
                .block();
                
        } catch (WebClientResponseException e) {
            logger.error("HTTP error {} when posting to {}: {}", 
                e.getStatusCode(), url, e.getResponseBodyAsString());
            handleHttpError(e, url);
            throw e;
        } catch (Exception e) {
            logger.error("Error making POST request to {}: {}", url, e.getMessage(), e);
            throw new RuntimeException("Failed to execute POST request to " + url, e);
        }
    }

    /**
     * Performs a blocking PUT request with authentication.
     *
     * @param client the configured WebClient (with auth)
     * @param url the URL to put to
     * @param request the request body
     */
    public void put(WebClient client, String url, Object request) {
        try {
            logger.debug("Making PUT request to: {}", url);
            
            client.put()
                .uri(url)
                .bodyValue(request != null ? request : "")
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(DEFAULT_TIMEOUT)
                .block();
                
        } catch (WebClientResponseException e) {
            logger.error("HTTP error {} when putting to {}: {}", 
                e.getStatusCode(), url, e.getResponseBodyAsString());
            handleHttpError(e, url);
            throw e;
        } catch (Exception e) {
            logger.error("Error making PUT request to {}: {}", url, e.getMessage(), e);
            throw new RuntimeException("Failed to execute PUT request to " + url, e);
        }
    }

    /**
     * Performs a blocking DELETE request with authentication.
     *
     * @param client the configured WebClient (with auth)
     * @param url the URL to delete
     */
    public void delete(WebClient client, String url) {
        try {
            logger.debug("Making DELETE request to: {}", url);
            
            client.delete()
                .uri(url)
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(DEFAULT_TIMEOUT)
                .block();
                
        } catch (WebClientResponseException e) {
            logger.error("HTTP error {} when deleting {}: {}", 
                e.getStatusCode(), url, e.getResponseBodyAsString());
            handleHttpError(e, url);
            throw e;
        } catch (Exception e) {
            logger.error("Error making DELETE request to {}: {}", url, e.getMessage(), e);
            throw new RuntimeException("Failed to execute DELETE request to " + url, e);
        }
    }

    /**
     * Handles HTTP errors with appropriate logging.
     */
    private void handleHttpError(WebClientResponseException e, String url) {
        HttpStatus status = (HttpStatus) e.getStatusCode();
        
        switch (status) {
            case UNAUTHORIZED:
                logger.warn("Unauthorized access to {}: Check credentials", url);
                break;
            case FORBIDDEN:
                logger.warn("Forbidden access to {}: Insufficient permissions", url);
                break;
            case NOT_FOUND:
                logger.warn("Resource not found: {}", url);
                break;
            case BAD_REQUEST:
                logger.warn("Bad request to {}: {}", url, e.getResponseBodyAsString());
                break;
            case INTERNAL_SERVER_ERROR:
                logger.error("Server error when calling {}: {}", url, e.getResponseBodyAsString());
                break;
            default:
                logger.error("HTTP error {} when calling {}: {}", 
                    status, url, e.getResponseBodyAsString());
        }
    }
}