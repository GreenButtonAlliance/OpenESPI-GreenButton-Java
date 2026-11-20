package org.greenbuttonalliance.espi.thirdparty.subscription;

import org.greenbuttonalliance.espi.thirdparty.subscription.SubscriptionTree.MeterReadingNode;
import org.greenbuttonalliance.espi.thirdparty.subscription.SubscriptionTree.UsagePointNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The reader reads what the Data Custodian sends today: the tests use the unedited DC capture
 * ({@code feed-with-278-errors.xml}), whose SummaryMeasurement children carry no namespace (tracked in #297).
 */
class SubscriptionFeedReaderTest {

	private final SubscriptionFeedReader reader = new SubscriptionFeedReader();

	private static String fixture(String name) throws IOException {
		try (var in = SubscriptionFeedReaderTest.class.getResourceAsStream("/subscription/" + name)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/** The real DC capture without the one entry whose text contains {@code marker}; fails if none was removed. */
	private static byte[] withoutEntryContaining(String marker) throws IOException {
		String entry = "<atom:entry>((?!</atom:entry>).)*" + Pattern.quote(marker) + "((?!</atom:entry>).)*</atom:entry>";
		String original = fixture("feed-with-278-errors.xml");
		Matcher matcher = Pattern.compile(entry, Pattern.DOTALL).matcher(original);
		String xml = matcher.replaceFirst("");
		assertThat(xml).as("entry containing %s removed", marker).hasSizeLessThan(original.length());
		return xml.getBytes(StandardCharsets.UTF_8);
	}

	@Test
	void readsSampleSubscription() throws IOException {
		SubscriptionTree tree = reader.read(fixture("feed-with-278-errors.xml").getBytes(StandardCharsets.UTF_8));

		assertThat(tree)
				.returns(8, SubscriptionTree::entryCount)
				.returns("Green Button Subscription Feed", SubscriptionTree::title)
				.satisfies(t -> assertThat(t.linkingProblems()).isEmpty())
				.satisfies(t -> assertThat(t.usagePoints()).hasSize(1));

		UsagePointNode usagePoint = tree.usagePoints().get(0);
		assertThat(usagePoint)
				.satisfies(n -> assertThat(n.usagePoint())
						.returns("Sample residential electric service", SubscriptionTree.Entry::title)
						.returns("urn:uuid:07171718-b165-522a-b413-11d9eeb1ca43", SubscriptionTree.Entry::id))
				.satisfies(n -> assertThat(n.localTimeParameters()).isNotNull())
				.satisfies(n -> assertThat(n.meterReadings()).hasSize(1))
				// pins today's unqualified SummaryMeasurement output; these values must change with #297, not before
				.satisfies(n -> assertThat(n.usageSummaries()).singleElement()
						.satisfies(s -> assertThat(s.resource().getOverallConsumptionLastPeriod())
								.returns(1325000000L, m -> m.getValue())
								.returns(1769922000L, m -> m.getTimeStamp())))
				.satisfies(n -> assertThat(n.powerQualitySummaries()).singleElement()
						.satisfies(s -> assertThat(s.resource().getMainsVoltage()).isEqualTo(240000L)));

		MeterReadingNode meterReading = usagePoint.meterReadings().get(0);
		assertThat(meterReading)
				.satisfies(n -> assertThat(n.meterReading().title()).isEqualTo("Hourly electricity consumption"))
				.satisfies(n -> assertThat(n.readingType()).isNotNull()
						.satisfies(r -> assertThat(r.resource())
								.returns("-3", rt -> rt.getPowerOfTenMultiplier())
								.returns("72", rt -> rt.getUom())))
				.satisfies(n -> assertThat(n.intervalBlocks())
						.hasSize(2)
						.allSatisfy(b -> assertThat(b.resource().getIntervalReadings()).hasSize(24))
						.extracting(b -> b.resource().getInterval().getStart())
						.isSorted())
				.satisfies(n -> assertThat(n.intervalBlocks().get(0).resource().getIntervalReadings().get(0).getValue())
						.isEqualTo(420000L));
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
	void duplicateSelfHrefKeepsTheFirstAndIsReported() throws IOException {
		String first = "http://localhost:8081/DataCustodian/espi/1_1/resource/IntervalBlock/b3e75607-31de-53a7-9300-42b8be3ddb73";
		String secondHref = "http://localhost:8081/DataCustodian/espi/1_1/resource/IntervalBlock/48b371f7-b1a2-52ef-a86e-0788a3d97328";
		String xml = fixture("feed-with-278-errors.xml").replace(secondHref + "\"", first + "\"");

		SubscriptionTree tree = reader.read(xml.getBytes(StandardCharsets.UTF_8));

		assertThat(tree.linkingProblems())
				.containsExactly("Entry urn:uuid:48b371f7-b1a2-52ef-a86e-0788a3d97328: duplicate self link " + first);
	}

	@Test
	void entryWithoutSelfLinkIsReportedAndStillPlaced() throws IOException {
		String selfLine = "<atom:link rel=\"self\" href=\"http://localhost:8081/DataCustodian/espi/1_1/resource/IntervalBlock/"
				+ "48b371f7-b1a2-52ef-a86e-0788a3d97328\" type=\"espi-entry/IntervalBlock\"/>";
		String original = fixture("feed-with-278-errors.xml");
		assertThat(original).contains(selfLine);

		SubscriptionTree tree = reader.read(original.replace(selfLine, "").getBytes(StandardCharsets.UTF_8));

		assertThat(tree)
				.satisfies(t -> assertThat(t.linkingProblems())
						.containsExactly("Entry urn:uuid:48b371f7-b1a2-52ef-a86e-0788a3d97328: no self link"))
				.satisfies(t -> assertThat(t.usagePoints()).singleElement()
						.satisfies(n -> assertThat(n.meterReadings()).singleElement()
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
