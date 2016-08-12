package com.francelabs.datafari.logs;

import org.apache.log4j.Level;

public class MonitoringLevel extends Level {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 454458726846296695L;

	/** . */
	public static final int MONITORING_INT = DEBUG_INT + 20;

	/**
	 * {@link Level} representing my log level
	 */
	public static final Level MONITORING = new StatLevel(MONITORING_INT, "MONITORING", 7);

	/**
	 * Constructor
	 *
	 * @param level
	 * @param levelStr
	 * @param syslogEquivalent
	 */
	protected MonitoringLevel(final int level, final String levelStr, final int syslogEquivalent) {
		super(level, levelStr, syslogEquivalent);
	}

	/**
	 * Checks whether sArg is "MONITORING" level. If yes then returns
	 * {@link StatLevel#MONITORING}, else calls
	 * {@link StatLevel#toLevel(String, Level)} passing it {@link Level#DEBUG}
	 * as the defaultLevel.
	 *
	 * @see Level#toLevel(java.lang.String)
	 * @see Level#toLevel(java.lang.String, org.apache.log4j.Level)
	 *
	 */
	public static Level toLevel(final String sArg) {
		if (sArg != null && sArg.toUpperCase().equals("MONITORING")) {
			return MONITORING;
		}
		return toLevel(sArg, Level.DEBUG);
	}

	/**
	 * Checks whether val is {@link StatLevel#MONITORING_INT}. If yes then
	 * returns {@link StatLevel#MONITORING}, else calls
	 * {@link StatLevel#toLevel(int, Level)} passing it {@link Level#DEBUG} as
	 * the defaultLevel
	 *
	 * @see Level#toLevel(int)
	 * @see Level#toLevel(int, org.apache.log4j.Level)
	 *
	 */
	public static Level toLevel(final int val) {
		if (val == MONITORING_INT) {
			return MONITORING;
		}
		return toLevel(val, Level.DEBUG);
	}

	/**
	 * Checks whether val is {@link StatLevel#MONITORING_INT}. If yes then
	 * returns {@link StatLevel#MONITORING}, else calls
	 * {@link Level#toLevel(int, org.apache.log4j.Level)}
	 *
	 * @see Level#toLevel(int, org.apache.log4j.Level)
	 */
	public static Level toLevel(final int val, final Level defaultLevel) {
		if (val == MONITORING_INT) {
			return MONITORING;
		}
		return Level.toLevel(val, defaultLevel);
	}

	/**
	 * Checks whether sArg is "MONITORING" level. If yes then returns
	 * {@link StatLevel#MONITORING}, else calls
	 * {@link Level#toLevel(java.lang.String, org.apache.log4j.Level)}
	 *
	 * @see Level#toLevel(java.lang.String, org.apache.log4j.Level)
	 */
	public static Level toLevel(final String sArg, final Level defaultLevel) {
		if (sArg != null && sArg.toUpperCase().equals("MONITORING")) {
			return MONITORING;
		}
		return Level.toLevel(sArg, defaultLevel);
	}
}
