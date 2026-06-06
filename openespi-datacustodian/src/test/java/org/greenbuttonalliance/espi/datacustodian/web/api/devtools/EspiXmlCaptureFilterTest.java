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

package org.greenbuttonalliance.espi.datacustodian.web.api.devtools;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EspiXmlCaptureFilter} — the dev-only ESPI XML capture aid (#119).
 */
@DisplayName("EspiXmlCaptureFilter (dev XML capture) #119")
class EspiXmlCaptureFilterTest {

    private static final String XML = "<feed xmlns=\"http://www.w3.org/2005/Atom\"><id>urn:uuid:x</id></feed>";

    /** All .xml files anywhere under {@code base} (captures live in a per-run sub-dir). */
    private static List<Path> xmlFiles(Path base) throws Exception {
        if (!Files.exists(base)) {
            return List.of();
        }
        try (var paths = Files.walk(base)) {
            return paths.filter(p -> p.toString().endsWith(".xml")).toList();
        }
    }

    private static FilterChain chain(String contentType, String body) {
        return (req, res) -> {
            res.setContentType(contentType);
            res.getOutputStream().write(body.getBytes());
        };
    }

    @Test
    @DisplayName("captures an XML resource response under a per-run sub-dir and passes the body through")
    void capturesXmlResponse(@TempDir Path dir) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/espi/1_1/resource/UsagePoint/503888");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new EspiXmlCaptureFilter(true, dir.toString())
                .doFilter(request, response, chain("application/atom+xml", XML));

        List<Path> files = xmlFiles(dir);
        assertThat(files).hasSize(1);
        Path captured = files.get(0);
        assertThat(captured.getFileName().toString()).isEqualTo("GET_UsagePoint_503888.xml");
        assertThat(captured.getParent().getFileName().toString()).startsWith("run-");
        assertThat(Files.readString(captured)).isEqualTo(XML);
        // body is still delivered to the client
        assertThat(response.getContentAsString()).isEqualTo(XML);
    }

    @Test
    @DisplayName("does not capture non-XML responses")
    void ignoresNonXml(@TempDir Path dir) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/espi/1_1/resource/UsagePoint");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new EspiXmlCaptureFilter(true, dir.toString())
                .doFilter(request, response, chain("application/json", "{}"));

        assertThat(xmlFiles(dir)).isEmpty();
        assertThat(response.getContentAsString()).isEqualTo("{}");
    }

    @Test
    @DisplayName("does not filter requests outside the resource path")
    void skipsNonResourcePaths(@TempDir Path dir) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new EspiXmlCaptureFilter(true, dir.toString())
                .doFilter(request, response, chain("application/atom+xml", XML));

        assertThat(xmlFiles(dir)).isEmpty();
        assertThat(response.getContentAsString()).isEqualTo(XML);
    }

    @Test
    @DisplayName("disabled flag suppresses capture entirely")
    void disabledSuppressesCapture(@TempDir Path dir) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/espi/1_1/resource/UsagePoint");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new EspiXmlCaptureFilter(false, dir.toString())
                .doFilter(request, response, chain("application/atom+xml", XML));

        assertThat(xmlFiles(dir)).isEmpty();
        assertThat(response.getContentAsString()).isEqualTo(XML);
    }
}
