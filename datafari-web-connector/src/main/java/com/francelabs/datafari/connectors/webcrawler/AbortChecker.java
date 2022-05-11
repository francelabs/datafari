/* $Id: AbortChecker.java 1650911 2015-01-11 16:20:23Z kwright $ */

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
import org.apache.manifoldcf.connectorcommon.interfaces.*;
import org.apache.manifoldcf.agents.interfaces.*;
import org.apache.manifoldcf.crawler.interfaces.*;

/** This class furnishes an abort signal whenever the job activity says it should.
* It should never be invoked from a background thread, only from a ManifoldCF thread.
*/
public class AbortChecker implements IBreakCheck
{
  protected final IAbortActivity activities;
  protected ServiceInterruption serviceInterruption = null;
  protected ManifoldCFException mcfException = null;
    
  public AbortChecker(IAbortActivity activities)
  {
    this.activities = activities;
  }
    
  @Override
  public long abortCheck()
    throws BreakException, InterruptedException
  {
    try
    {
      activities.checkJobStillActive();
      return 1000L;
    }
    catch (ServiceInterruption e)
    {
      serviceInterruption = e;
      throw new BreakException("Break requested: "+e.getMessage(),e);
    }
    catch (ManifoldCFException e)
    {
      if (e.getErrorCode() == ManifoldCFException.INTERRUPTED)
        throw new InterruptedException("Interrupted: "+e.getMessage());
      mcfException = e;
      throw new BreakException("Error during break check: "+e.getMessage(),e);
    }
  }
    
  public void rethrowExceptions()
    throws ManifoldCFException, ServiceInterruption
  {
    if (serviceInterruption != null)
      throw serviceInterruption;
    if (mcfException != null)
      throw mcfException;
  }
}
