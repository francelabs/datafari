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
package com.francelabs.datafari.connectors.share;

/**
 * This class describes shared drive connection parameters.
 */
public class SharedDriveParameters {
  public static final String _rcsid = "@(#)$Id: SharedDriveParameters.java 988245 2010-08-23 18:39:35Z kwright $";

  /* SMB/CIFS share server */
  public final static String server = "Server";

  /* Optional domain/realm */
  public final static String domain = "Domain/Realm";

  /* username for the above server */
  public final static String username = "User Name";

  /* password for the above server */
  public final static String password = "Password";

  /* SIDs handling */
  public final static String useSIDs = "Use SIDs";

  /* User-settable bin name */
  public final static String binName = "Bin Name";

  public final static String connectionTimeout = "Connection Timeout";
  public final static String sessionTimeout = "Session Timeout";
  public final static String socketTimeout = "Socket Timeout";
  public final static String responseTimeout = "Response Timeout";
  public final static String resolveOrder = "Resolve Order";
  public final static String listCount = "List Count";
  public final static String smbMaxVersion = "Maximum version of SMB";
}
