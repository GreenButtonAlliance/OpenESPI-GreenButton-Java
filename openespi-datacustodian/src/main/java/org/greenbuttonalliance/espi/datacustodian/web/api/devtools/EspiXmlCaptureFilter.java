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
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Dev-only diagnostic: tees ESPI resource XML responses to files on disk so a developer can view
 * (and diff) the actual marshalled Atom XML before committing changes (#119).
 *
 * <p>Active only under the development profiles ({@code dev-mysql}, {@code dev-postgresql},
 * {@code local}, {@code dev}) — never in {@code test}/{@code prod}/{@code docker}, so it has zero
 * impact on CI or production. It is controller-agnostic: it captures every XML response under
 * {@code /espi/1_1/resource/**}, so it works for the existing controllers and every resource added
 * to the canonical surface.</p>
 *
 * <p>Layout — a persistent, git-ignored, repo-root directory ({@code espi.dev.xml-capture.dir},
 * default {@code dev-xml-capture}) with one timestamped sub-directory <b>per application run</b>:</p>
 * <pre>
 *   dev-xml-capture/
 *     run-20260605-231007-413/
 *       GET_UsagePoint_503888.xml
 *       GET_Customer.xml
 *     run-20260605-233512-088/
 *       GET_UsagePoint_503888.xml
 * </pre>
 * <p>Per-run sub-dirs (rather than a flat overwrite) let you {@code diff -r} the output of one run
 * against another to see the effect of a change. Within a run, filenames are stable per
 * method+path (a repeated call overwrites with the latest response). The response body is always
 * copied through to the client unchanged.</p>
 */
@Component
@Profile({"dev-mysql", "dev-postgresql", "local", "dev"})
public class EspiXmlCaptureFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(EspiXmlCaptureFilter.class);
    private static final String RESOURCE_PATH = "/espi/1_1/resource";
    private static final DateTimeFormatter RUN_ID =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final boolean enabled;
    private final Path runDir;

    public EspiXmlCaptureFilter(
            @Value("${espi.dev.xml-capture.enabled:true}") boolean enabled,
            @Value("${espi.dev.xml-capture.dir:dev-xml-capture}") String captureDir) {
        this.enabled = enabled;
        // One sub-directory per application run, so successive runs can be compared rather than
        // overwriting each other.
        this.runDir = Path.of(captureDir).resolve("run-" + LocalDateTime.now().format(RUN_ID));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !request.getRequestURI().contains(RESOURCE_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, wrapper);
        } finally {
            capture(request, wrapper);
            wrapper.copyBodyToResponse();
        }
    }

    private void capture(HttpServletRequest request, ContentCachingResponseWrapper wrapper) {
        byte[] body = wrapper.getContentAsByteArray();
        String contentType = wrapper.getContentType();
        if (body.length == 0 || contentType == null
                || !contentType.toLowerCase().contains("xml")) {
            return;
        }
        try {
            Files.createDirectories(runDir);
            Path file = runDir.resolve(fileName(request));
            Files.write(file, body);
            log.info("[dev] captured ESPI XML ({} bytes) -> {}", body.length, file.toAbsolutePath());
        } catch (IOException e) {
            // dev aid only — never let a capture failure affect the response
            log.warn("[dev] ESPI XML capture failed: {}", e.getMessage());
        }
    }

    /** Stable per-run filename from the request, e.g. {@code GET_UsagePoint_503888.xml}. */
    private String fileName(HttpServletRequest request) {
        String path = request.getRequestURI();
        int idx = path.indexOf(RESOURCE_PATH);
        String tail = idx >= 0 ? path.substring(idx + RESOURCE_PATH.length()) : path;
        String safe = tail.replaceAll("^/+", "").replaceAll("[^A-Za-z0-9._-]+", "_");
        if (safe.isBlank()) {
            safe = "root";
        }
        return request.getMethod() + "_" + safe + ".xml";
    }
}
