package org.energyos.espi.thirdparty.domain;

import org.energyos.espi.thirdparty.models.atom.adapters.IntervalBlockAdapter;
import org.energyos.espi.thirdparty.utils.EspiMarshaller;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;

import static org.junit.Assert.assertEquals;

public class IntervalBlockTests {

    static final String XML_INPUT =
            "<IntervalBlock xmlns=\"http://naesb.org/espi\">" +
                "<interval>" +
                    "<duration>86400</duration>" +
                    "<start>1330578000</start>" +
                "</interval>" +
                "<IntervalReading></IntervalReading>" +
                "<IntervalReading></IntervalReading>" +
            "</IntervalBlock>";

    private IntervalBlock intervalBlock;

    @Before
    public void before() throws Exception {
        IntervalBlockAdapter intervalBlockAdapter = new IntervalBlockAdapter();
        JAXBElement<IntervalBlock> intervalBlockJAXBElement = EspiMarshaller.unmarshal(XML_INPUT);
        intervalBlock = intervalBlockAdapter.unmarshal(intervalBlockJAXBElement);
    }

    @Test
    public void unmarshallsIntervalBlock() {
        assertEquals(IntervalBlock.class, intervalBlock.getClass());
    }

    @Test
    public void unmarshal_setsIntervalDuration() {
        assertEquals(86400L,intervalBlock.getInterval().getDuration().longValue());
    }

    @Test
    public void unmarshal_setsIntervalStart() {
        assertEquals(1330578000L, intervalBlock.getInterval().getStart().longValue());
    }

    @Test
    public void unmarshal_setsIntervalReadings() {
        assertEquals(intervalBlock, intervalBlock.getIntervalReadings().get(0).getIntervalBlock());
        assertEquals(intervalBlock, intervalBlock.getIntervalReadings().get(1).getIntervalBlock());
    }
}
