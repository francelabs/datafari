/* $Id: FormDataAccumulator.java 1756230 2016-08-12 22:20:00Z kwright $ */

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
import java.util.*;
import java.util.regex.*;

/** This class accumulates form data and allows overrides */
public class FormDataAccumulator implements FormData
{
  // Note well: We don't handle multipart posts at this time!!

  // Element categorization
  protected final static int ELEMENTCATEGORY_FREEFORM = 0;
  protected final static int ELEMENTCATEGORY_FIXEDEXCLUSIVE = 1;
  protected final static int ELEMENTCATEGORY_FIXEDINCLUSIVE = 2;

  /** The form's action URI */
  protected String actionURI;
  /** The form's submit method */
  protected int submitMethod;

  /** The set of elements */
  protected ArrayList elementList = new ArrayList();

  public FormDataAccumulator(String actionURI, int submitMethod)
  {
    this.actionURI = actionURI;
    this.submitMethod = submitMethod;
  }

  public void addElement(Map attributes)
  {
    // Interpret the input tag, and make a list of the potential elements we'll want to submit
    String type = (String)attributes.get("type");
    if (type == null)
      type = "text";
    String name = (String)attributes.get("name");
    if (name != null)
    {
      String lowerType = type.toLowerCase(Locale.ROOT);
      if (lowerType.equals("submit"))
      {
        String value = (String)attributes.get("value");
        if (value == null)
          value = "Submit Form";
        elementList.add(new FormItem(name,value,ELEMENTCATEGORY_FREEFORM,true));
      }
      else if (lowerType.equals("hidden") || lowerType.equals("text") || lowerType.equals("password"))
      {
        String value = (String)attributes.get("value");
        if (value == null)
          value = "";
        elementList.add(new FormItem(name,value,ELEMENTCATEGORY_FREEFORM,true));
      }
      else if (lowerType.equals("select"))
      {
        String value = (String)attributes.get("value");
        if (value == null)
          value = "";
        String selected = (String)attributes.get("selected");
        boolean isSelected = false;
        if (selected != null)
          isSelected = true;
        String multiple = (String)attributes.get("multiple");
        boolean isMultiple = false;
        if (multiple != null)
          isMultiple = true;
        elementList.add(new FormItem(name,value,isMultiple?ELEMENTCATEGORY_FIXEDINCLUSIVE:ELEMENTCATEGORY_FIXEDEXCLUSIVE,isSelected));
      }
      else if (lowerType.equals("radio"))
      {
        String value = (String)attributes.get("value");
        if (value == null)
          value = "";
        String selected = (String)attributes.get("checked");
        boolean isSelected = false;
        if (selected != null)
          isSelected = true;
        elementList.add(new FormItem(name,value,ELEMENTCATEGORY_FIXEDEXCLUSIVE,isSelected));
      }
      else if (lowerType.equals("checkbox"))
      {
        String value = (String)attributes.get("value");
        if (value == null)
          value = "";
        String selected = (String)attributes.get("checked");
        boolean isSelected = false;
        if (selected != null)
          isSelected = true;
        elementList.add(new FormItem(name,value,ELEMENTCATEGORY_FIXEDINCLUSIVE,isSelected));
      }
      else if (lowerType.equals("textarea"))
      {
        elementList.add(new FormItem(name,"",ELEMENTCATEGORY_FREEFORM,true));
      }
    }
  }

  public void overrideActionURI(String overrideURI)
  {
    this.actionURI = overrideURI;
  }
  
  public void applyOverrides(LoginParameters lp)
  {
    // This map contains the control names we have ALREADY wiped clean.
    Map overrideMap = new HashMap();

    // Override the specified elements with the specified values
    int i = 0;
    while (i < lp.getParameterCount())
    {
      Pattern namePattern = lp.getParameterNamePattern(i);
      String value = lp.getParameterValue(i);
      i++;

      // For each parameter specified, go through the element list and do the right thing.  This will require us to keep some state around about
      // what exactly we've done to the element list so far, so that each parameter rule in turn applies properly.
      //
      // Each rule regular expression will be deemed to apply to all matching controls.  If the rule matches the control name, then the precise behavior
      // will depend on the type of the control.
      //
      // Controls can be categorized in the following way:
      // - free-form value
      // - specified exclusive value (e.g. radio button)
      // - specified inclusive value (e.g. checkbox)
      //
      // For free-form values, the value given will simply override the value of the element.
      // For exclusive controls, all values in the family will be disabled, and the value matching the one specified will be enabled.
      // For inclusive controls, all values in the family will be cleared ONCE, and then subsequently the value matching the one specified will be enabled.
      //
      int j = 0;
      while (j < elementList.size())
      {
        FormItem fi = (FormItem)elementList.get(j++);
        Matcher m = namePattern.matcher(fi.getElementName());
        if (m.find())
        {
          // Hey, it seems to apply!
          switch (fi.getType())
          {
          case ELEMENTCATEGORY_FREEFORM:
            // Override immediately
            fi.setValue(value);
            break;
          case ELEMENTCATEGORY_FIXEDEXCLUSIVE:
            // If it doesn't match the value, disable.
            fi.setEnabled(fi.getElementValue().equals(value));
            break;
          case ELEMENTCATEGORY_FIXEDINCLUSIVE:
            // Make sure we clear the entire control ONCE (and only once).
            if (overrideMap.get(fi.getElementName()) == null)
            {
              // Zip through the entire list
              int k = 0;
              while (k < elementList.size())
              {
                FormItem fi2 = (FormItem)elementList.get(k++);
                if (fi2.getElementName().equals(fi.getElementName()))
                  fi.setEnabled(false);
              }
              overrideMap.put(fi.getElementName(),fi.getElementName());
            }
            if (fi.getElementValue().equals(value))
              fi.setEnabled(true);
          default:
            break;
          }
        }
      }
    }
  }

  /** Get the full action URI for this form. */
  public String getActionURI()
  {
    return actionURI;
  }

  /** Get the submit method for this form. */
  public int getSubmitMethod()
  {
    return submitMethod;
  }

  /** Iterate over the active form data elements.  The returned iterator returns FormDataElement objects. */
  public Iterator getElementIterator()
  {
    return new FormItemIterator(elementList);
  }

  /** Iterator over FormItems */
  protected static class FormItemIterator implements Iterator
  {
    protected ArrayList elementList;
    protected int currentIndex = 0;

    public FormItemIterator(ArrayList elementList)
    {
      this.elementList = elementList;
    }

    public boolean hasNext()
    {
      while (true)
      {
        if (currentIndex == elementList.size())
          return false;
        if (((FormItem)elementList.get(currentIndex)).getEnabled() == false)
          currentIndex++;
        else
          break;
      }
      return true;
    }

    public Object next()
    {
      while (true)
      {
        if (currentIndex == elementList.size())
          throw new NoSuchElementException("No such element");
        if (((FormItem)elementList.get(currentIndex)).getEnabled() == false)
          currentIndex++;
        else
          break;
      }
      return elementList.get(currentIndex++);
    }

    public void remove()
    {
      throw new UnsupportedOperationException("Unsupported operation");
    }
  }

}
