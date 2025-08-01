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

package org.greenbuttonalliance.espi.thirdparty.service.impl;

//import org.greenbuttonalliance.espi.common.domain.RetailCustomer;
//import org.greenbuttonalliance.espi.common.repositories.RetailCustomerRepository;

import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.greenbuttonalliance.espi.common.service.impl.RetailCustomerServiceImpl;
import org.junit.Before;
import org.junit.Test;

//todo - JT, commenting out missing classes
public class RetailCustomerServiceImplTests {
	private RetailCustomerRepository repository;
	private RetailCustomerServiceImpl service;

	@Before
	public void setup() {
//		repository = mock(RetailCustomerRepository.class);
//		service = new RetailCustomerServiceImpl();
//		service.setRetailCustomerRepository(repository);
	}

	@Test
	public void loadUserByUsername() {
//		service.loadUserByUsername("alan");
//
//		verify(repository).findByUsername("alan");
	}

	@Test
	public void persist() {
//		RetailCustomer retailCustomer = new RetailCustomer();
//
//		service.persist(retailCustomer);
//
//		verify(repository).persist(retailCustomer);
	}

	@Test
	public void findAll_returnsAllRetailCustomers() {
//		List<RetailCustomer> allRetailCustomers = new ArrayList<RetailCustomer>();
//
//		when(repository.findAll()).thenReturn(allRetailCustomers);
//
//		assertEquals(allRetailCustomers, service.findAll());
	}

	@Test
	public void findById_returnsRetailCustomers() {
//		RetailCustomer customer = new RetailCustomer();
//		customer.setId(13L);
//
//		when(repository.findById(customer.getId())).thenReturn(customer);
//
//		assertEquals(customer, service.findById(customer.getId()));
	}
}
