/* $Id: AuthenticationCredentials.java 988245 2010-08-23 18:39:35Z kwright $ */

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
package com.francelabs.datafari.connectors.webcrawler;

/** This interface describes immutable classes which represents authentication information for all kinds of authentication.
*/
public interface AuthenticationCredentials
{
  public static final String _rcsid = "@(#)$Id: AuthenticationCredentials.java 988245 2010-08-23 18:39:35Z kwright $";

  /** Compare against another object */
  public boolean equals(Object o);

  /** Calculate a hash function */
  public int hashCode();
}
