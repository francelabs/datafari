/*******************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.servlets.admin;

import java.util.concurrent.Semaphore;
/**Javadoc
 * 
 * This class is a mutex semaphore that only applies to a stopword, or synonym file, with a specified language
 * To create one you have to provide a language and a type (for now Stop for stopwords and Syn for Synonyms)
 * Used in both Synonyms and Stopwords Servlets.
 * @author Alexis Karassev
 *
 */
public class SemaphoreLn extends Semaphore {
	private static final long serialVersionUID = 1L;
	private String language;
	private String type;
	public SemaphoreLn(String lang, String type) {
		super(1);
		this.language = lang;
		this.type=type;
	}
	public String getLanguage(){
		return this.language;
	}
	public String getType(){
		return this.type;
	}
}
