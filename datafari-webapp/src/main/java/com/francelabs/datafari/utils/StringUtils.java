package com.francelabs.datafari.utils;


import java.util.regex.Pattern;

public class StringUtils {

	/**
	 * @param str
	 *            the string to test
	 * @return true if the String contains at least on upper case letter
	 */
	public static boolean hasUppercaseLetters(String str)  {
		final Pattern textPattern = Pattern.compile(".*[A-Z].*");
		return textPattern.matcher(str).matches();
	}

	/**
	 * @param str
	 *            the string to test
	 * @return true if the String contains at least one character out of the whitelist
	 * The whitelist is : letters, digits, "_" , ".", "_", "@"
	 */
	public static boolean hasSpecialChars(String str)  {
		final Pattern textPattern = Pattern.compile(".*[^a-z0-9\\-@_.].*");
		return textPattern.matcher(str).matches();
	}

}
