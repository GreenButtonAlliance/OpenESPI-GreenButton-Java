package org.greenbuttonalliance.espi.thirdparty.subscription;

import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionFeedValidatorTest {

	private static String fixture(String name) throws IOException {
		try (var in = SubscriptionFeedValidatorTest.class.getResourceAsStream("/subscription/" + name)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static byte[] bytes(String name) throws IOException {
		return fixture(name).getBytes(StandardCharsets.UTF_8);
	}

	@Test
	void offSkipsValidation() {
		FeedValidation result = new SubscriptionFeedValidator(XmlValidationMode.OFF).validate("<not xml".getBytes(StandardCharsets.UTF_8));

		assertThat(result)
				.returns(null, FeedValidation::valid)
				.returns(0, FeedValidation::errorCount)
				.matches(r -> r.issues().isEmpty(), "lists no issues")
				.returns(false, FeedValidation::blocksDisplay);
	}

	@Test
	void reportListsIssuesWithoutBlocking() throws IOException {
		FeedValidation result = new SubscriptionFeedValidator(XmlValidationMode.REPORT).validate(bytes("feed-with-278-errors.xml"));

		assertThat(result)
				.returns(false, FeedValidation::valid)
				.matches(r -> r.errorCount() > 0, "has errors")
				.matches(r -> !r.issues().isEmpty(), "lists issues")
				.returns(false, FeedValidation::blocksDisplay);
	}

	@Test
	void enforceBlocksInvalidFeed() throws IOException {
		FeedValidation result = new SubscriptionFeedValidator(XmlValidationMode.ENFORCE).validate(bytes("feed-with-278-errors.xml"));

		assertThat(result)
				.returns(false, FeedValidation::valid)
				.returns(true, FeedValidation::blocksDisplay);
	}

	@Test
	void enforceAllowsValidFeed() throws IOException {
		FeedValidation result = new SubscriptionFeedValidator(XmlValidationMode.ENFORCE).validate(bytes("valid-feed.xml"));

		assertThat(result)
				.returns(true, FeedValidation::valid)
				.returns(0, FeedValidation::errorCount)
				.returns(false, FeedValidation::blocksDisplay);
	}

	@Test
	void revalidateUsesTheSavedModeNotTheConfiguredOne() throws IOException {
		FeedValidation result = new SubscriptionFeedValidator(XmlValidationMode.OFF)
				.revalidate(fixture("feed-with-278-errors.xml"), XmlValidationMode.ENFORCE);

		assertThat(result)
				.returns(XmlValidationMode.ENFORCE, FeedValidation::mode)
				.returns(true, FeedValidation::blocksDisplay);
	}

	@Test
	void lowercaseConfigValuesBindToTheEnum() {
		try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(SubscriptionFeedValidator.class)
				.web(WebApplicationType.NONE).bannerMode(Banner.Mode.OFF).run("--espi.thirdparty.xml-validation=enforce")) {
			assertThat(ctx.getBean(SubscriptionFeedValidator.class).mode()).isEqualTo(XmlValidationMode.ENFORCE);
		}
	}
}
