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

package org.greenbuttonalliance.espi.thirdparty.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.Length;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * One saved fetch of a customer's {@code Batch/Subscription} feed (#89): the response as received,
 * with the HTTP status and the schema validation outcome. Third Party only; the Data Custodian never
 * gets this table.
 */
@Entity
@Table(name = "subscription_feed_fetches")
public class SubscriptionFeedFetchEntity implements Persistable<UUID> {

	/**
	 * The id is assigned in the constructor, so Spring Data cannot tell a new row by a null id: without this
	 * flag {@code save} would SELECT before every INSERT. False once persisted or loaded.
	 */
	@Transient
	private boolean newEntity = true;

	@Id
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(length = 36, columnDefinition = "char(36)", updatable = false, nullable = false)
	private UUID id;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "authorization_id", length = 36, columnDefinition = "char(36)", updatable = false, nullable = false)
	private UUID authorizationId;

	@Column(name = "fetched_at", nullable = false)
	private Instant fetchedAt;

	@Column(name = "resource_uri", length = 1024, nullable = false)
	private String resourceUri;

	@Column(name = "published_min")
	private Instant publishedMin;

	@Column(name = "published_max")
	private Instant publishedMax;

	@Column(name = "http_status", nullable = false)
	private int httpStatus;

	/** The response body: a whole feed, so far beyond VARCHAR (CLOB, LONGTEXT, TEXT). */
	@JdbcTypeCode(SqlTypes.LONGVARCHAR)
	@Column(name = "body", length = Length.LONG32)
	private String body;

	@Enumerated(EnumType.STRING)
	@Column(name = "validation_mode", length = 16, nullable = false)
	private XmlValidationMode validationMode;

	/** Null when validation was off. */
	@Column(name = "schema_valid")
	private Boolean schemaValid;

	@Column(name = "error_count", nullable = false)
	private int errorCount;

	protected SubscriptionFeedFetchEntity() {
		// JPA
	}

	public SubscriptionFeedFetchEntity(UUID authorizationId, Instant fetchedAt, String resourceUri,
									   Instant publishedMin, Instant publishedMax, int httpStatus, String body,
									   FeedValidation validation) {
		this.id = UUID.randomUUID();
		this.authorizationId = authorizationId;
		this.fetchedAt = fetchedAt;
		this.resourceUri = resourceUri;
		this.publishedMin = publishedMin;
		this.publishedMax = publishedMax;
		this.httpStatus = httpStatus;
		this.body = body;
		this.validationMode = validation.mode();
		this.schemaValid = validation.valid();
		this.errorCount = validation.errorCount();
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return newEntity;
	}

	@PostPersist
	@PostLoad
	void markNotNew() {
		this.newEntity = false;
	}

	public UUID getAuthorizationId() {
		return authorizationId;
	}

	public Instant getFetchedAt() {
		return fetchedAt;
	}

	public String getResourceUri() {
		return resourceUri;
	}

	public Instant getPublishedMin() {
		return publishedMin;
	}

	public Instant getPublishedMax() {
		return publishedMax;
	}

	public int getHttpStatus() {
		return httpStatus;
	}

	public String getBody() {
		return body;
	}

	public XmlValidationMode getValidationMode() {
		return validationMode;
	}

	public Boolean getSchemaValid() {
		return schemaValid;
	}

	public int getErrorCount() {
		return errorCount;
	}
}
