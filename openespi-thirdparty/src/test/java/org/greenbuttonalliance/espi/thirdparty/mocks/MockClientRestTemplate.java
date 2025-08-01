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

package org.greenbuttonalliance.espi.thirdparty.mocks;

//import org.greenbuttonalliance.espi.common.domain.AccessToken;
//import org.greenbuttonalliance.espi.thirdparty.web.ClientRestTemplate;

import org.springframework.web.client.RestClientException;

public class MockClientRestTemplate { //} extends ClientRestTemplate {

	@SuppressWarnings("unchecked")
	public <T> T getForObject(String url, Class<T> responseType,
			Object... urlVariables) throws RestClientException {
	//	AccessToken accessToken = new AccessToken();

//		accessToken.setAccessToken("6b945882-8349-471a-915f-25e791971248");
//		accessToken.setTokenType("Bearer");
//		accessToken.setExpiresIn(43199L);
//		accessToken.setScope("read write");

	//	return (T) accessToken;
		return null;
	}
}
