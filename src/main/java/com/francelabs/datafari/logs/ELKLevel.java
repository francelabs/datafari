package com.francelabs.datafari.logs;

import org.apache.log4j.Level;

public class ELKLevel extends Level {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 6937682440789210049L;

	/** . */
	public static final int ELK_INT = DEBUG_INT + 20;

	/**
	 * {@link Level} representing my log level
	 */
	public static final Level ELK = new StatLevel(ELK_INT, "ELK", 7);

	/**
	 * Constructor
	 *
	 * @param level
	 * @param levelStr
	 * @param syslogEquivalent
	 */
	protected ELKLevel(final int level, final String levelStr, final int syslogEquivalent) {
		super(level, levelStr, syslogEquivalent);
	}

	/**
	 * Checks whether sArg is "ELK" level. If yes then returns
	 * {@link StatLevel#ELK}, else calls
	 * {@link StatLevel#toLevel(String, Level)} passing it {@link Level#DEBUG}
	 * as the defaultLevel.
	 *
	 * @see Level#toLevel(java.lang.String)
	 * @see Level#toLevel(java.lang.String, org.apache.log4j.Level)
	 *
	 */
	public static Level toLevel(final String sArg) {
		if (sArg != null && sArg.toUpperCase().equals("ELK")) {
			return ELK;
		}
		return toLevel(sArg, Level.DEBUG);
	}

	/**
	 * Checks whether val is {@link StatLevel#ELK_INT}. If yes then returns
	 * {@link StatLevel#ELK}, else calls {@link StatLevel#toLevel(int, Level)}
	 * passing it {@link Level#DEBUG} as the defaultLevel
	 *
	 * @see Level#toLevel(int)
	 * @see Level#toLevel(int, org.apache.log4j.Level)
	 *
	 */
	public static Level toLevel(final int val) {
		if (val == ELK_INT) {
			return ELK;
		}
		return toLevel(val, Level.DEBUG);
	}

	/**
	 * Checks whether val is {@link StatLevel#ELK_INT}. If yes then returns
	 * {@link StatLevel#ELK}, else calls
	 * {@link Level#toLevel(int, org.apache.log4j.Level)}
	 *
	 * @see Level#toLevel(int, org.apache.log4j.Level)
	 */
	public static Level toLevel(final int val, final Level defaultLevel) {
		if (val == ELK_INT) {
			return ELK;
		}
		return Level.toLevel(val, defaultLevel);
	}

	/**
	 * Checks whether sArg is "ELK" level. If yes then returns
	 * {@link StatLevel#ELK}, else calls
	 * {@link Level#toLevel(java.lang.String, org.apache.log4j.Level)}
	 *
	 * @see Level#toLevel(java.lang.String, org.apache.log4j.Level)
	 */
	public static Level toLevel(final String sArg, final Level defaultLevel) {
		if (sArg != null && sArg.toUpperCase().equals("ELK")) {
			return ELK;
		}
		return Level.toLevel(sArg, defaultLevel);
	}
}
