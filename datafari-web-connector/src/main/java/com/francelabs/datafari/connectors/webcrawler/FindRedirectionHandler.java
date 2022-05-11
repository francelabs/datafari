/* $Id: FindRedirectionHandler.java 1416199 2012-12-02 16:57:56Z kwright $ */

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

import org.apache.manifoldcf.core.interfaces.*;

/** This class is the handler for redirection parsing during state transitions */
public class FindRedirectionHandler extends FindHandler implements IRedirectionHandler
{
  public FindRedirectionHandler(String parentURI)
  {
    super(parentURI);
  }

}

