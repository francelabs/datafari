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
package com.francelabs.datafari.simplifiedui.utils;

public class WebJob {

  private String repositoryConnection;
  private String seeds;
  private String exclusions;

  public WebJob() {

  }

  public String getRepositoryConnection() {
    return repositoryConnection;
  }

  public void setRepositoryConnection(final String repositoryConnection) {
    this.repositoryConnection = repositoryConnection;
  }

  public String getSeeds() {
    return seeds;
  }

  public void setSeeds(final String seeds) {
    this.seeds = seeds;
  }

  public String getExclusions() {
    return exclusions;
  }

  public void setExclusions(final String exclusions) {
    this.exclusions = exclusions;
  }

}
