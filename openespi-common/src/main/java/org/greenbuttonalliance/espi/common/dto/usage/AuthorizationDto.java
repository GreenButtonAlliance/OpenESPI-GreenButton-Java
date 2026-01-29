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

package org.greenbuttonalliance.espi.common.dto.usage;

import org.greenbuttonalliance.espi.common.dto.common.DateTimeIntervalDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Authorization DTO class for JAXB XML marshalling/unmarshalling.
 *
 * Represents OAuth 2.0 authorization for third-party access to Green Button data.
 * Complies with NAESB ESPI 4.0 XSD specification.
 *
 * @see <a href="https://www.naesb.org/ESPI_Standards.asp">NAESB ESPI 4.0</a>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Authorization", namespace = "http://naesb.org/espi")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Authorization", namespace = "http://naesb.org/espi", propOrder = {
    "authorizedPeriod",     // ESPI 4.0 XSD sequence
    "publishedPeriod",
    "status",
    "expiresIn",            // Maps to expires_at in XML
    "grantType",
    "scope",
    "tokenType",
    "error",
    "errorDescription",
    "errorUri",
    "resourceURI",
    "authorizationUri",
    "customerResourceURI"   // NEW - PII subscription URI
})
public class AuthorizationDto {

    // XSD-compliant fields (in order)

    @Schema(description = "Period during which this authorization is valid",
            example = "{\"start\": 1704067200, \"duration\": 31536000}")
    private DateTimeIntervalDto authorizedPeriod;

    @Schema(description = "Period during which data was published",
            example = "{\"start\": 1704067200, \"duration\": 31536000}")
    private DateTimeIntervalDto publishedPeriod;

    @Schema(description = "Authorization status (1=ACTIVE, 2=REVOKED, 3=EXPIRED, 4=PENDING)",
            example = "1")
    private Short status;

    @Schema(description = "Expiration timestamp (epoch seconds)",
            example = "1735689600")
    private Long expiresIn;

    @Schema(description = "OAuth2 grant type",
            example = "authorization_code",
            allowableValues = {"authorization_code", "client_credentials", "refresh_token"})
    private String grantType;

    @Schema(description = "OAuth2 scope defining permissions",
            example = "FB=1_3_4_5_13_14_39;IntervalDuration=3600",
            required = true)
    private String scope;

    @Schema(description = "OAuth2 token type",
            example = "Bearer",
            allowableValues = {"Bearer"})
    private String tokenType;

    @Schema(description = "OAuth2 error code if authorization failed",
            example = "invalid_grant")
    private String error;

    @Schema(description = "Human-readable error description",
            example = "The provided authorization grant is invalid")
    private String errorDescription;

    @Schema(description = "URI with more information about the error",
            example = "https://example.com/oauth/errors/invalid_grant")
    private String errorUri;

    @Schema(description = "URI for accessing the authorized energy usage resource",
            example = "https://api.example.com/espi/1_1/resource/Batch/Subscription/12345",
            required = true)
    private String resourceURI;

    @Schema(description = "URI for managing this authorization",
            example = "https://api.example.com/espi/1_1/resource/Authorization/67890",
            required = true)
    private String authorizationUri;

    @Schema(description = "URI for accessing PII data the Third Party is authorized to access. Points to PII resource subscription endpoint with different namespace than resourceURI.",
            example = "https://api.example.com/customer/espi/1_1/resource/Batch/RetailCustomer/12345")
    private String customerResourceURI;

    // OAuth2 implementation fields (not in ESPI XSD - marked as @XmlTransient)

    @XmlTransient
    @Schema(description = "OAuth2 access token (not included in XML for security)",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @XmlTransient
    @Schema(description = "OAuth2 refresh token (not included in XML for security)",
            example = "def50200...")
    private String refreshToken;

    @XmlTransient
    @Schema(description = "OAuth2 authorization code (temporary, not in XML)",
            example = "abc123xyz")
    private String authorizationCode;

    @XmlTransient
    @Schema(description = "OAuth2 state parameter for CSRF protection (not in ESPI XSD)",
            example = "xyz789")
    private String state;

    @XmlTransient
    @Schema(description = "OAuth2 response type (not in ESPI XSD)",
            example = "code")
    private String responseType;

    @XmlTransient
    @Schema(description = "Third party application identifier (not in ESPI XSD)",
            example = "ThirdPartyApp")
    private String thirdParty;

    @XmlTransient
    @Schema(description = "Application information ID (relationship, not in XSD)",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String applicationInformationId;

    @XmlTransient
    @Schema(description = "Retail customer ID (relationship, not in XSD for privacy)",
            example = "660e8400-e29b-41d4-a716-446655440000")
    private String retailCustomerId;

    // JAXB property accessors for XSD-compliant fields (in propOrder sequence)

    // OAuth2 implementation field accessors (marked @XmlTransient, not in XML output)

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getAuthorizationCode() { return authorizationCode; }
    public String getState() { return state; }
    public String getResponseType() { return responseType; }
    public String getThirdParty() { return thirdParty; }
    public String getApplicationInformationId() { return applicationInformationId; }
    public String getRetailCustomerId() { return retailCustomerId; }
}