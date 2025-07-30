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

import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.service.db.LangDataServicePostgres;

public class Lang {

  /**
   * Get the preferred language of a user
   *
   * @param username
   *          of the user
   * @return the prefered language.
   * @throws DatafariServerException
   */
  public static String getLang(final String username) throws DatafariServerException {
    return LangDataServicePostgres.getInstance().getLang(username);
  }

  /**
   * Set user's preferred lang
   *
   * @param username
   * @param lang
   * @return CodesReturned.ALLOK if all was ok
   * @throws DatafariServerException
   */
  public static int setLang(final String username, final String lang) throws DatafariServerException {
    return LangDataServicePostgres.getInstance().setLang(username, lang);
  }

  /**
   * Update user's preferred lang
   *
   * @param username
   * @param lang
   * @return CodesReturned.ALLOK if all was ok
   * @throws DatafariServerException
   */
  public static int updateLang(final String username, final String lang) throws DatafariServerException {
    return LangDataServicePostgres.getInstance().updateLang(username, lang);
  }

  /**
   * Delete user's preferred lang
   *
   * @param username
   * @return CodesReturned.ALLOK if all was ok
   * @throws DatafariServerException
   */
  public static int deleteLang(final String username) throws DatafariServerException {
    return LangDataServicePostgres.getInstance().deleteLang(username);
  }

}
