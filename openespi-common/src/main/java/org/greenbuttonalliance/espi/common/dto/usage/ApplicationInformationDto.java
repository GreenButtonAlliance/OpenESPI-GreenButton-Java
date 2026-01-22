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

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ApplicationInformation DTO class for JAXB XML marshalling/unmarshalling.
 *
 * Represents OAuth 2.0 application information for third-party access
 * to Green Button data.
 *
 * Field order strictly matches ESPI 4.0 XSD schema sequence (espi.xsd lines 62-246).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "ApplicationInformation", namespace = "http://naesb.org/espi")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ApplicationInformation", namespace = "http://naesb.org/espi", propOrder = {
    // Core identification
    "dataCustodianId",
    "dataCustodianApplicationStatus",

    // Third Party Application Details
    "thirdPartyApplicationDescription",
    "thirdPartyApplicationStatus",
    "thirdPartyApplicationType",
    "thirdPartyApplicationUse",
    "thirdPartyPhone",

    // Authorization Server URIs
    "authorizationServerUri",
    "thirdPartyNotifyURI",
    "authorizationServerAuthorizationEndpoint",
    "authorizationServerRegistrationEndpoint",
    "authorizationServerTokenEndpoint",

    // Data Custodian Endpoints
    "dataCustodianBulkRequestURI",
    "dataCustodianResourceEndpoint",

    // UI Screen URIs
    "thirdPartyScopeSelectionScreenURI",
    "thirdPartyUserPortalScreenURI",

    // OAuth 2.0 Client Credentials
    "clientSecret",        // client_secret in XSD
    "logoUri",             // logo_uri in XSD
    "clientName",          // client_name in XSD
    "clientUri",           // client_uri in XSD
    "redirectUri",         // redirect_uri in XSD
    "clientId",            // client_id in XSD
    "tosUri",              // tos_uri in XSD
    "policyUri",           // policy_uri in XSD
    "softwareId",          // software_id in XSD
    "softwareVersion",     // software_version in XSD
    "clientIdIssuedAt",    // client_id_issued_at in XSD
    "clientSecretExpiresAt", // client_secret_expires_at in XSD

    // OAuth 2.0 Additional Fields
    "contacts",
    "tokenEndpointAuthMethod",
    "scopes",              // scope in XSD (maxOccurs="unbounded")
    "grantTypes",          // grant_types in XSD (minOccurs="2")
    "responseTypes",       // response_types in XSD

    // Registration
    "registrationClientUri",
    "registrationAccessToken",

    // Deprecated (kept for backward compatibility)
    "dataCustodianScopeSelectionScreenURI"
})
public class ApplicationInformationDto {

    // Internal UUID (not in XSD)
    @XmlTransient
    private String uuid;

    // 1. dataCustodianId - Required
    private String dataCustodianId;

    // 2. dataCustodianApplicationStatus - Required
    private Short dataCustodianApplicationStatus;

    // 3. thirdPartyApplicationDescription - Optional
    private String thirdPartyApplicationDescription;

    // 4. thirdPartyApplicationStatus - Optional
    private Short thirdPartyApplicationStatus;

    // 5. thirdPartyApplicationType - Optional
    private Short thirdPartyApplicationType;

    // 6. thirdPartyApplicationUse - Optional
    private Short thirdPartyApplicationUse;

    // 7. thirdPartyPhone - Optional
    private String thirdPartyPhone;

    // 8. authorizationServerUri - Optional
    private String authorizationServerUri;

    // 9. thirdPartyNotifyUri - Required
    private String thirdPartyNotifyURI;

    // 10. authorizationServerAuthorizationEndpoint - Required
    private String authorizationServerAuthorizationEndpoint;

    // 11. authorizationServerRegistrationEndpoint - Optional
    private String authorizationServerRegistrationEndpoint;

    // 12. authorizationServerTokenEndpoint - Required
    private String authorizationServerTokenEndpoint;

    // 13. dataCustodianBulkRequestURI - Required
    private String dataCustodianBulkRequestURI;

    // 14. dataCustodianResourceEndpoint - Required
    private String dataCustodianResourceEndpoint;

    // 15. thirdPartyScopeSelectionScreenURI - Optional
    private String thirdPartyScopeSelectionScreenURI;

    // 16. thirdPartyUserPortalScreenURI - Optional
    private String thirdPartyUserPortalScreenURI;

    // 17. client_secret - Required
    private String clientSecret;

    // 18. logo_uri - Optional
    private String logoUri;

    // 19. client_name - Required
    private String clientName;

    // 20. client_uri - Optional
    private String clientUri;

    // 21. redirect_uri - Required (maxOccurs="unbounded")
    private String redirectUri;

    // 22. client_id - Required
    private String clientId;

    // 23. tos_uri - Optional
    private String tosUri;

    // 24. policy_uri - Optional
    private String policyUri;

    // 25. software_id - Required
    private String softwareId;

    // 26. software_version - Required
    private String softwareVersion;

    // 27. client_id_issued_at - Required
    private Long clientIdIssuedAt;

    // 28. client_secret_expires_at - Required
    private Long clientSecretExpiresAt;

    // 29. contacts - Optional (maxOccurs="unbounded")
    private String contacts;

    // 30. token_endpoint_auth_method - Required
    private String tokenEndpointAuthMethod;

    // 31. scope - Required (maxOccurs="unbounded")
    private String scopes;

    // 32. grant_types - Required (minOccurs="2")
    private String grantTypes;

    // 33. response_types - Required
    private String responseTypes;

    // 34. registration_client_uri - Required
    private String registrationClientUri;

    // 35. registration_access_token - Required
    private String registrationAccessToken;

    // 36. dataCustodianScopeSelectionScreenURI - Deprecated
    private String dataCustodianScopeSelectionScreenURI;
}
