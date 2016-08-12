package com.francelabs.datafari.logs;

import org.apache.log4j.Level;

public class StatLevel extends Level {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 7548950173635885457L;

	/** STAT_INT. */
	public static final int STAT_INT = DEBUG_INT + 10;

	/**
	 * {@link Level} representing my log level
	 */
	public static final Level STAT = new StatLevel(STAT_INT, "STAT", 7);

	/**
	 * Constructor
	 *
	 * @param level
	 * @param levelStr
	 * @param syslogEquivalent
	 */
	protected StatLevel(final int level, final String levelStr, final int syslogEquivalent) {
		super(level, levelStr, syslogEquivalent);
	}

	/**
	 * Checks whether sArg is "STAT" level. If yes then returns
	 * {@link StatLevel#STAT}, else calls
	 * {@link StatLevel#toLevel(String, Level)} passing it {@link Level#DEBUG}
	 * as the defaultLevel.
	 *
	 * @see Level#toLevel(java.lang.String)
	 * @see Level#toLevel(java.lang.String, org.apache.log4j.Level)
	 *
	 */
	public static Level toLevel(final String sArg) {
		if (sArg != null && sArg.toUpperCase().equals("STAT")) {
			return STAT;
		}
		return toLevel(sArg, Level.DEBUG);
	}

	/**
	 * Checks whether val is {@link StatLevel#STAT_INT}. If yes then returns
	 * {@link StatLevel#STAT}, else calls {@link StatLevel#toLevel(int, Level)}
	 * passing it {@link Level#DEBUG} as the defaultLevel
	 *
	 * @see Level#toLevel(int)
	 * @see Level#toLevel(int, org.apache.log4j.Level)
	 *
	 */
	public static Level toLevel(final int val) {
		if (val == STAT_INT) {
			return STAT;
		}
		return toLevel(val, Level.DEBUG);
	}

	/**
	 * Checks whether val is {@link StatLevel#STAT_INT}. If yes then returns
	 * {@link StatLevel#STAT}, else calls
	 * {@link Level#toLevel(int, org.apache.log4j.Level)}
	 *
	 * @see Level#toLevel(int, org.apache.log4j.Level)
	 */
	public static Level toLevel(final int val, final Level defaultLevel) {
		if (val == STAT_INT) {
			return STAT;
		}
		return Level.toLevel(val, defaultLevel);
	}

	/**
	 * Checks whether sArg is "STAT" level. If yes then returns
	 * {@link StatLevel#STAT}, else calls
	 * {@link Level#toLevel(java.lang.String, org.apache.log4j.Level)}
	 *
	 * @see Level#toLevel(java.lang.String, org.apache.log4j.Level)
	 */
	public static Level toLevel(final String sArg, final Level defaultLevel) {
		if (sArg != null && sArg.toUpperCase().equals("STAT")) {
			return STAT;
		}
		return Level.toLevel(sArg, defaultLevel);
	}

}
