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
import com.francelabs.datafari.service.db.DepartmentDataServicePostgres;

public class Department {

  /**
   * Get the user's department
   *
   * @param username
   *          of the user
   * @return the user's department
   * @throws DatafariServerException
   */
  public static String getDepartment(final String username) throws DatafariServerException {
    return DepartmentDataServicePostgres.getInstance().getDepartment(username);
  }

  /**
   * Set user's department
   *
   * @param username
   * @param department
   * @return CodesReturned.ALLOK if all was ok
   * @throws DatafariServerException
   */
  public static int setDepartment(final String username, final String department) throws DatafariServerException {
    return DepartmentDataServicePostgres.getInstance().setDepartment(username, department);
  }

  /**
   * Update user's department
   *
   * @param username
   * @param department
   * @return CodesReturned.ALLOK if all was ok
   * @throws DatafariServerException
   */
  public static int updateDepartment(final String username, final String department) throws DatafariServerException {
    return DepartmentDataServicePostgres.getInstance().updateDepartment(username, department);
  }

  /**
   * Delete user's department
   *
   * @param username
   * @return CodesReturned.ALLOK if all was ok
   * @throws DatafariServerException
   */
  public static int deleteDepartment(final String username) throws DatafariServerException {
    return DepartmentDataServicePostgres.getInstance().deleteDepartment(username);
  }

}
