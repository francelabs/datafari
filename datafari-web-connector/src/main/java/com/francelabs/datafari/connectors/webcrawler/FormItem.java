/* $Id: FormItem.java 1005681 2010-10-08 00:27:46Z kwright $ */

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

/** This class provides an individual data item */
public class FormItem implements FormDataElement
{
  protected String name;
  protected String value;
  protected boolean isEnabled;
  protected int type;

  public FormItem(String name, String value, int type, boolean isEnabled)
  {
    this.name = name;
    this.value = value;
    this.isEnabled = isEnabled;
    this.type = type;
  }

  public void setEnabled(boolean enabled)
  {
    isEnabled = enabled;
  }

  public boolean getEnabled()
  {
    return isEnabled;
  }

  public void setValue(String value)
  {
    this.value = value;
  }

  public int getType()
  {
    return type;
  }

  /** Get the element name */
  public String getElementName()
  {
    return name;
  }

  /** Get the element value */
  public String getElementValue()
  {
    return value;
  }

}
