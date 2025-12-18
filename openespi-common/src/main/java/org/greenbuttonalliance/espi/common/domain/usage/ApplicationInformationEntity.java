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

package org.greenbuttonalliance.espi.common.domain.usage;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.greenbuttonalliance.espi.common.domain.common.GrantType;
import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.greenbuttonalliance.espi.common.domain.common.ResponseType;
import org.greenbuttonalliance.espi.common.utils.encryption.FieldEncryptionConverter;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.proxy.HibernateProxy;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Pure JPA/Hibernate entity for ApplicationInformation without JAXB concerns.
 * 
 * Contains information about a Third Party Application requesting access to the 
 * DataCustodian services. Information includes Organization Name, Website, Contact Info, 
 * Application Name, Description, Icon, Type, default Notification and Callback endpoints.
 */
@Entity
@Table(name = "application_information")
@Getter
@Setter
@NoArgsConstructor
public class ApplicationInformationEntity extends IdentifiedObject {

    private static final long serialVersionUID = 1L;

    // ========================================
    // ESPI 4.0 XSD Fields in Sequence Order
    // Lines follow espi.xsd lines 62-246
    // ========================================

    /**
     * Data custodian ID.
     * ESPI 4.0 XSD field #1
     */
    @Size(min = 2, max = 64)
    @Column(name = "data_custodian_id")
    private String dataCustodianId;

    /**
     * Data custodian application status.
     * ESPI 4.0 XSD field #2
     */
    @Column(name = "data_custodian_application_status")
    private String dataCustodianApplicationStatus;

    /**
     * Third party application description.
     * ESPI 4.0 XSD field #3
     */
    @Column(name = "third_party_application_description")
    private String thirdPartyApplicationDescription;

    /**
     * Third party application status.
     */
    @Column(name = "third_party_application_status")
    private String thirdPartyApplicationStatus;

    /**
     * Third party application type.
     */
    @Column(name = "third_party_application_type")
    private String thirdPartyApplicationType;

    /**
     * Third party application use.
     */
    @Column(name = "third_party_application_use")
    private String thirdPartyApplicationUse;

    /**
     * Third party phone number.
     */
    @Column(name = "third_party_phone")
    private String thirdPartyPhone;

    /**
     * Authorization server URI.
     */
    @Column(name = "authorization_server_uri")
    private String authorizationServerUri;

    /**
     * Third party notification URI.
     */
    @Column(name = "third_party_notify_uri")
    private String thirdPartyNotifyUri;

    /**
     * Authorization server authorization endpoint.
     */
    @Column(name = "authorization_server_authorization_endpoint")
    private String authorizationServerAuthorizationEndpoint;

    /**
     * Authorization server registration endpoint.
     */
    @Column(name = "authorization_server_registration_endpoint")
    private String authorizationServerRegistrationEndpoint;

    /**
     * Authorization server token endpoint.
     */
    @Column(name = "authorization_server_token_endpoint")
    private String authorizationServerTokenEndpoint;

    /**
     * Data custodian bulk request URI.
     * ESPI 4.0 XSD field #14
     */
    @Column(name = "data_custodian_bulk_request_uri")
    private String dataCustodianBulkRequestURI;

    /**
     * Data custodian resource endpoint.
     * ESPI 4.0 XSD field #15
     */
    @Column(name = "data_custodian_resource_endpoint")
    private String dataCustodianResourceEndpoint;

    /**
     * Third party scope selection screen URI.
     * ESPI 4.0 XSD field #16
     */
    @Column(name = "third_party_scope_selection_screen_uri")
    private String thirdPartyScopeSelectionScreenURI;

    /**
     * Third party user portal screen URI.
     * ESPI 4.0 XSD field #17
     */
    @Column(name = "third_party_user_portal_screen_uri")
    private String thirdPartyUserPortalScreenURI;

    /**
     * Client secret for OAuth2 authentication.
     * Encrypted at rest using AES-256-GCM.
     */
    @Column(name = "client_secret")
    @Convert(converter = FieldEncryptionConverter.class)
    private String clientSecret;

    /**
     * Logo URI for the application.
     */
    @Column(name = "logo_uri")
    private String logoUri;

    /**
     * Client name for the application.
     */
    @Column(name = "client_name")
    private String clientName;

    /**
     * Client URI for the application.
     */
    @Column(name = "client_uri")
    private String clientUri;

    /**
     * Redirect URI for OAuth2 flow.
     */
    @Column(name = "redirect_uri")
    private String redirectUri;

    /**
     * Client ID for OAuth2 authentication.
     * Required field with size constraints.
     */
    @NotEmpty
    @Size(min = 2, max = 64)
    @Column(name = "client_id", nullable = false)
    private String clientId;

    /**
     * Terms of service URI.
     */
    @Column(name = "tos_uri")
    private String tosUri;

    /**
     * Privacy policy URI.
     */
    @Column(name = "policy_uri")
    private String policyUri;

    /**
     * Software ID identifier.
     */
    @Column(name = "software_id")
    private String softwareId;

    /**
     * Software version identifier.
     */
    @Column(name = "software_version")
    private String softwareVersion;

    /**
     * Timestamp when client ID was issued.
     */
    @Column(name = "client_id_issued_at")
    private Long clientIdIssuedAt;

