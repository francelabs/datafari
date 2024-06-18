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

package org.apache.manifoldcf.crawler.connectors.confluence.v6;

/**
 * <p>
 * ConfluenceConfiguration class
 * </p>
 * <p>
 * Class used to keep configuration parameters for Confluence repository connection
 * </p>
 *
 * @author Julien Massiera &amp; Antonio David Perez Morales
 *
 */
public class ConfluenceConfiguration {

  public static interface Server {
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String PROTOCOL = "protocol";
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String PATH = "path";
    public static final String SOCKET_TIMEOUT = "socket_timeout";
    public static final String CONNECTION_TIMEOUT = "connection_timeout";
    public static final String RETRY_NUMBER = "retryNumber";
    public static final String RETRY_INTERVAL = "retryInterval";

    public static final String PROXY_USERNAME = "proxy_username";
    public static final String PROXY_PASSWORD = "proxy_password";
    public static final String PROXY_PROTOCOL = "proxy_protocol";
    public static final String PROXY_HOST = "proxy_host";
    public static final String PROXY_PORT = "proxy_port";

    public static final String PROTOCOL_DEFAULT_VALUE = "http";
    public static final String HOST_DEFAULT_VALUE = "";
    public static final String PORT_DEFAULT_VALUE = "8090";
    public static final String PATH_DEFAULT_VALUE = "/confluence";
    public static final String USERNAME_DEFAULT_VALUE = "";
    public static final String PASSWORD_DEFAULT_VALUE = "";
    public static final String SOCKET_TIMEOUT_DEFAULT_VALUE = "900000";
    public static final String CONNECTION_TIMEOUT_DEFAULT_VALUE = "60000";
    public static final String RETRY_NUMBER_DEFAULT_VALUE = "2";
    public static final String RETRY_INTERVAL_DEFAULT_VALUE = "20000";

    public static final String PROXY_USERNAME_DEFAULT_VALUE = "";
    public static final String PROXY_PASSWORD_DEFAULT_VALUE = "";
    public static final String PROXY_PROTOCOL_DEFAULT_VALUE = "http";
    public static final String PROXY_HOST_DEFAULT_VALUE = "";
    public static final String PROXY_PORT_DEFAULT_VALUE = "";
  }

  public static interface Authority {
    public static final String CACHE_LIFETIME = "cache_lifetime";
    public static final String CACHE_LRU_SIZE = "cache_lru_size";
  }

  public static interface Specification {
    public static final String SPACES = "spaces";
    public static final String SPACE = "space";
    public static final String SPACE_KEY_ATTRIBUTE = "key";
    public static final String PAGES = "pages";
    public static final String SECURITY = "security";
    public static final String ACTIVATE_SECURITY_ATTRIBUTE_KEY = "activate_security";
    public static final String PROCESS_ATTACHMENTS_ATTRIBUTE_KEY = "process_attachments";
    public static final String PAGETYPE = "pagetype";

  }

}
