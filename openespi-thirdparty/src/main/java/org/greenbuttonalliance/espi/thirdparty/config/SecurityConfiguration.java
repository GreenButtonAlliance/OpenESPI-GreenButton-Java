/*
 *
 *        Copyright (c) 2025-2026 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.thirdparty.config;

import org.greenbuttonalliance.espi.thirdparty.security.RetailCustomerUserDetailsService;
import org.greenbuttonalliance.espi.thirdparty.security.ReturnToAuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

/**
 * Security configuration for OpenESPI Third Party (OAuth2 Client) application.
 * 
 * This configuration sets up the third party application as an OAuth2 client
 * that can authenticate with the authorization server and access protected resources.
 */
@Configuration
@EnableWebSecurity
// Makes the controllers' @PreAuthorize role checks (ROLE_USER / ROLE_CUSTODIAN) take effect.
@EnableMethodSecurity
public class SecurityConfiguration {

    /** BCrypt encoder for the TP's own accounts ({@code retail_customers.password}). */
    @Bean
    public PasswordEncoder customerPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * The one provider for TP form login, declared explicitly: the TP's accounts with the BCrypt
     * {@link #customerPasswordEncoder()}, whatever other {@code PasswordEncoder} beans exist. As an
     * {@code AuthenticationProvider} bean it becomes the global (parent) provider, so it must not also
     * be added to a chain with {@code http.authenticationProvider(...)}: a second copy re-checks every
     * failed login (two lookups, two BCrypt checks). Not {@code http.authenticationManager(...)}
     * either: that would drop the provider {@code oauth2Login} registers on the chain.
     */
    @Bean
    public DaoAuthenticationProvider customerAuthenticationProvider(RetailCustomerUserDetailsService userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(customerPasswordEncoder());
        return provider;
    }

    /**
     * Two separate steps (#249): a TP customer or administrator first signs in to the Third Party
     * with {@code formLogin} against the TP's own accounts ({@link #customerAuthenticationProvider});
     * {@code oauth2Login} is then used from the home page only to authorize the TP at a Data
     * Custodian, never to sign in to the TP.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           @Value("${spring.h2.console.path:/h2-console}") String h2ConsolePath)
            throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Public pages (Terms of Service and Usage Policy are linked from the navbar, About from
                // the footer)
                .requestMatchers("/", "/home", "/login", "/about", "/TermsOfService", "/UsagePolicy", "/error",
                        "/css/**", "/js/**", "/images/**").permitAll()
                // ESPI notification receipt: the Data Custodian POSTs a BatchList here without an
                // OAuth token — this endpoint is secured at the transport layer (TLS), not with an
                // OAuth access token. (The Third Party, as an OAuth client, presents its access token
                // only when it later fetches the source URLs carried in the BatchList.)
                .requestMatchers(HttpMethod.POST, "/espi/1_1/Notification").permitAll()
                // H2 console (spring.h2.console.path): closed here. Where the console is enabled
                // (spring.h2.console.enabled, development profiles only), H2ConsoleSecurityConfiguration's
                // chain handles it ahead of this one; everywhere else this rule keeps it shut.
                .requestMatchers(PathPatternRequestMatcher.withDefaults()
                        .matcher(H2ConsoleSecurityConfiguration.basePath(h2ConsolePath) + "/**")).denyAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(new ReturnToAuthenticationSuccessHandler())
                .failureUrl("/login?error")
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                // A refused or failed Data Custodian authorization is not a TP sign-in failure:
                // send the customer home with its own message, not to the login page's
                // "Invalid username or password".
                .failureUrl("/?authorization_error")
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                    .preload(true)
                )
                .contentTypeOptions(Customizer.withDefaults())
                    .referrerPolicy(referrerPolicyConfig -> referrerPolicyConfig.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // CSRF on for the browser UI (Thymeleaf th:action adds the token to every form, including
            // login and logout). Exempt: the DC's server-to-server Notification POST, which carries no
            // session or token.
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/espi/1_1/Notification")));

        return http.build();
    }

    /**
     * OAuth2 Authorized Client Manager for handling OAuth2 flows.
     * Supports authorization_code, refresh_token flows with automatic token refresh.
     */
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .authorizationCode()
                        .refreshToken()
                        .build();

        DefaultOAuth2AuthorizedClientManager authorizedClientManager =
                new DefaultOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientRepository);
        
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    /**
     * WebClient configured with OAuth2 support for making authenticated API calls.
     * This is the PREFERRED method for HTTP calls - replaces RestTemplate.
     */
    @Bean
    public WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        
        return WebClient.builder()
                .apply(oauth2Client.oauth2Configuration())
                .build();
    }

    /**
     * {@link RestClient.Builder} for components scanned from openespi-common (e.g.
     * {@code NotificationServiceImpl}) that inject it. Spring Boot's RestClient autoconfiguration
     * doesn't supply one in this context, so provide it explicitly.
     */
    @Bean
    public org.springframework.web.client.RestClient.Builder restClientBuilder() {
        return org.springframework.web.client.RestClient.builder();
    }

    /**
     * Legacy RestTemplate bean for backward compatibility.
     * TODO: Replace all RestTemplate usage with OAuth2-enabled WebClient.
     * @deprecated Use webClient() bean instead for OAuth2-aware HTTP calls.
     */
    @Bean("restTemplate")
    @Deprecated
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("https://localhost:*", "https://sandbox.greenbuttonalliance.org:*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}