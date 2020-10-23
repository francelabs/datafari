/* $Id$ */

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

package com.francelabs.datafari.transformation.emptier;

/**
 * Parameters for Tika transformation connector.
 */
public class EmptierConfig {

  // Configuration parameters

  // Specification nodes and values
  // Specification nodes and values
  public static final String NODE_FILTERFIELD = "filterfield";
  public static final String NODE_INCLUDEFILTER = "includefilter";
  public static final String NODE_EXCLUDEFILTER = "excludefilter";
  public static final String NODE_MAXDOCSIZE = "maxdocsize";
  public static final String NODE_MINDOCSIZE = "mindocsize";
  public static final String INCLUDEFILTER_DEFAULT = ".*";
  public static final String ATTRIBUTE_REGEX = "regex";
  public static final String ATTRIBUTE_VALUE = "value";

}
