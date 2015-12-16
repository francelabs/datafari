/*******************************************************************************
 *  * Copyright 2015 France Labs
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  * 
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;


public class SendHttpRequest {
	
	public final static String USERAGENT = "Mozilla/5.0";
	
	public static void sendGET(String url) throws IOException {
		sendGET(url,USERAGENT);
	}
	private static Logger logger = Logger.getLogger(SendHttpRequest.class);
	
	public static void sendGET(String url,String userAgent) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", userAgent);
        int responseCode = con.getResponseCode();
        logger.debug("GET Response Code :: " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) { // success
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
 
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
 
            // print result
            logger.debug(response.toString());
        } else {
        	logger.debug("GET request not worked");
        }
    }
	
	public static String sendPost(String url, List<NameValuePair> urlParameters) throws Exception {
		return sendPost(url,USERAGENT,urlParameters);
	}
	
	public static String sendPost(String url, String agent,List<NameValuePair> urlParameters) throws Exception {
			
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(url);

			
		post.setHeader("User-Agent", agent);
		
//		urlParameters.add(new BasicNameValuePair("sn", "C02G8416DRJM"));
//			urlParameters.add(new BasicNameValuePair("cn", ""));
//			urlParameters.add(new BasicNameValuePair("locale", ""));
//			urlParameters.add(new BasicNameValuePair("caller", ""));
//			urlParameters.add(new BasicNameValuePair("num", "12345"));

		post.setEntity(new UrlEncodedFormEntity(urlParameters));

		HttpResponse response = client.execute(post);
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + post.getEntity());
		System.out.println("Response Code : " + 
	                                    response.getStatusLine().getStatusCode());

		BufferedReader rd = new BufferedReader(
				new InputStreamReader(response.getEntity().getContent()));
		
		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		return result.toString();
	}

}
