package org.greenbuttonalliance.espi.thirdparty.subscription;

/** No such Authorization for the signed-in customer (also when it belongs to someone else): a 404. */
public class AuthorizationNotFoundException extends RuntimeException {

	public AuthorizationNotFoundException(java.util.UUID authorizationId) {
		super("No authorization " + authorizationId);
	}
}
