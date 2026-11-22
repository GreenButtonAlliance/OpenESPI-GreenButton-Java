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

package org.greenbuttonalliance.espi.datacustodian.config;


import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.xml.MarshallingHttpMessageConverter;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.servlet.config.annotation.*;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;

/**
 * Web configuration for the OpenESPI Data Custodian Resource Server.
 * 
 * This configuration replaces the legacy Spring MVC XML configuration with Spring Boot 4
 * web configuration and ESPI-specific customizations.
 *
 * Key Features:
 * - JAXB XML marshalling for ESPI Atom feeds
 * - JSON serialization for the internal back-channel
 * - Content negotiation on the Accept header: ESPI resources are application/atom+xml only (#312)
 * - CORS and static resource handling
 */
@Configuration
@EnableWebMvc
public class WebConfiguration implements WebMvcConfigurer {

    @Value("${espi.xml.pretty-print:true}")
    private boolean xmlPrettyPrint;

    @Value("${espi.xml.include-namespaces:true}")
    private boolean includeNamespaces;

    /**
     * Configure HTTP message converters for XML and JSON.
     */
    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        // Add JAXB XML converter for ESPI Atom feeds
        builder.withXmlConverter(createXmlConverter());

        // Add JSON converter with proper date handling
        builder.withJsonConverter(createJsonConverter3());

        WebMvcConfigurer.super.configureMessageConverters(builder);
    }

    /**
     * Configure content negotiation for ESPI endpoints.
     *
     * <p>A request without an {@code Accept} header accepts {@code application/atom+xml} first, the only type
     * the ESPI resource endpoints produce (#312), then {@code application/json}, which the internal back-channel
     * produces.</p>
     *
     * <p>Only the {@code Accept} header negotiates: there is no format parameter and no path extension, so no
     * extension-to-type mappings are registered.</p>
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            .favorParameter(false)
            .ignoreAcceptHeader(false)
            .defaultContentType(MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON);
    }

    /**
     * JAXB2 Marshaller for ESPI XML processing.
     */
    @Bean
    public Jaxb2Marshaller jaxb2Marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();

        // Set context path for ESPI domain objects
        marshaller.setPackagesToScan(
            "org.greenbuttonalliance.espi.common.domain",
            "org.greenbuttonalliance.espi.common.dto"
        );

        // Configure marshaller properties including the NamespacePrefixMapper
        marshaller.setMarshallerProperties(java.util.Map.of(
            Marshaller.JAXB_FORMATTED_OUTPUT, true,
            Marshaller.JAXB_ENCODING, "UTF-8",
            "org.glassfish.jaxb.namespacePrefixMapper", new org.greenbuttonalliance.espi.common.utils.EspiNamespacePrefixMapper()
        ));

        return marshaller;
    }

    /**
     * Create XML message converter with JAXB marshalling.
     */
    private HttpMessageConverter<?> createXmlConverter() {
        MarshallingHttpMessageConverter xmlConverter = new MarshallingHttpMessageConverter();
        xmlConverter.setMarshaller(jaxb2Marshaller());
        xmlConverter.setUnmarshaller(jaxb2Marshaller());
        
        // The converter can write all three types, but the ESPI resource endpoints declare
        // produces = application/atom+xml, so only Atom is negotiated for them (#312)
        xmlConverter.setSupportedMediaTypes(Arrays.asList(
            MediaType.APPLICATION_XML,
            MediaType.APPLICATION_ATOM_XML,
            MediaType.TEXT_XML
        ));
        
        return xmlConverter;
    }

    /**
     * Create JSON message converter with proper date handling.
     */
    private HttpMessageConverter<?> createJsonConverter3() {

        return new JacksonJsonHttpMessageConverter(JsonMapper.builder()
                        .enable(SerializationFeature.INDENT_OUTPUT)
                //.configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, true)
                     //   .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .build());
                // Java 8 time should be included by default
    }

    /**
     * WebClient for external HTTP communication.
     */
//    @Bean
//    public WebClient webClient() {
//        return WebClient.builder()
//            .codecs(configurer -> configurer
//                .defaultCodecs()
//                .maxInMemorySize(1024 * 1024) // 1MB buffer
//            )
//            .build();
//    }

    /**
     * Configure static resource handling.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/")
            .setCachePeriod(3600);

        // @EnableWebMvc disables Spring Boot's default static-resource mappings, so the portal's
        // CSS/JS/images must be registered explicitly; otherwise the templates' /css and /js links
        // 404 and the portal renders unstyled (#173). These paths are also security-ignored via the
        // WebSecurityCustomizer in SecurityConfiguration.
        // classpath:/META-INF/resources/css/ serves the shared Green Button Alliance theme
        // (gba-theme.css) from the OpenESPI-Common jar.
        registry.addResourceHandler("/css/**")
            .addResourceLocations("classpath:/static/css/", "classpath:/META-INF/resources/css/")
            .setCachePeriod(3600);

        registry.addResourceHandler("/js/**")
            .addResourceLocations("classpath:/static/js/")
            .setCachePeriod(3600);

        // classpath:/META-INF/resources/images/ serves the shared GBA logo and favicon from the
        // OpenESPI-Common jar.
        registry.addResourceHandler("/images/**")
            .addResourceLocations("classpath:/static/images/", "classpath:/META-INF/resources/images/")
            .setCachePeriod(86400);
    }

    /**
     * Configure CORS mappings.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/espi/**")
            .allowedOriginPatterns("http://localhost:*", "https://*.greenbuttonalliance.org")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }

    /**
     * JAXBContext for manual ESPI XML processing.
     */
//    @Bean
//    public JAXBContext espiJaxbContext() throws JAXBException {
//        return JAXBContext.newInstance(
//            "org.greenbuttonalliance.espi.common.domain:" +
//            "org.greenbuttonalliance.espi.common.models.atom"
//        );
//    }

    /**
     * JAXB Marshaller for ESPI XML export.
     */
    @Bean
    public Marshaller espiMarshaller(Jaxb2Marshaller jaxb2Marshaller) throws JAXBException {
        Marshaller marshaller = jaxb2Marshaller.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, xmlPrettyPrint);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        
        if (includeNamespaces) {
            marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, 
                "http://naesb.org/espi espi.xsd");
        }
        
        return marshaller;
    }

    /**
     * JAXB Unmarshaller for ESPI XML import.
     */
    @Bean
    public Unmarshaller espiUnmarshaller(Jaxb2Marshaller jaxb2Marshaller) throws JAXBException {
        return jaxb2Marshaller.createUnmarshaller();
    }

}