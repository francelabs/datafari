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
