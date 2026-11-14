/*
 *
 *        Copyright (c) 2025-2026 Green Button Alliance, Inc.
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    /** All files with the given suffix anywhere under {@code base}. */
    private static List<Path> files(Path base, String suffix) throws Exception {
        if (!Files.exists(base)) {
            return List.of();
        }
        try (var paths = Files.walk(base)) {
            return paths.filter(p -> p.toString().endsWith(suffix)).toList();
        }
    }

    @Test
    @DisplayName("#247: an exception writes an .error.txt with the exception chain, then is rethrown")
    void capturesExceptionChain(@TempDir Path dir) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/espi/1_1/resource/UsagePoint");
        request.setQueryString("limit=5");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RuntimeException failure = new RuntimeException("Failed to export DTO",
                new IllegalStateException("class java.util.ImmutableCollections$ListN nor any of its super class is known"));

        assertThatThrownBy(() -> new EspiXmlCaptureFilter(true, dir.toString())
                .doFilter(request, response, (req, res) -> { throw failure; }))
                .isSameAs(failure);

        List<Path> errors = files(dir, ".error.txt");
        assertThat(errors).singleElement()
                .satisfies(p -> assertThat(p.getFileName().toString()).isEqualTo("GET_UsagePoint.error.txt"))
                .satisfies(p -> assertThat(Files.readString(p))
                        .contains("Request: GET /espi/1_1/resource/UsagePoint?limit=5", "unhandled exception",
                                "java.lang.RuntimeException: Failed to export DTO",
                                "java.lang.IllegalStateException: class java.util.ImmutableCollections$ListN"));
        assertThat(xmlFiles(dir)).isEmpty();
    }

    @Test
    @DisplayName("#247: a 4xx/5xx response writes an .error.txt with the status and body, not an .xml")
    void captures4xxResponse(@TempDir Path dir) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/espi/1_1/resource/Authorization/42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new EspiXmlCaptureFilter(true, dir.toString()).doFilter(request, response, (req, res) -> {
            ((jakarta.servlet.http.HttpServletResponse) res).setStatus(404);
            res.setContentType("application/xml");
            res.getOutputStream().write("<error>not found</error>".getBytes());
        });

        assertThat(xmlFiles(dir)).isEmpty();
        assertThat(files(dir, ".error.txt")).singleElement()
                .satisfies(p -> assertThat(Files.readString(p))
                        .contains("Status:  404", "<error>not found</error>"));
        assertThat(response.getContentAsString()).isEqualTo("<error>not found</error>");
    }

    @Test
    @DisplayName("a path-traversal request path cannot write outside the run directory")
    void traversalStaysInsideRunDir(@TempDir Path dir) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/espi/1_1/resource/../../../../outside/evil");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new EspiXmlCaptureFilter(true, dir.resolve("capture").toString())
                .doFilter(request, response, chain("application/atom+xml", XML));

        try (var paths = Files.walk(dir)) {
            assertThat(paths.filter(Files::isRegularFile).toList())
                    .allSatisfy(p -> assertThat(p.getParent().getParent()).isEqualTo(dir.resolve("capture")));
        }
        assertThat(dir.resolve("outside")).doesNotExist();
    }

    @Test
    @DisplayName("a relative capture dir is anchored at the repository root")
    void relativeDirResolvesToRepoRoot() {
        Path resolved = EspiXmlCaptureFilter.resolveCaptureDir("dev-xml-capture");

        // endsWithRaw: plain endsWith canonicalises the path and fails when the directory does not
        // exist yet, as on a fresh CI checkout
        assertThat(resolved).isAbsolute().endsWithRaw(Path.of("dev-xml-capture"));
        assertThat(resolved.getParent().resolve(".git")).exists();
    }
}
