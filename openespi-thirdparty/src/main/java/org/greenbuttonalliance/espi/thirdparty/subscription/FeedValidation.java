package org.greenbuttonalliance.espi.thirdparty.subscription;

import org.greenbuttonalliance.espi.common.xml.EspiXmlSchemaValidator;

import java.util.List;

/**
 * The outcome of validating a fetched feed under a mode. {@code valid} is null when validation was off.
 */
public record FeedValidation(XmlValidationMode mode, Boolean valid, int errorCount,
							 List<EspiXmlSchemaValidator.Issue> issues) {

	public FeedValidation {
		issues = List.copyOf(issues);
	}

	/** True when the mode is ENFORCE and the feed failed validation. */
	public boolean blocksDisplay() {
		return mode == XmlValidationMode.ENFORCE && Boolean.FALSE.equals(valid);
	}
}
