package com.francelabs.datafari.statistics;

import java.math.BigDecimal;

public class StatsUtils {

	public static double round(double unrounded, int precision,
			int roundingMode) {
		BigDecimal bd = new BigDecimal(unrounded);
		BigDecimal rounded = bd.setScale(precision, roundingMode);
		return rounded.doubleValue();
	}
}
