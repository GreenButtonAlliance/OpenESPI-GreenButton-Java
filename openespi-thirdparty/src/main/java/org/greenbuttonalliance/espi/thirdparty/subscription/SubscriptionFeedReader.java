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
			if (href != null) {
				index.put(href, entry);
			}
		}

		Map<String, UsagePointNode> usagePoints = new LinkedHashMap<>();
		Map<String, MeterReadingNode> meterReadings = new LinkedHashMap<>();

		for (AtomEntryDto entry : entries) {
			if (entry.getResource() instanceof UsagePointDto resource) {
				Entry<UsagePointDto> usagePoint = wrap(entry, resource);
				Entry<TimeConfigurationDto> localTime = related(entry, "LocalTimeParameters", index,
						TimeConfigurationDto.class);
				usagePoints.put(usagePoint.selfHref(), new UsagePointNode(usagePoint, localTime, new ArrayList<>(),
						new ArrayList<>(), new ArrayList<>()));
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
				meterReadings.put(meterReading.selfHref(), node);
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
		meterReadings.values().forEach(n -> n.intervalBlocks().sort(byStart));

		return new SubscriptionTree(feed.getId(), feed.getTitle(), feed.getUpdated(), entries.size(),
				List.copyOf(usagePoints.values()), List.copyOf(problems));
	}

	private static AtomFeedDto unmarshal(byte[] xml) {
		XMLStreamReader reader = null;
		try {
			XMLInputFactory factory = XMLInputFactory.newFactory();
			factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
			factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
			reader = factory.createXMLStreamReader(new ByteArrayInputStream(xml));
			if (CONTEXT.createUnmarshaller().unmarshal(reader) instanceof AtomFeedDto feed) {
				return feed;
			}
		} catch (JAXBException | XMLStreamException e) {
			throw new SubscriptionFeedReadException("The fetched bytes are not an ESPI Atom feed", e);
		} finally {
			closeQuietly(reader);
		}
		throw new SubscriptionFeedReadException("The fetched document is not an Atom feed", null);
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
