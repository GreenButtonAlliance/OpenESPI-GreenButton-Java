/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
 *
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

package org.greenbuttonalliance.espi.thirdparty.web;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;

/**
 * Modern WebClient-based replacement for ClientRestTemplate
 * Uses Spring WebFlux WebClient for OAuth2 API calls
 */
@Component
public class ClientWebClient {

    private final WebClient webClient;

    public ClientWebClient() {
        this.webClient = WebClient.builder().build();
    }

    public ClientWebClient(String username, String password) {
        String credentials = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                .build();
    }

    public ClientWebClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * GET request with OAuth2 Bearer token
     */
    public Mono<String> get(String uri, String bearerToken) {
        return webClient.get()
                .uri(uri)
                .headers(headers -> headers.setBearerAuth(bearerToken))
                .retrieve()
                .bodyToMono(String.class);
    }

    /**
     * GET request with basic authentication
     */
    public Mono<String> getWithBasicAuth(String uri) {
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class);
    }

    /**
     * POST request with OAuth2 Bearer token
     */
    public Mono<String> post(String uri, String bearerToken, Object body) {
        return webClient.post()
                .uri(uri)
                .headers(headers -> headers.setBearerAuth(bearerToken))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class);
    }

    /**
     * Get the underlying WebClient for custom operations
     */
    public WebClient getWebClient() {
        return webClient;
    }
}