/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.common.xml;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.greenbuttonalliance.espi.common.dto.usage.BatchListDto;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Single canonical codec for the ESPI {@code BatchList} notification wire format (#158).
 *
 * <p>The Data Custodian→Third Party notification seam exchanges a {@code BatchList} XML document
 * (a list of resource URIs to fetch). This codec is the <em>single source of truth</em> for that
 * serialization: the DC sender ({@code NotificationServiceImpl}) marshals through it and the TP
 * receiver ({@code NotificationController}) unmarshals through it, so the two sides cannot drift.
 * It is the BatchList analogue of {@link org.greenbuttonalliance.espi.common.uri.EspiBatchUri}
 * (the canonical Batch-URI codec) and is likewise a stateless static utility — no Spring wiring.</p>
 *
 * <p>Per the project's JAXB rule the wire type is the JAXB-annotated {@link BatchListDto}
 * ({@code @XmlRootElement(name = "BatchList", namespace = "http://naesb.org/espi")}), never the
 * JPA entity. The {@link JAXBContext} is created once and cached (contexts are thread-safe;
 * marshaller/unmarshaller instances are created per call as they are not).</p>
 */
public final class BatchListXmlCodec {

    /** ESPI namespace of the {@code BatchList} root element. */
    public static final String ESPI_NAMESPACE = "http://naesb.org/espi";

    /** Local name of the root element. */
    public static final String ROOT_ELEMENT = "BatchList";

    private static final JAXBContext CONTEXT = createContext();

    private BatchListXmlCodec() {
        // static utility
    }

    private static JAXBContext createContext() {
        try {
            return JAXBContext.newInstance(BatchListDto.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to initialize BatchList JAXB context", e);
        }
    }

    /**
     * Marshals a {@link BatchListDto} to its ESPI {@code BatchList} XML string.
     *
     * @param batchList the batch list to marshal (must not be {@code null})
     * @return the UTF-8 XML document as a string
     * @throws IllegalArgumentException if {@code batchList} is {@code null}
     * @throws IllegalStateException    if marshalling fails
     */
    public static String marshal(BatchListDto batchList) {
        if (batchList == null) {
            throw new IllegalArgumentException("batchList must not be null");
        }
        try {
            Marshaller marshaller = CONTEXT.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter writer = new StringWriter();
            marshaller.marshal(batchList, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to marshal BatchList", e);
        }
    }

    /**
     * Unmarshals an ESPI {@code BatchList} XML string to a {@link BatchListDto}.
     *
     * @param xml the XML document (must not be {@code null})
     * @return the parsed batch list
     * @throws IllegalArgumentException if {@code xml} is {@code null}
     * @throws IllegalStateException    if unmarshalling fails
     */
    public static BatchListDto unmarshal(String xml) {
        if (xml == null) {
            throw new IllegalArgumentException("xml must not be null");
        }
        try {
            Unmarshaller unmarshaller = CONTEXT.createUnmarshaller();
            return unmarshaller.unmarshal(
                    new StreamSource(new StringReader(xml)), BatchListDto.class).getValue();
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to unmarshal BatchList", e);
        }
    }
}
