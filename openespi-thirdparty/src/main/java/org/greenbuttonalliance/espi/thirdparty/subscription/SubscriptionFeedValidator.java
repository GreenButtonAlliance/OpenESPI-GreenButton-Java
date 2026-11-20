package org.greenbuttonalliance.espi.thirdparty.subscription;

import org.greenbuttonalliance.espi.common.xml.EspiXmlSchemaValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/** Validates a fetched Batch/Subscription feed against the ESPI schemas under the configured mode. */
@Component
public class SubscriptionFeedValidator {

	private static final Logger log = LoggerFactory.getLogger(SubscriptionFeedValidator.class);

	private final XmlValidationMode mode;

	public SubscriptionFeedValidator(@Value("${espi.thirdparty.xml-validation:report}") XmlValidationMode mode) {
		this.mode = mode;
	}

	public XmlValidationMode mode() {
		return mode;
	}

	/** Validates the feed as received, under the configured mode. */
	public FeedValidation validate(byte[] xml) {
		return mode == XmlValidationMode.OFF ? skipped(mode) : toValidation(mode, EspiXmlSchemaValidator.instance().validate(xml));
	}

	/** Validates the saved XML text under the mode it was saved with, not the configured one. */
	public FeedValidation revalidate(String xml, XmlValidationMode savedMode) {
		return savedMode == XmlValidationMode.OFF ? skipped(savedMode)
				: toValidation(savedMode, EspiXmlSchemaValidator.instance().validate(xml));
	}

	private static FeedValidation skipped(XmlValidationMode mode) {
		return new FeedValidation(mode, null, 0, List.of());
	}

	private static FeedValidation toValidation(XmlValidationMode mode, EspiXmlSchemaValidator.Result result) {
		log.info("Feed validation ({}): valid={}, issueCount={}", mode, result.valid(), result.issues().size());
		result.issues().forEach(issue -> log.debug("Feed validation issue: {}", issue));
		return new FeedValidation(mode, result.valid(), (int) result.errorCount(), result.issues());
	}
}
