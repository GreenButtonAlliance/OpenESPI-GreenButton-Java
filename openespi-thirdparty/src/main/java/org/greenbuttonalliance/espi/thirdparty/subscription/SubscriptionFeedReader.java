package org.greenbuttonalliance.espi.thirdparty.subscription;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.dto.atom.LinkDto;
import org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.usage.ElectricPowerQualitySummaryDto;
import org.greenbuttonalliance.espi.common.dto.usage.IntervalBlockDto;
import org.greenbuttonalliance.espi.common.dto.usage.MeterReadingDto;
import org.greenbuttonalliance.espi.common.dto.usage.ReadingTypeDto;
import org.greenbuttonalliance.espi.common.dto.usage.TimeConfigurationDto;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.greenbuttonalliance.espi.common.dto.usage.UsageSummaryDto;
import org.greenbuttonalliance.espi.thirdparty.subscription.SubscriptionTree.Entry;
import org.greenbuttonalliance.espi.thirdparty.subscription.SubscriptionTree.MeterReadingNode;
import org.greenbuttonalliance.espi.thirdparty.subscription.SubscriptionTree.UsagePointNode;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reads a {@code Batch/Subscription} feed into a {@link SubscriptionTree}. Entries are indexed by their self href
 * and placed by their {@code rel="related"} links of type {@code espi-entry/...}; {@code espi-feed/...} links are
 * ignored. A child with no resolvable parent is left out and named in {@code linkingProblems}; an unresolved
 * forward link (a ReadingType or LocalTimeParameters not in the feed) is not a problem.
 */
@Component
public class SubscriptionFeedReader {

	private static final JAXBContext CONTEXT = createContext();

	private static JAXBContext createContext() {
		try {
			return JAXBContext.newInstance(AtomFeedDto.class, UsageAtomEntryDto.class);
		} catch (JAXBException e) {
			throw new IllegalStateException("Cannot create the JAXB context for ESPI feeds", e);
		}
	}

	public SubscriptionTree read(byte[] xml) {
		AtomFeedDto feed = unmarshal(xml);
		List<AtomEntryDto> entries = feed.getEntries() == null ? List.of() : feed.getEntries();
		List<String> problems = new ArrayList<>();

		Map<String, AtomEntryDto> index = new LinkedHashMap<>();
		for (AtomEntryDto entry : entries) {
			String href = selfHref(entry);
			if (href == null) {
				problems.add("Entry " + entry.getId() + ": no self link");
			} else if (index.putIfAbsent(href, entry) != null) {
				problems.add("Entry " + entry.getId() + ": duplicate self link " + href);
			}
		}

		// Every UsagePoint is shown; children link only to the first one with a given self href (as the index
		// keeps the first), and one without a self href takes no children. Both are already reported above.
		List<UsagePointNode> shown = new ArrayList<>();
		Map<String, UsagePointNode> usagePoints = new LinkedHashMap<>();
		Map<String, MeterReadingNode> meterReadings = new LinkedHashMap<>();

		for (AtomEntryDto entry : entries) {
			if (entry.getResource() instanceof UsagePointDto resource) {
				Entry<UsagePointDto> usagePoint = wrap(entry, resource);
				Entry<TimeConfigurationDto> localTime = related(entry, "LocalTimeParameters", index,
						TimeConfigurationDto.class);
				UsagePointNode node = new UsagePointNode(usagePoint, localTime, new ArrayList<>(),
						new ArrayList<>(), new ArrayList<>());
				shown.add(node);
				if (usagePoint.selfHref() != null) {
					usagePoints.putIfAbsent(usagePoint.selfHref(), node);
				}
			}
		}
		for (AtomEntryDto entry : entries) {
			if (entry.getResource() instanceof MeterReadingDto resource) {
				UsagePointNode parent = parent(entry, "UsagePoint", usagePoints);
				if (parent == null) {
					problems.add(problem("MeterReading", entry, "UsagePoint"));
					continue;
				}
				Entry<MeterReadingDto> meterReading = wrap(entry, resource);
				MeterReadingNode node = new MeterReadingNode(meterReading,
						related(entry, "ReadingType", index, ReadingTypeDto.class), new ArrayList<>());
				if (meterReading.selfHref() != null) {
					meterReadings.putIfAbsent(meterReading.selfHref(), node);
				}
				parent.meterReadings().add(node);
			}
		}
		for (AtomEntryDto entry : entries) {
			Object resource = entry.getResource();
			if (resource instanceof IntervalBlockDto block) {
				MeterReadingNode parent = parent(entry, "MeterReading", meterReadings);
				if (parent == null) {
					problems.add(problem("IntervalBlock", entry, "MeterReading"));
				} else {
					parent.intervalBlocks().add(wrap(entry, block));
				}
			} else if (resource instanceof UsageSummaryDto summary) {
				UsagePointNode parent = parent(entry, "UsagePoint", usagePoints);
				if (parent == null) {
					problems.add(problem("UsageSummary", entry, "UsagePoint"));
				} else {
					parent.usageSummaries().add(wrap(entry, summary));
				}
			} else if (resource instanceof ElectricPowerQualitySummaryDto summary) {
				UsagePointNode parent = parent(entry, "UsagePoint", usagePoints);
				if (parent == null) {
					problems.add(problem("ElectricPowerQualitySummary", entry, "UsagePoint"));
				} else {
					parent.powerQualitySummaries().add(wrap(entry, summary));
				}
			} else if (!(resource instanceof UsagePointDto || resource instanceof MeterReadingDto
					|| resource instanceof ReadingTypeDto || resource instanceof TimeConfigurationDto)) {
				problems.add("Entry " + entry.getId() + ": unknown resource type");
			}
		}

		Comparator<Entry<IntervalBlockDto>> byStart = Comparator.comparing(
				b -> b.resource().getInterval() == null ? null : b.resource().getInterval().getStart(),
				Comparator.nullsLast(Comparator.naturalOrder()));
		shown.forEach(u -> u.meterReadings().forEach(m -> m.intervalBlocks().sort(byStart)));

		return new SubscriptionTree(feed.getId(), feed.getTitle(), feed.getUpdated(), entries.size(),
				shown.stream().map(SubscriptionFeedReader::frozen).toList(), List.copyOf(problems));
	}

