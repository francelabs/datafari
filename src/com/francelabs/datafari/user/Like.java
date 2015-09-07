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
package com.francelabs.datafari.user;

import java.util.List;

import org.apache.log4j.Logger;

import com.francelabs.datafari.constants.CodesReturned;
import com.francelabs.datafari.service.db.DocumentDataService;

public class Like {

	private final static Logger logger = Logger.getLogger(Like.class);

	
	/**
	 * Add a document to the list of documents liked by the user
	 * @param username of the user
	 * @param idDocument the id that should be liked
	 * @return Like.ALREADYPERFORMED if the like was already done, CodesUser.ALLOK 
	 * if all was ok and CodesUser.PROBLEMCONNECTIONDATABASE if there's an error
	 */
	public static int addLike(String username, String idDocument){
		try{
			return DocumentDataService.getInstance().addLike(username, idDocument);
		}catch(Exception e){
			logger.error(e);
			e.printStackTrace();
			return CodesReturned.PROBLEMCONNECTIONDATABASE;
		}
	}
	
	
	/**
	 * unlike a document 
	 * @param username of the user who unlike a document
	 * @param idDocument the id that should be unliked
	 * @return Like.ALREADYPERFORMED if the like was already done, Like.ALLOK 
	 * if all was ok and Like.CodesReturned.PROBLEMCONNECTIONDATABASE if there's an error
	 */
	public static int unlike(String username, String idDocument){
		try{
			return DocumentDataService.getInstance().unlike(username, idDocument);
		}catch(Exception e){
			logger.error(e);
			e.printStackTrace();
			return CodesReturned.PROBLEMCONNECTIONDATABASE;
		}
	}
	
	/**
	 * get all the likes of a user
	 * @param username of the user
	 * @return an array list of all the the likes of the user. Return null if there's an error.
	 */
	public static List<String> getLikes(String username){
		try{
			return DocumentDataService.getInstance().getLikes(username);
		}catch(Exception e){
			logger.error(e);
			e.printStackTrace();
			return null;
		}
	}


	
	/**
	 * Delete all likes of a user without deleting also his favorites
	 * @param username
	 * @return CodesReturned.ALLOK if the operation was success and CodesReturned.PROBLEMCONNECTIONDATABASE if the database isn't running	
	 */
	public static int removeLikes(String username){
		try{
			return DocumentDataService.getInstance().removeLikes(username);
		}catch(Exception e){
			logger.error(e);
			return CodesReturned.PROBLEMCONNECTIONDATABASE;
		}
	}




}
