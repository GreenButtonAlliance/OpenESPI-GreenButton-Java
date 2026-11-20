package org.greenbuttonalliance.espi.thirdparty.subscription;

/** The bytes of a fetched feed do not unmarshal to an Atom feed. */
public class SubscriptionFeedReadException extends RuntimeException {

	public SubscriptionFeedReadException(String message, Throwable cause) {
		super(message, cause);
	}
}
