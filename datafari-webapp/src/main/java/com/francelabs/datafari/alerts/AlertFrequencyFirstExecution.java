/*******************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.alerts;

public class AlertFrequencyFirstExecution {

	public static enum AlertFrequency {
		DAILY, HOURLY, WEEKLY
	};

	private boolean hasBeenExecuted;
	private AlertFrequency alertFrequency;

	public AlertFrequencyFirstExecution(AlertFrequency alertFrequency,
			boolean hasBeenExecuted) {
		super();
		this.alertFrequency = alertFrequency;
		this.hasBeenExecuted = hasBeenExecuted;
	}

	public AlertFrequency getFrequency() {
		return alertFrequency;
	}

	public void setFrequency(AlertFrequency alertFrequency) {
		this.alertFrequency = alertFrequency;
	}

	public boolean hasBeenExecuted() {
		return hasBeenExecuted;
	}

	public void setHasBeenExecuted(boolean hasBeenExecuted) {
		this.hasBeenExecuted = hasBeenExecuted;
	}
}
