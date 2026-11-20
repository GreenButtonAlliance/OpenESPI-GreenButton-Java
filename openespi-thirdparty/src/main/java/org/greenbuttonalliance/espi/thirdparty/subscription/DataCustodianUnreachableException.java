package org.greenbuttonalliance.espi.thirdparty.subscription;

/** The Data Custodian could not be reached (connection failure, not an HTTP error status). */
public class DataCustodianUnreachableException extends RuntimeException {

	public DataCustodianUnreachableException(String message, Throwable cause) {
		super(message, cause);
	}
}
