package com.francelabs.datafari.servlets.admin;

import java.util.concurrent.Semaphore;
/**Javadoc
 * 
 * This class is a mutex semaphore that only applies to one of the many files they can be applied to.
 * To create one you have to provide a language and a type (for now Stop for stopwords and Syn for Synonyms)
 * @author Alexis Karassev
 *
 */

public class SemaphoreLn extends Semaphore {
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
