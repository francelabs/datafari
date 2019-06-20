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
package com.francelabs.datafari.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

public class TestUtils {

	/**
	 * 
	 * Get the file from a resource name
	 * 
	 * @param resourceName
	 * @return File of the resource
	 */
	public static File getFileFromResourceName(String resourceName) {
		URL url = TestUtils.class.getResource(resourceName);
		return new File(url.getFile());
	}

	/**
	 * Read a resource file and convert to String
	 * 
	 * @param resourceName
	 * @return String of the content of the file
	 * @throws IOException
	 */
	public static String readResource(String resourceName) throws IOException {
		File file = getFileFromResourceName(resourceName);
		return readFile(file);

	}
	
	/**
	 * Read a file and convert to String
	 * 
	 * @param File
	 * @return String of the content of the file
	 * @throws IOException
	 */
	public static String readFile(File file) throws IOException {
		StringBuilder result = new StringBuilder("");
		Scanner scanner = new Scanner(file);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			result.append(line).append("\n");
		}
		scanner.close();
		return result.toString();

	}

}
