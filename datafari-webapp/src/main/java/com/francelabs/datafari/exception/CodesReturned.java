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
package com.francelabs.datafari.exception;

public enum CodesReturned {
	TRUE(3),
	FALSE(2),
	ALREADYPERFORMED(1),
	ALLOK(0),
	GENERALERROR(-1),
	NOTCONNECTED(-2),
	PROBLEMCONNECTIONDATABASE(-3),
	PROBLEMQUERY(-4),
	PARAMETERNOTWELLSET(-5),
	PROBLEMCONNECTIONAD(-6),
	ADUSERNOTEXISTS(-900),
	FAILTOSIGNIN(-101),
	USERALREADYINBASE(-800);

    private final int value;

    CodesReturned(final int newValue) {
        value = newValue;
    }

    public int getValue() { return value; }
    
    @Override
    public String toString() { return Integer.toString(getValue()); }
}
