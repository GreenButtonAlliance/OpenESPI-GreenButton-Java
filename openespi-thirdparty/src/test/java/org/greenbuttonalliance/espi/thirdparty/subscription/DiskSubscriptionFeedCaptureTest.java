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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class DiskSubscriptionFeedCaptureTest {

	@TempDir
	Path tempDir;

	private List<Path> captured() throws IOException {
		try (Stream<Path> files = Files.walk(tempDir)) {
			return files.filter(Files::isRegularFile).sorted().toList();
		}
	}

	@Test
	void writesFeedFile() throws IOException {
		byte[] body = "<feed>é</feed>".getBytes(StandardCharsets.UTF_8);

		new DiskSubscriptionFeedCapture(true, tempDir.toString()).capture("sub-1", 200, body);

		List<Path> files = captured();
		byte[] written = Files.readAllBytes(files.get(0));
		assertThat(files).singleElement()
				.satisfies(f -> assertThat(f.getFileName().toString()).isEqualTo("GET_Batch_Subscription_sub-1_1.xml"))
				.extracting(f -> f.getParent().getFileName().toString()).asString()
				.matches("thirdparty-run-\\d{8}-\\d{6}");
		assertThat(written).isEqualTo(body);
	}

	@Test
	void writesErrorFileForNon200() throws IOException {
		new DiskSubscriptionFeedCapture(true, tempDir.toString())
				.capture("sub-2", 403, "forbidden body".getBytes(StandardCharsets.UTF_8));

		List<Path> files = captured();
		String written = Files.readString(files.get(0));
		assertThat(files).extracting(f -> f.getFileName().toString())
				.containsExactly("GET_Batch_Subscription_sub-2_1.error.txt");
		assertThat(written).contains("HTTP 403").contains("forbidden body");
	}

	@Test
	void numbersSuccessiveCaptures() throws IOException {
		DiskSubscriptionFeedCapture capture = new DiskSubscriptionFeedCapture(true, tempDir.toString());
		capture.capture("sub-3", 200, "<a/>".getBytes(StandardCharsets.UTF_8));
		capture.capture("sub-3", 200, "<b/>".getBytes(StandardCharsets.UTF_8));

		assertThat(captured()).extracting(f -> f.getFileName().toString())
				.containsExactly("GET_Batch_Subscription_sub-3_1.xml", "GET_Batch_Subscription_sub-3_2.xml");
	}

	@Test
	void disabledWritesNothing() throws IOException {
		new DiskSubscriptionFeedCapture(false, tempDir.toString())
				.capture("sub-4", 200, "<a/>".getBytes(StandardCharsets.UTF_8));

		assertThat(captured()).isEmpty();
	}

	@Test
	void invalidDirDisablesCaptureWithoutFailing() throws IOException {
		DiskSubscriptionFeedCapture capture = new DiskSubscriptionFeedCapture(true, "bad\0dir");

		assertThatCode(() -> capture.capture("sub-5", 200, "<a/>".getBytes(StandardCharsets.UTF_8)))
				.doesNotThrowAnyException();
		assertThat(captured()).isEmpty();
	}

	@Test
	void disabledIgnoresAnInvalidDir() {
		assertThatCode(() -> new DiskSubscriptionFeedCapture(false, "bad\0dir")).doesNotThrowAnyException();
	}

	@Test
	void relativeDirResolvesUnderNearestGitAncestor() throws IOException {
		Files.createDirectories(tempDir.resolve("repo/.git"));
		Path nested = Files.createDirectories(tempDir.resolve("repo/module/deep"));

		assertThat(DiskSubscriptionFeedCapture.resolveCaptureDir("dev-xml-capture", nested))
				.isEqualTo(tempDir.resolve("repo/dev-xml-capture"));
	}

	@Test
	void absoluteDirIsReturnedUnchanged() throws IOException {
		Path nested = Files.createDirectories(tempDir.resolve("repo/.git/x"));
		Path absolute = tempDir.resolve("elsewhere").toAbsolutePath();

		assertThat(DiskSubscriptionFeedCapture.resolveCaptureDir(absolute.toString(), nested)).isEqualTo(absolute);
	}

	@Test
	void withoutGitAncestorFallsBackToStartDir() throws IOException {
		Path start = Files.createDirectories(tempDir.resolve("nogit/sub"));
		// A stray .git above the temp dir would legitimately win, so only assert when none exists.
		boolean gitAbove = false;
		for (Path p = tempDir; p != null; p = p.getParent()) {
			gitAbove |= Files.exists(p.resolve(".git"));
		}
		org.junit.jupiter.api.Assumptions.assumeFalse(gitAbove);

		assertThat(DiskSubscriptionFeedCapture.resolveCaptureDir("out", start)).isEqualTo(start.resolve("out"));
	}

	@Test
	void ioFailureIsLoggedAndNotThrown() throws IOException {
		Path occupied = Files.writeString(tempDir.resolve("occupied"), "a regular file");
		Logger logger = (Logger) LoggerFactory.getLogger(DiskSubscriptionFeedCapture.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		try {
			DiskSubscriptionFeedCapture capture = new DiskSubscriptionFeedCapture(true, occupied.toString());

			assertThatCode(() -> capture.capture("sub-6", 200, "<a/>".getBytes(StandardCharsets.UTF_8)))
					.doesNotThrowAnyException();
			assertThat(appender.list).singleElement()
					.satisfies(e -> assertThat(e.getLevel()).isEqualTo(Level.WARN));
		} finally {
			logger.detachAppender(appender);
		}
	}

	@Nested
	@SpringBootTest
	@ActiveProfiles("test")
	class NotActiveOnTestProfile {

		@Autowired
		private ObjectProvider<SubscriptionFeedCapture> capture;

		@Test
		void notActiveOnTestProfile() {
			assertThat(capture.getIfAvailable()).isNull();
		}
	}
}
