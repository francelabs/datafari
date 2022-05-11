/* $Id: FormData.java 988245 2010-08-23 18:39:35Z kwright $ */

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

import java.util.*;

/** This interface describes the form data gleaned from an HTML page.  The data will be combined with override information from a LoginParameters
* object to construct the proper information for a form submission.
*/
public interface FormData
{
  public static final String _rcsid = "@(#)$Id: FormData.java 988245 2010-08-23 18:39:35Z kwright $";

  // Submit methods
  public final static int SUBMITMETHOD_GET = 0;
  public final static int SUBMITMETHOD_POST = 1;

  /** Get the full action URI for this form. */
  public String getActionURI();

  /** Get the submit method for this form. */
  public int getSubmitMethod();

  /** Iterate over the active form data elements.  The returned iterator returns FormDataElement objects. */
  public Iterator getElementIterator();

}
