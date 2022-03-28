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

package com.francelabs.datafari.transformation.spacy.fastapi;

/**
 * Parameters for Spacy NER transformation connector.
 */
public class SpacyNERConfig {

  // Configuration parameters
  public static final String PARAM_SERVERADDRESS = "serverAddress";
  public static final String SOCKET_TIMEOUT = "socket_timeout";
  public static final String CONNECTION_TIMEOUT = "connection_timeout";
  public static final String SOCKET_TIMEOUT_DEFAULT_VALUE = "900000";
  public static final String CONNECTION_TIMEOUT_DEFAULT_VALUE = "120000";

  // Specification nodes and values
  public static final String NODE_ENDPOINTTOUSE = "endpointToUse";
  public static final String NODE_MODELTOUSE = "modelToUse";
  public static final String NODE_OUTPUTFIELDPREFIX = "outputFieldPrefix";
  public static final String ATTRIBUTE_VALUE = "value";

}
