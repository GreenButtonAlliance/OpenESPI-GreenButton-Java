/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
 *
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

package org.greenbuttonalliance.espi.thirdparty.integration.web.filters;

import org.greenbuttonalliance.espi.thirdparty.web.filter.CORSFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

//@WebMvcTest(CORSFilter.class)
//@TestPropertySource(properties = "spring.profiles.active=test")
@Disabled
public class CORSFilterTests {

	//@Autowired
	private CORSFilter filter;

	//@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	@BeforeEach
	public void setup() {
		this.mockMvc = webAppContextSetup(this.wac).addFilters(filter).build();
	}

	@Test
	public void optionsResponse_hasCorrectFilters() throws Exception {
		RequestBuilder requestBuilder = MockMvcRequestBuilders.options(
				"/ThirdParty").header("Origin", "JUnit_Test");

		mockMvc.perform(requestBuilder)
				.andExpect(
						header().string("Access-Control-Allow-Origin", is("*")))
				.andExpect(
						header().string("Access-Control-Allow-Methods",
								is("GET, POST, PUT, DELETE, OPTIONS")))
				.andExpect(
						header().string(
								"Access-Control-Allow-Headers",
								is("Origin, Authorization, Accept, Content-Type")))
				.andExpect(
						header().string("Access-Control-Max-Age", is("1800")))
				.andReturn();
	}

}
