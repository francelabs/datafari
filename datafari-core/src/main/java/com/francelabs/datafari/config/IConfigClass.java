/*******************************************************************************
 * Copyright 2019 France Labs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.config;

import java.io.IOException;

public interface IConfigClass {

  public String getProperty(final String key) throws IOException;

  public String getProperty(final String key, final String defaultValue);

  public void setProperty(final String key, final String value);

  public void saveProperties() throws IOException;

  public void listenChanges();

  public void stopListeningChanges();

  public void onPropertiesReloaded();

}
