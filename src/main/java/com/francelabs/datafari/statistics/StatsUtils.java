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
package com.francelabs.datafari.statistics;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.solr.common.SolrDocument;

public class StatsUtils {

	/** Statistic fields to put in the logs. */
	public static final List<String> statFields = new ArrayList<String>(Arrays.asList("id", "date", "q", "noHits", "numFound", "numClicks", "QTime",
			"positionClickTot", "click", "history", "url"));

	public static double round(final double unrounded, final int precision, final int roundingMode) {
		final BigDecimal bd = new BigDecimal(unrounded);
		final BigDecimal rounded = bd.setScale(precision, roundingMode);
		return rounded.doubleValue();
	}

	/**
	 * Format the statFields values (extracted from the input Solr document)
	 * into a String. The generated String will be used to generate a statistic
	 * log
	 * <p>
	 * Each statField value is separated by a '|', if a value is null or empty
	 * the '|' separator is still put
	 *
	 * @param solrDocStat
	 * @return
	 */
	public static String createStatLog(final SolrDocument solrDocStat) {
		String stat = "";
		int cpt = 0;
		for (final String statField : statFields) {
			if (cpt > 0) {
				stat += "|";
			} else {
				cpt++;
			}
			final Object value = solrDocStat.getFieldValue(statField);
			if (value != null) {
				if (value instanceof Date) {
					// Create an instance of SimpleDateFormat used for
					// formatting
					// the string representation of date
					final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
					final Date date = (Date) value;
					stat += df.format(date);
				} else {
					stat += value.toString();
				}
			}
		}
		return stat;
	}
}
