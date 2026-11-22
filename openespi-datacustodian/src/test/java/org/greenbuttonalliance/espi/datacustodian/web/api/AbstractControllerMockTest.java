package org.greenbuttonalliance.espi.datacustodian.web.api;

import org.greenbuttonalliance.espi.common.mapper.customer.CustomerAccountMapper;
import org.greenbuttonalliance.espi.common.mapper.customer.CustomerMapper;
import org.greenbuttonalliance.espi.common.mapper.usage.ElectricPowerQualitySummaryMapper;
import org.greenbuttonalliance.espi.common.mapper.usage.IntervalBlockMapper;
import org.greenbuttonalliance.espi.common.mapper.usage.MeterReadingMapper;
import org.greenbuttonalliance.espi.common.mapper.usage.ReadingTypeMapper;
import org.greenbuttonalliance.espi.common.mapper.usage.UsagePointMapper;
import org.greenbuttonalliance.espi.common.mapper.usage.UsageSummaryMapper;
import org.greenbuttonalliance.espi.common.repositories.customer.CustomerAccountRepository;
import org.greenbuttonalliance.espi.common.repositories.customer.CustomerRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.ApplicationInformationRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.ElectricPowerQualitySummaryRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.IntervalBlockRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.MeterReadingRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.ReadingTypeRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.UsageSummaryRepository;
import org.greenbuttonalliance.espi.common.service.ApplicationInformationService;
import org.greenbuttonalliance.espi.common.service.customer.CustomerAccountService;
import org.greenbuttonalliance.espi.common.service.customer.CustomerService;
import org.greenbuttonalliance.espi.common.service.impl.ApplicationInformationExportService;
import org.greenbuttonalliance.espi.common.service.impl.CustomerAccountExportService;
import org.greenbuttonalliance.espi.common.service.impl.CustomerExportService;
import org.greenbuttonalliance.espi.common.service.impl.ElectricPowerQualitySummaryExportService;
import org.greenbuttonalliance.espi.common.service.impl.IntervalBlockExportService;
import org.greenbuttonalliance.espi.common.service.impl.MeterReadingExportService;
import org.greenbuttonalliance.espi.common.service.impl.ReadingTypeExportService;
import org.greenbuttonalliance.espi.common.service.impl.UsageExportService;
import org.greenbuttonalliance.espi.common.service.impl.UsageSummaryExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;

/**
 * Created by jt, Spring Framework Guru.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class AbstractControllerMockTest {

    @Autowired
    public MockMvc mockMvc;

    @Autowired
    public ObjectMapper objectMapper;

    @MockitoBean
    UsagePointRepository usagePointRepository;

    @MockitoBean
    UsagePointMapper usagePointMapper;

    @MockitoBean
    UsageExportService usageExportService;

    @MockitoBean
    OpaqueTokenIntrospector opaqueTokenIntrospector;

    @MockitoBean
    MeterReadingRepository meterReadingRepository;

    @MockitoBean
    MeterReadingMapper meterReadingMapper;

    @MockitoBean
    MeterReadingExportService meterReadingExportService;

    @MockitoBean
    private RetailCustomerRepository retailCustomerRepository;

    @MockitoBean
    ReadingTypeRepository readingTypeRepository;

    @MockitoBean
    ReadingTypeMapper readingTypeMapper;

    @MockitoBean
    ReadingTypeExportService readingTypeExportService;

    @MockitoBean
    ElectricPowerQualitySummaryRepository electricPowerQualitySummaryRepository;

    @MockitoBean
    ElectricPowerQualitySummaryMapper electricPowerQualitySummaryMapper;

    @MockitoBean
    ElectricPowerQualitySummaryExportService electricPowerQualitySummaryExportService;

    @MockitoBean
    UsageSummaryRepository usageSummaryRepository;

    @MockitoBean
    UsageSummaryMapper usageSummaryMapper;

    @MockitoBean
    UsageSummaryExportService usageSummaryExportService;

    @MockitoBean
    public IntervalBlockRepository intervalBlockRepository;

    @MockitoBean
    public IntervalBlockMapper intervalBlockMapper;

    @MockitoBean
    public IntervalBlockExportService intervalBlockExportService;

    @MockitoBean
    public ApplicationInformationService applicationInformationService;

    @MockitoBean
    public ApplicationInformationRepository applicationInformationRepository;

    @MockitoBean
    public ApplicationInformationExportService applicationInformationExportService;

    @MockitoBean
    public CustomerAccountRepository customerAccountRepository;

    @MockitoBean
    public CustomerAccountMapper customerAccountMapper;

    @MockitoBean
    public CustomerAccountExportService customerAccountExportService;

    @MockitoBean
    public CustomerAccountService customerAccountService;

    @MockitoBean
    public CustomerRepository customerRepository;

    @MockitoBean
    public CustomerMapper customerMapper;

    @MockitoBean
    public CustomerExportService customerExportService;

    @MockitoBean
    public CustomerService customerService;

    /**
     * 403 from the controller's row-level check (#286), not from the URL rule: the token's Function Blocks let it
     * reach the endpoint, but it is not the admin (root collections and entries) or does not hold the path's
     * subscription. Its challenge is the same {@code insufficient_scope} as a URL-rule refusal's (#316).
     */
    public static ResultMatcher refusedByRowScope() {
        return EspiErrorResponses.insufficientScope();
    }

    /** An opaque bearer token with these authorities and no subscription, as a Third Party would send. */
    public static RequestPostProcessor bearer(String... authorities) {
        return opaqueToken().authorities(Arrays.stream(authorities)
                .<GrantedAuthority>map(SimpleGrantedAuthority::new).toList());
    }
}