    /**
     * Timestamp when client secret expires.
     */
    @Column(name = "client_secret_expires_at")
    private Long clientSecretExpiresAt;

    /**
     * Contact information for the application.
     */
    @Column(name = "contacts")
    private String contacts;

    /**
     * Token endpoint authentication method.
     */
    @Column(name = "token_endpoint_auth_method")
    private String tokenEndpointAuthMethod;

    /**
     * OAuth2 scopes for this application.
     * ESPI 4.0 XSD field #33
     */
    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    @CollectionTable(
        name = "application_information_scopes",
        joinColumns = @JoinColumn(name = "application_information_id")
    )
    @Column(name = "scope")
    private Set<String> scope = new HashSet<>();

    /**
     * OAuth2 grant types supported by this application.
     * ESPI 4.0 XSD field #34
     * FIXED: Changed from @JoinTable to @CollectionTable for @ElementCollection
     */
    @ElementCollection(targetClass = GrantType.class)
    @LazyCollection(LazyCollectionOption.FALSE)
    @CollectionTable(
        name = "application_information_grant_types",
        joinColumns = @JoinColumn(name = "application_information_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "grant_type")
    private Set<GrantType> grantTypes = new HashSet<>();

    /**
     * OAuth2 response types supported by this application.
     * ESPI 4.0 XSD field #35
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "response_types")
    private ResponseType responseTypes;

    /**
     * Registration client URI.
     * ESPI 4.0 XSD field #36
     */
    @Column(name = "registration_client_uri")
    private String registrationClientUri;

    /**
     * Registration access token.
     * ESPI 4.0 XSD field #37
     * Encrypted at rest using AES-256-GCM.
     */
    @Column(name = "registration_access_token")
    @Convert(converter = FieldEncryptionConverter.class)
    private String registrationAccessToken;

    /**
     * Data custodian scope selection screen URI.
     * ESPI 4.0 XSD field #38 (last field in XSD sequence)
     */
    @Column(name = "data_custodian_scope_selection_screen_uri")
    private String dataCustodianScopeSelectionScreenURI;

    /**
     * Gets the scope array for backward compatibility.
     *
     * @return array of scope strings
     */
    public String[] getScopeArray() {
        if (scope == null || scope.isEmpty()) {
            return new String[]{};
        }
        return scope.toArray(new String[0]);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        ApplicationInformationEntity that = (ApplicationInformationEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
                "id = " + getId() + ", " +
                "dataCustodianId = " + dataCustodianId + ", " +
                "dataCustodianApplicationStatus = " + dataCustodianApplicationStatus + ", " +
                "thirdPartyApplicationDescription = " + thirdPartyApplicationDescription + ", " +
                "thirdPartyApplicationStatus = " + thirdPartyApplicationStatus + ", " +
                "thirdPartyApplicationType = " + thirdPartyApplicationType + ", " +
                "thirdPartyApplicationUse = " + thirdPartyApplicationUse + ", " +
                "thirdPartyPhone = " + thirdPartyPhone + ", " +
                "authorizationServerUri = " + authorizationServerUri + ", " +
                "thirdPartyNotifyUri = " + thirdPartyNotifyUri + ", " +
                "authorizationServerAuthorizationEndpoint = " + authorizationServerAuthorizationEndpoint + ", " +
                "authorizationServerRegistrationEndpoint = " + authorizationServerRegistrationEndpoint + ", " +
                "authorizationServerTokenEndpoint = " + authorizationServerTokenEndpoint + ", " +
                "dataCustodianBulkRequestURI = " + dataCustodianBulkRequestURI + ", " +
                "dataCustodianResourceEndpoint = " + dataCustodianResourceEndpoint + ", " +
                "thirdPartyScopeSelectionScreenURI = " + thirdPartyScopeSelectionScreenURI + ", " +
                "thirdPartyUserPortalScreenURI = " + thirdPartyUserPortalScreenURI + ", " +
                "clientSecret = [PROTECTED], " +
                "logoUri = " + logoUri + ", " +
                "clientName = " + clientName + ", " +
                "clientUri = " + clientUri + ", " +
                "redirectUri = " + redirectUri + ", " +
                "clientId = " + clientId + ", " +
                "tosUri = " + tosUri + ", " +
                "policyUri = " + policyUri + ", " +
                "softwareId = " + softwareId + ", " +
                "softwareVersion = " + softwareVersion + ", " +
                "clientIdIssuedAt = " + clientIdIssuedAt + ", " +
                "clientSecretExpiresAt = " + clientSecretExpiresAt + ", " +
                "contacts = " + contacts + ", " +
                "tokenEndpointAuthMethod = " + tokenEndpointAuthMethod + ", " +
                "scope = " + scope + ", " +
                "grantTypes = " + grantTypes + ", " +
                "responseTypes = " + responseTypes + ", " +
                "registrationClientUri = " + registrationClientUri + ", " +
                "registrationAccessToken = [PROTECTED], " +
                "dataCustodianScopeSelectionScreenURI = " + dataCustodianScopeSelectionScreenURI + ", " +
                "description = " + getDescription() + ", " +
                "created = " + getCreated() + ", " +
                "updated = " + getUpdated() + ", " +
                "published = " + getPublished() + ")";
    }
}