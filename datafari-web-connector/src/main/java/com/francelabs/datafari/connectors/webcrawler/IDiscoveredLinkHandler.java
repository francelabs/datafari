/* $Id: IDiscoveredLinkHandler.java 1895187 2021-11-19 22:16:11Z kwright $ */

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

/** This interface describes the functionality needed by a link extractor to note a discovered link.
*/
public interface IDiscoveredLinkHandler
{
  /** Inform the world of a new base HREF.
  *@param rawURL is the new base HREF, in raw form.  This may be relative, malformed, etc.
  */
  public void noteDiscoveredBase(String rawURL)
    throws ManifoldCFException;

  /** Inform the world of a discovered link.
  *@param rawURL is the raw discovered url.  This may be relative, malformed, or otherwise unsuitable for use until final form is acheived.
  */
  public void noteDiscoveredLink(String rawURL)
    throws ManifoldCFException;
}
