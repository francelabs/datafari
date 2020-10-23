/* $Id: GetConnectionInfo.java 988245 2010-08-23 18:39:35Z kwright $ */

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

import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.agents.interfaces.*;
import org.apache.manifoldcf.crawler.interfaces.*;
import org.apache.manifoldcf.crawler.system.Logging;
import org.apache.manifoldcf.crawler.system.ManifoldCF;

public class GetConnectionInfo
{
  public static final String _rcsid = "@(#)$Id: GetConnectionInfo.java 988245 2010-08-23 18:39:35Z kwright $";

  private GetConnectionInfo()
  {
  }


  public static void main(String[] args)
  {
    if (args.length != 1)
    {
      System.err.println("Usage: GetConnectionInfo <connection_name>");
      System.err.println("");
      System.err.println("The result will be printed to standard out, and will contain the following columns:");
      System.err.println("    share_server");
      System.exit(1);
    }

    String connectionName = args[0];

    try
    {
      IThreadContext tc = ThreadContextFactory.make();
      ManifoldCF.initializeEnvironment(tc);
      IRepositoryConnectionManager connectionManager = RepositoryConnectionManagerFactory.make(tc);
      IRepositoryConnection connection = connectionManager.load(connectionName);
      if (connection == null)
        throw new ManifoldCFException("Connection "+connectionName+" does not exist");

      if (connection.getClassName() == null || !connection.getClassName().equals("org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector"))
        throw new ManifoldCFException("Command can only be used on working share connector connections.");

      ConfigParams cfg = connection.getConfigParams();

      UTF8Stdout.println(commaEscape(cfg.getParameter(SharedDriveParameters.server)));

      System.err.println("Connection info done");
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.exit(2);
    }
  }

  protected static String commaEscape(String input)
  {
    StringBuilder output = new StringBuilder();
    int i = 0;
    while (i < input.length())
    {
      char x = input.charAt(i++);
      if (x == '\\' || x == ',')
        output.append("\\");
      output.append(x);
    }
    return output.toString();
  }

}
