package com.francelabs.datafari.utils;

/**
 * Copyright France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class RequestParams {

	Map<String, List<String>> params = new HashMap<String, List<String>>();


	public RequestParams() {
	}
	
	public RequestParams(String params) {
		this.params = parseQuery(params);
	}
	
	public Map<String, List<String>> getQueryMap() {
		return params;
	}

	private Map<String, List<String>> parseQuery(String query) {
		String[] params = query.split("&");
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		for (String param : params) {
			String name = param.split("=")[0];
			String value = param.split("=")[1];

			if (!map.containsKey(name)) {
				map.put(name, new ArrayList<String>());
			}

			List<String> mapValue = map.get(name);
			mapValue.add(value);
		}
		return map;
	}

	public String toQueryString() {
		StringBuilder query = new StringBuilder();
		for (Entry<String, List<String>> param : params.entrySet()) {
			for (String paramValue : param.getValue()) {
				query.append(param.getKey() + "=" + paramValue + "&");
			}
		}
		if (query.length() > 0) {
			query = query.delete(query.length() - 1, query.length());
		}
		return query.toString();
	}

	public void add(String key, String value) {
		if (!params.containsKey(key)) {
			params.put(key, new ArrayList<String>());
		}

		List<String> mapValue = params.get(key);
		mapValue.add(value);
	}

	public void put(String key, String value) {
		params.put(key, new ArrayList<String>());
		
		List<String> mapValue = params.get(key);
		mapValue.add(value);
	}
	
	public void addAll(RequestParams requestParams) {
		for (Entry<String, List<String>> param : requestParams.params.entrySet()){
			for (String paramValue : param.getValue()){
				add(param.getKey(), paramValue);
			}
		}
	}

	public void putAll(RequestParams requestParams) {
		params.putAll(requestParams.params);
	}

}
