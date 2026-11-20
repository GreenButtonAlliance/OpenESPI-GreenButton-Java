/*
 * Copyright (c) 2026 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.greenbuttonalliance.espi.thirdparty.subscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dev-only: writes each fetched {@code Batch/Subscription} response to disk so a developer can open
 * the Atom XML. Active only under the development profiles, never in {@code test}, {@code prod} or
 * {@code docker}.
 *
 * <p>Layout: {@code <dir>/thirdparty-run-<yyyyMMdd-HHmmss>/GET_Batch_Subscription_<id>_<n>.xml}, one
 * run folder per application run and {@code n} counting from 1 within the run. A status other than
 * 200 writes {@code ..._<n>.error.txt} holding {@code HTTP <status>} and the body. A relative
 * {@code espi.dev.xml-capture.dir} resolves against the repository root (the nearest ancestor
 * containing {@code .git}). I/O failures are logged and never thrown.</p>
 */
@Component
@Profile({"dev-mysql", "dev-postgresql", "local", "dev"})
public class DiskSubscriptionFeedCapture implements SubscriptionFeedCapture {

	private static final Logger log = LoggerFactory.getLogger(DiskSubscriptionFeedCapture.class);
	private static final DateTimeFormatter RUN_ID = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	private final Path runDir;
	private final AtomicInteger counter = new AtomicInteger();

	public DiskSubscriptionFeedCapture(
			@Value("${espi.dev.xml-capture.enabled:true}") boolean enabled,
			@Value("${espi.dev.xml-capture.dir:dev-xml-capture}") String dir) {
		this.runDir = enabled ? resolveRunDir(dir) : null;
	}

	/** The run folder, or null (capture off) when the configured dir is not a valid path. */
	private static Path resolveRunDir(String dir) {
		try {
			return resolveCaptureDir(dir, Path.of("").toAbsolutePath())
					.resolve("thirdparty-run-" + LocalDateTime.now().format(RUN_ID));
		} catch (InvalidPathException e) {
			log.warn("[dev] Subscription feed capture is off: invalid espi.dev.xml-capture.dir: {}", e.getMessage());
			return null;
		}
	}

	/** A relative capture dir is anchored at the nearest ancestor of {@code start} holding .git, else at start. */
	static Path resolveCaptureDir(String captureDir, Path start) {
		Path dir = Path.of(captureDir);
		if (dir.isAbsolute()) {
			return dir;
		}
		for (Path p = start; p != null; p = p.getParent()) {
			if (Files.exists(p.resolve(".git"))) {
				return p.resolve(dir);
			}
		}
		return start.resolve(dir);
	}

	@Override
	public void capture(String subscriptionId, int httpStatus, byte[] body) {
		if (runDir == null) {
			return;
		}
		byte[] content = body == null ? new byte[0] : body;
		int n = counter.incrementAndGet();
		// The id comes from a URL or form; keep it to safe file-name characters.
		String safeId = String.valueOf(subscriptionId).replaceAll("[^A-Za-z0-9._-]+", "_");
		String base = "GET_Batch_Subscription_" + safeId + "_" + n;
		try {
			Files.createDirectories(runDir);
			Path file;
			byte[] out;
			if (httpStatus == 200) {
				file = runDir.resolve(base + ".xml");
				out = content;
			} else {
				file = runDir.resolve(base + ".error.txt");
				out = ("HTTP " + httpStatus + "\n\n" + new String(content, StandardCharsets.UTF_8) + "\n")
						.getBytes(StandardCharsets.UTF_8);
			}
			Files.write(file, out);
			log.info("[dev] captured Subscription feed (HTTP {}, {} bytes) -> {}", httpStatus, content.length,
					file.toAbsolutePath());
		} catch (IOException e) {
			// dev aid only: never let a capture failure affect the fetch
			log.warn("[dev] Subscription feed capture failed: {}", e.getMessage());
		}
	}
}