	/** The node with its lists copied: they were filled while linking and are fixed from here on. */
	private static UsagePointNode frozen(UsagePointNode node) {
		return new UsagePointNode(node.usagePoint(), node.localTimeParameters(),
				node.meterReadings().stream()
						.map(m -> new MeterReadingNode(m.meterReading(), m.readingType(), List.copyOf(m.intervalBlocks())))
						.toList(),
				List.copyOf(node.usageSummaries()), List.copyOf(node.powerQualitySummaries()));
	}

	private static AtomFeedDto unmarshal(byte[] xml) {
		XMLStreamReader reader = null;
		try {
			XMLInputFactory factory = XMLInputFactory.newFactory();
			factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
			factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
			reader = factory.createXMLStreamReader(new ByteArrayInputStream(xml));
			Object root = CONTEXT.createUnmarshaller().unmarshal(reader);
			if (root instanceof AtomFeedDto feed) {
				return feed;
			}
			// no exception was thrown, so there is no cause to pass on
			throw new SubscriptionFeedReadException(
					"The fetched document is not an Atom feed (root " + root.getClass().getSimpleName() + ")", null);
		} catch (JAXBException | XMLStreamException e) {
			throw new SubscriptionFeedReadException("The fetched bytes are not an ESPI Atom feed", e);
		} finally {
			closeQuietly(reader);
		}
	}

	private static void closeQuietly(XMLStreamReader reader) {
		if (reader != null) {
			try {
				reader.close();
			} catch (XMLStreamException e) {
				// nothing was read from it after the unmarshal finished
			}
		}
	}

	private static String selfHref(AtomEntryDto entry) {
		LinkDto self = entry.getSelfLink();
		return self == null ? null : self.getHref();
	}

	private static <T> Entry<T> wrap(AtomEntryDto entry, T resource) {
		return new Entry<>(entry.getId(), entry.getTitle(), selfHref(entry), entry.getPublished(), entry.getUpdated(),
				resource);
	}

	/** The hrefs of the entry's {@code rel="related"} links of type {@code espi-entry/<type>}. */
	private static List<String> relatedHrefs(AtomEntryDto entry, String type) {
		List<LinkDto> links = entry.getLinks() == null ? List.of() : entry.getLinks();
		return links.stream()
				.filter(l -> "related".equals(l.getRel()) && ("espi-entry/" + type).equals(l.getType()))
				.map(LinkDto::getHref)
				.toList();
	}

	private static <N> N parent(AtomEntryDto entry, String type, Map<String, N> nodes) {
		return relatedHrefs(entry, type).stream().map(nodes::get).filter(Objects::nonNull).findFirst().orElse(null);
	}

	private static <T> Entry<T> related(AtomEntryDto entry, String type, Map<String, AtomEntryDto> index,
			Class<T> resourceType) {
		return relatedHrefs(entry, type).stream()
				.map(index::get)
				.filter(e -> e != null && resourceType.isInstance(e.getResource()))
				.map(e -> wrap(e, resourceType.cast(e.getResource())))
				.findFirst()
				.orElse(null);
	}

	private static String problem(String type, AtomEntryDto entry, String parentType) {
		return type + " " + selfHref(entry) + ": parent " + parentType + " not in the feed";
	}
}
