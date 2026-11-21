package org.greenbuttonalliance.espi.thirdparty.subscription;

/**
 * What the Third Party does with the XSD validation of a fetched feed (espi.thirdparty.xml-validation):
 * OFF skips it, REPORT validates and lists the issues but still shows the data, ENFORCE refuses to
 * show a feed that does not validate.
 */
public enum XmlValidationMode {
	OFF, REPORT, ENFORCE
}
