/**
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.manifoldcf.authorities.confluence.tests;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.manifoldcf.authorities.authorities.confluence.v6.ConfluenceAuthorityConnector;
import org.apache.manifoldcf.authorities.interfaces.AuthorizationResponse;
import org.apache.manifoldcf.authorities.interfaces.IAuthorityConnector;
import org.apache.manifoldcf.core.interfaces.ICacheManager;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.client.ConfluenceClient;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.ConfluenceUser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConfluenceAuthorityTest {

	@Mock
	private ConfluenceClient client;
	
	private ConfluenceAuthorityConnector connector;
	
	@Before
	public void setup() throws Exception{
		connector = new ConfluenceAuthorityConnector();
		connector.setConfluenceClient(client);
//		ICacheManager cacheManager = mock(ICacheManager.class);
//		connector.setCacheManager(cacheManager);
	}
	
	@Test
	public void checkMockInjection() throws Exception{
		when(client.checkAuth()).thenReturn(true);
		Assert.assertEquals(connector.check(), "Connection working");
	}
	
	@Test
	public void checkUserNotFound() throws Exception{
		ConfluenceUser user = mock(ConfluenceUser.class);
		when(user.getUsername()).thenReturn(null);
		when(client.getUserAuthorities(anyString())).thenReturn(user);
		AuthorizationResponse response = connector.getAuthorizationResponse(anyString());
		String[] tokens = response.getAccessTokens();
		Assert.assertEquals(tokens.length, 1);
		Assert.assertEquals(tokens[0], IAuthorityConnector.GLOBAL_DENY_TOKEN);
		Assert.assertEquals(response.getResponseStatus(), AuthorizationResponse.RESPONSE_USERNOTFOUND);
	}
	
	@Test
	public void checkUserFound() throws Exception{
		ConfluenceUser user = mock(ConfluenceUser.class);
		when(user.getUsername()).thenReturn("A");
		List<String> tokens = new ArrayList<String>();
		tokens.add("B");
		when(user.getAuthorities()).thenReturn(tokens);
		when(client.getUserAuthorities(anyString())).thenReturn(user);
		AuthorizationResponse response = connector.getAuthorizationResponse(anyString());
		String[] tokens_aux = response.getAccessTokens();
		Assert.assertEquals(tokens_aux.length, 1);
		Assert.assertEquals(tokens_aux[0], tokens.get(0));
		Assert.assertEquals(response.getResponseStatus(), AuthorizationResponse.RESPONSE_OK);
	}
	
}
