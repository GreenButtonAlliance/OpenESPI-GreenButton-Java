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
 *
 * <p>Failures are captured too (#247): a response with status 400 or above, or a request that ends
 * in an exception, writes {@code <METHOD>_<path>.error.txt} next to the XML, with the status, the
 * request, the full exception chain (for example a JAXB marshalling error) and the response body if
 * any. The exception is still rethrown, so error handling is unchanged.</p>
 *
 * <p>A relative {@code espi.dev.xml-capture.dir} is resolved against the repository root (the
 * nearest directory containing {@code .git}), so captures land in the same place whether the
 * application is started from the repository root or from the module directory.</p>
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
        this.runDir = resolveCaptureDir(captureDir).resolve("run-" + LocalDateTime.now().format(RUN_ID));
    }

    /** A relative capture dir is anchored at the repository root, else at the working directory. */
    static Path resolveCaptureDir(String captureDir) {
        Path dir = Path.of(captureDir);
        if (dir.isAbsolute()) {
            return dir;
        }
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.exists(p.resolve(".git"))) {
                return p.resolve(dir);
            }
        }
        return dir.toAbsolutePath();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !request.getRequestURI().contains(RESOURCE_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        Throwable failure = null;
        try {
            filterChain.doFilter(request, wrapper);
        } catch (ServletException | IOException | RuntimeException | Error e) {
            failure = e;
            throw e;
        } finally {
            capture(request, wrapper);
            captureFailure(request, wrapper, failure);
            wrapper.copyBodyToResponse();
        }
    }

    private void capture(HttpServletRequest request, ContentCachingResponseWrapper wrapper) {
        byte[] body = wrapper.getContentAsByteArray();
        String contentType = wrapper.getContentType();
        if (wrapper.getStatus() >= 400 || body.length == 0 || contentType == null
                || !contentType.toLowerCase().contains("xml")) {
            return;
        }
        write(fileName(request, ".xml"), body, "captured ESPI XML");
    }

    /** Writes {@code <METHOD>_<path>.error.txt} for a 4xx/5xx response or an exception (#247). */
    private void captureFailure(HttpServletRequest request, ContentCachingResponseWrapper wrapper,
                                Throwable failure) {
        if (failure == null && wrapper.getStatus() < 400) {
            return;
        }
        StringBuilder report = new StringBuilder()
                .append("Time:    ").append(LocalDateTime.now()).append('\n')
                .append("Request: ").append(request.getMethod()).append(' ').append(request.getRequestURI())
                .append(request.getQueryString() != null ? "?" + request.getQueryString() : "").append('\n')
                .append("Status:  ").append(failure != null ? "unhandled exception (the container answers 500)"
                        : String.valueOf(wrapper.getStatus())).append('\n');
        if (failure != null) {
            report.append("\nException chain:\n");
            for (Throwable t = failure; t != null; t = t.getCause() == t ? null : t.getCause()) {
                report.append("  ").append(t.getClass().getName()).append(": ").append(t.getMessage()).append('\n');
            }
        }
        byte[] body = wrapper.getContentAsByteArray();
        if (body.length > 0) {
            report.append("\nResponse body (").append(wrapper.getContentType()).append("):\n")
                    .append(new String(body, java.nio.charset.StandardCharsets.UTF_8)).append('\n');
        }
        write(fileName(request, ".error.txt"),
                report.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), "captured ESPI failure");
    }

    private void write(String fileName, byte[] content, String what) {
        try {
            Files.createDirectories(runDir);
            Path file = runDir.resolve(fileName);
            Files.write(file, content);
            log.info("[dev] {} ({} bytes) -> {}", what, content.length, file.toAbsolutePath());
        } catch (IOException e) {
            // dev aid only — never let a capture failure affect the response
            log.warn("[dev] ESPI capture failed: {}", e.getMessage());
        }
    }

    /** Stable per-run filename from the request, e.g. {@code GET_UsagePoint_503888.xml}. */
    private String fileName(HttpServletRequest request, String suffix) {
        String path = request.getRequestURI();
        int idx = path.indexOf(RESOURCE_PATH);
        String tail = idx >= 0 ? path.substring(idx + RESOURCE_PATH.length()) : path;
        String safe = tail.replaceAll("^/+", "").replaceAll("[^A-Za-z0-9._-]+", "_");
        if (safe.isBlank()) {
            safe = "root";
        }
        return request.getMethod() + "_" + safe + suffix;
    }
}
