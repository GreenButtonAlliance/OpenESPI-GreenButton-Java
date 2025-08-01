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

package org.greenbuttonalliance.espi.common.utils;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * <p>Serialized form of <code>javax.xml.datatype.XMLGregorianCalendar</code>.</p>
 *
 * @author Michael Glavassevich, IBM
 * @version $Id: SerializedXMLGregorianCalendar.java 756261 2009-03-19 23:35:50Z mrglavas $
 */
final class SerializedXMLGregorianCalendar implements Serializable {

    private static final long serialVersionUID = -7752272381890705397L;
    private final String lexicalValue;

    public SerializedXMLGregorianCalendar(String lexicalValue) {
        this.lexicalValue = lexicalValue;
    }

    private Object readResolve() throws ObjectStreamException {
        return new DatatypeFactoryImpl().newXMLGregorianCalendar(lexicalValue);
    }
}
