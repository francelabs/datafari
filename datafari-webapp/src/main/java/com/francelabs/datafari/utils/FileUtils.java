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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {

	/**
	 * Get the file content as a String
	 *
	 * @param file
	 * @return the file content as a String
	 * @throws IOException
	 */
	public static String getFileContent(final File file) throws IOException {
		final Path filePath = Paths.get(file.getAbsolutePath());
		final Charset charset = StandardCharsets.UTF_8;
		return new String(Files.readAllBytes(filePath), charset);
	}

	/**
	 * Save a String as a File
	 *
	 * @param file
	 *            the file to save
	 * @param fileContent
	 *            the String content of the file
	 * @throws Exception
	 */
	public static void saveStringToFile(final File file, final String fileContent) throws Exception {
		final Path configPath = Paths.get(file.getAbsolutePath());
		Files.write(configPath, fileContent.getBytes(StandardCharsets.UTF_8));
	}

}
