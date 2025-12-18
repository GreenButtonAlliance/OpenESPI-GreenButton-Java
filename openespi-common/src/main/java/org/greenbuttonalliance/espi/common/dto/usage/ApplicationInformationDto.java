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

/**
 * ApplicationInformation DTO record for JAXB XML marshalling/unmarshalling.
 *
 * Represents OAuth 2.0 application information for third-party access
 * to Green Button data.
 *
 * Field order strictly matches ESPI 4.0 XSD schema sequence (espi.xsd lines 62-246).
 */
@XmlRootElement(name = "ApplicationInformation", namespace = "http://naesb.org/espi")
@XmlAccessorType(XmlAccessType.PROPERTY)
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
public record ApplicationInformationDto(

    // Internal UUID (not in XSD)
    String uuid,

    // 1. dataCustodianId - Required
    String dataCustodianId,

    // 2. dataCustodianApplicationStatus - Required
    Short dataCustodianApplicationStatus,

    // 3. thirdPartyApplicationDescription - Optional
    String thirdPartyApplicationDescription,

    // 4. thirdPartyApplicationStatus - Optional
    Short thirdPartyApplicationStatus,

    // 5. thirdPartyApplicationType - Optional
    Short thirdPartyApplicationType,

    // 6. thirdPartyApplicationUse - Optional
    Short thirdPartyApplicationUse,

    // 7. thirdPartyPhone - Optional
    String thirdPartyPhone,

    // 8. authorizationServerUri - Optional
    String authorizationServerUri,

    // 9. thirdPartyNotifyUri - Required
    String thirdPartyNotifyURI,

    // 10. authorizationServerAuthorizationEndpoint - Required
    String authorizationServerAuthorizationEndpoint,

    // 11. authorizationServerRegistrationEndpoint - Optional
    String authorizationServerRegistrationEndpoint,

    // 12. authorizationServerTokenEndpoint - Required
    String authorizationServerTokenEndpoint,

    // 13. dataCustodianBulkRequestURI - Required
    String dataCustodianBulkRequestURI,

    // 14. dataCustodianResourceEndpoint - Required
    String dataCustodianResourceEndpoint,

    // 15. thirdPartyScopeSelectionScreenURI - Optional
    String thirdPartyScopeSelectionScreenURI,

    // 16. thirdPartyUserPortalScreenURI - Optional
    String thirdPartyUserPortalScreenURI,

    // 17. client_secret - Required
    String clientSecret,

    // 18. logo_uri - Optional
    String logoUri,

    // 19. client_name - Required
    String clientName,

    // 20. client_uri - Optional
    String clientUri,

    // 21. redirect_uri - Required (maxOccurs="unbounded")
    String redirectUri,

    // 22. client_id - Required
    String clientId,

    // 23. tos_uri - Optional
    String tosUri,

    // 24. policy_uri - Optional
    String policyUri,

    // 25. software_id - Required
    String softwareId,

    // 26. software_version - Required
    String softwareVersion,

    // 27. client_id_issued_at - Required
    Long clientIdIssuedAt,

    // 28. client_secret_expires_at - Required
    Long clientSecretExpiresAt,

    // 29. contacts - Optional (maxOccurs="unbounded")
    String contacts,

    // 30. token_endpoint_auth_method - Required
    String tokenEndpointAuthMethod,

    // 31. scope - Required (maxOccurs="unbounded")
    String scopes,

    // 32. grant_types - Required (minOccurs="2")
    String grantTypes,

    // 33. response_types - Required
    String responseTypes,

    // 34. registration_client_uri - Required
    String registrationClientUri,

    // 35. registration_access_token - Required
    String registrationAccessToken,

    // 36. dataCustodianScopeSelectionScreenURI - Deprecated
    String dataCustodianScopeSelectionScreenURI
) {

    /**
     * Default constructor for JAXB.
     */
    public ApplicationInformationDto() {
        this(null, null, null, null, null, null, null, null, null,
             null, null, null, null, null, null, null, null, null,
             null, null, null, null, null, null, null, null, null,
             null, null, null, null, null, null, null, null, null,
             null);
    }

    /**
     * Constructor for basic application information (minimum required fields).
     */
    public ApplicationInformationDto(String dataCustodianId, String clientId, String clientSecret, String clientName) {
        this(null, dataCustodianId, null, null, null, null, null, null, null,
             null, null, null, null, null, null, null, null, null,
             clientSecret, null, clientName, null, null, clientId, null, null,
             null, null, null, null, null, null, null, null, null,
             null, null);
    }

    // JAXB property accessors - must match propOrder sequence

    @XmlElement(name = "dataCustodianId", namespace = "http://naesb.org/espi")
    public String getDataCustodianId() { return dataCustodianId; }

    @XmlElement(name = "dataCustodianApplicationStatus", namespace = "http://naesb.org/espi")
    public Short getDataCustodianApplicationStatus() { return dataCustodianApplicationStatus; }

    @XmlElement(name = "thirdPartyApplicationDescription", namespace = "http://naesb.org/espi")
    public String getThirdPartyApplicationDescription() { return thirdPartyApplicationDescription; }

    @XmlElement(name = "thirdPartyApplicationStatus", namespace = "http://naesb.org/espi")
    public Short getThirdPartyApplicationStatus() { return thirdPartyApplicationStatus; }

    @XmlElement(name = "thirdPartyApplicationType", namespace = "http://naesb.org/espi")
    public Short getThirdPartyApplicationType() { return thirdPartyApplicationType; }

    @XmlElement(name = "thirdPartyApplicationUse", namespace = "http://naesb.org/espi")
    public Short getThirdPartyApplicationUse() { return thirdPartyApplicationUse; }

    @XmlElement(name = "thirdPartyPhone", namespace = "http://naesb.org/espi")
    public String getThirdPartyPhone() { return thirdPartyPhone; }

    @XmlElement(name = "authorizationServerUri", namespace = "http://naesb.org/espi")
    public String getAuthorizationServerUri() { return authorizationServerUri; }

    @XmlElement(name = "thirdPartyNotifyURI", namespace = "http://naesb.org/espi")
    public String getThirdPartyNotifyURI() { return thirdPartyNotifyURI; }

    @XmlElement(name = "authorizationServerAuthorizationEndpoint", namespace = "http://naesb.org/espi")
    public String getAuthorizationServerAuthorizationEndpoint() { return authorizationServerAuthorizationEndpoint; }

    @XmlElement(name = "authorizationServerRegistrationEndpoint", namespace = "http://naesb.org/espi")
    public String getAuthorizationServerRegistrationEndpoint() { return authorizationServerRegistrationEndpoint; }

    @XmlElement(name = "authorizationServerTokenEndpoint", namespace = "http://naesb.org/espi")
    public String getAuthorizationServerTokenEndpoint() { return authorizationServerTokenEndpoint; }

    @XmlElement(name = "dataCustodianBulkRequestURI", namespace = "http://naesb.org/espi")
    public String getDataCustodianBulkRequestURI() { return dataCustodianBulkRequestURI; }

    @XmlElement(name = "dataCustodianResourceEndpoint", namespace = "http://naesb.org/espi")
    public String getDataCustodianResourceEndpoint() { return dataCustodianResourceEndpoint; }

    @XmlElement(name = "thirdPartyScopeSelectionScreenURI", namespace = "http://naesb.org/espi")
    public String getThirdPartyScopeSelectionScreenURI() { return thirdPartyScopeSelectionScreenURI; }

    @XmlElement(name = "thirdPartyUserPortalScreenURI", namespace = "http://naesb.org/espi")
    public String getThirdPartyUserPortalScreenURI() { return thirdPartyUserPortalScreenURI; }

    @XmlElement(name = "client_secret", namespace = "http://naesb.org/espi")
    public String getClientSecret() { return clientSecret; }

    @XmlElement(name = "logo_uri", namespace = "http://naesb.org/espi")
    public String getLogoUri() { return logoUri; }

    @XmlElement(name = "client_name", namespace = "http://naesb.org/espi")
    public String getClientName() { return clientName; }

    @XmlElement(name = "client_uri", namespace = "http://naesb.org/espi")
    public String getClientUri() { return clientUri; }

    @XmlElement(name = "redirect_uri", namespace = "http://naesb.org/espi")
    public String getRedirectUri() { return redirectUri; }

    @XmlElement(name = "client_id", namespace = "http://naesb.org/espi")
    public String getClientId() { return clientId; }

    @XmlElement(name = "tos_uri", namespace = "http://naesb.org/espi")
    public String getTosUri() { return tosUri; }

    @XmlElement(name = "policy_uri", namespace = "http://naesb.org/espi")
    public String getPolicyUri() { return policyUri; }

    @XmlElement(name = "software_id", namespace = "http://naesb.org/espi")
    public String getSoftwareId() { return softwareId; }

    @XmlElement(name = "software_version", namespace = "http://naesb.org/espi")
    public String getSoftwareVersion() { return softwareVersion; }

    @XmlElement(name = "client_id_issued_at", namespace = "http://naesb.org/espi")
    public Long getClientIdIssuedAt() { return clientIdIssuedAt; }

    @XmlElement(name = "client_secret_expires_at", namespace = "http://naesb.org/espi")
    public Long getClientSecretExpiresAt() { return clientSecretExpiresAt; }

    @XmlElement(name = "contacts", namespace = "http://naesb.org/espi")
    public String getContacts() { return contacts; }

    @XmlElement(name = "token_endpoint_auth_method", namespace = "http://naesb.org/espi")
    public String getTokenEndpointAuthMethod() { return tokenEndpointAuthMethod; }

    @XmlElement(name = "scope", namespace = "http://naesb.org/espi")
    public String getScopes() { return scopes; }

    @XmlElement(name = "grant_types", namespace = "http://naesb.org/espi")
    public String getGrantTypes() { return grantTypes; }

    @XmlElement(name = "response_types", namespace = "http://naesb.org/espi")
    public String getResponseTypes() { return responseTypes; }

    @XmlElement(name = "registration_client_uri", namespace = "http://naesb.org/espi")
    public String getRegistrationClientUri() { return registrationClientUri; }

    @XmlElement(name = "registration_access_token", namespace = "http://naesb.org/espi")
    public String getRegistrationAccessToken() { return registrationAccessToken; }

    @XmlElement(name = "dataCustodianScopeSelectionScreenURI", namespace = "http://naesb.org/espi")
    public String getDataCustodianScopeSelectionScreenURI() { return dataCustodianScopeSelectionScreenURI; }
}
