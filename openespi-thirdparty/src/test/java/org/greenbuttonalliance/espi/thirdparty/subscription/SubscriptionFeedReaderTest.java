package org.greenbuttonalliance.espi.thirdparty.subscription;

import org.greenbuttonalliance.espi.thirdparty.subscription.SubscriptionTree.MeterReadingNode;
import org.greenbuttonalliance.espi.thirdparty.subscription.SubscriptionTree.UsagePointNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionFeedReaderTest {

	private final SubscriptionFeedReader reader = new SubscriptionFeedReader();

	private static String fixture(String name) throws IOException {
		try (var in = SubscriptionFeedReaderTest.class.getResourceAsStream("/subscription/" + name)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/** The fixture without the one entry whose text contains {@code marker}. */
	private static byte[] withoutEntryContaining(String marker) throws IOException {
		String entry = "<atom:entry>((?!</atom:entry>).)*" + Pattern.quote(marker) + "((?!</atom:entry>).)*</atom:entry>";
		String xml = Pattern.compile(entry, Pattern.DOTALL).matcher(fixture("valid-feed.xml")).replaceFirst("");
		return xml.getBytes(StandardCharsets.UTF_8);
	}

	@Test
	void readsSampleSubscription() throws IOException {
		SubscriptionTree tree = reader.read(fixture("valid-feed.xml").getBytes(StandardCharsets.UTF_8));

		assertThat(tree)
				.returns(8, SubscriptionTree::entryCount)
				.returns("Green Button Subscription Feed", SubscriptionTree::title)
				.satisfies(t -> assertThat(t.linkingProblems()).isEmpty())
				.extracting(SubscriptionTree::usagePoints, org.assertj.core.api.InstanceOfAssertFactories.LIST)
				.hasSize(1);

		UsagePointNode usagePoint = tree.usagePoints().get(0);
		assertThat(usagePoint)
				.satisfies(n -> assertThat(n.localTimeParameters()).isNotNull())
				.satisfies(n -> assertThat(n.usageSummaries()).hasSize(1))
				.satisfies(n -> assertThat(n.powerQualitySummaries()).hasSize(1))
				.satisfies(n -> assertThat(n.meterReadings()).hasSize(1));

		MeterReadingNode meterReading = usagePoint.meterReadings().get(0);
		assertThat(meterReading)
				.satisfies(n -> assertThat(n.readingType()).isNotNull())
				.satisfies(n -> assertThat(n.intervalBlocks())
						.hasSize(2)
						.allSatisfy(b -> assertThat(b.resource().getIntervalReadings()).hasSize(24))
						.extracting(b -> b.resource().getInterval().getStart())
						.isSorted());
	}

	@Test
	void childWithMissingParentIsReported() throws IOException {
		SubscriptionTree tree = reader.read(withoutEntryContaining("<espi:MeterReading/>"));

		assertThat(tree)
				.satisfies(t -> assertThat(t.usagePoints()).singleElement()
						.satisfies(n -> assertThat(n.meterReadings()).isEmpty()))
				.satisfies(t -> assertThat(t.linkingProblems())
						.hasSize(2)
						.allSatisfy(p -> assertThat(p)
								.startsWith("IntervalBlock ")
								.endsWith(": parent MeterReading not in the feed")));
	}

	@Test
	void meterReadingWithoutReadingType() throws IOException {
		SubscriptionTree tree = reader.read(withoutEntryContaining("<espi:ReadingType>"));

		assertThat(tree)
				.satisfies(t -> assertThat(t.linkingProblems()).isEmpty())
				.satisfies(t -> assertThat(t.usagePoints()).singleElement()
						.satisfies(n -> assertThat(n.meterReadings()).singleElement()
								.satisfies(m -> assertThat(m.readingType()).isNull())
								.satisfies(m -> assertThat(m.intervalBlocks()).hasSize(2))));
	}

	@Test
	void emptyFeedHasNoUsagePoints() throws IOException {
		SubscriptionTree tree = reader.read(fixture("empty-feed.xml").getBytes(StandardCharsets.UTF_8));

		assertThat(tree)
				.returns(0, SubscriptionTree::entryCount)
				.matches(t -> t.usagePoints().isEmpty(), "has no usage points")
				.matches(t -> t.linkingProblems().isEmpty(), "has no linking problems");
	}

	@Test
	void malformedXmlThrows() {
		assertThatThrownBy(() -> reader.read("<html>oops</html>".getBytes(StandardCharsets.UTF_8)))
				.isInstanceOf(SubscriptionFeedReadException.class);
	}
}
