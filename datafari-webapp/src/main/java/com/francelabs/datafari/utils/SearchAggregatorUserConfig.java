package com.francelabs.datafari.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class SearchAggregatorUserConfig {

    private static final Logger LOGGER = LogManager.getLogger(SearchAggregatorUserConfig.class.getName());
    private static final SearchAggregatorConfiguration sac = SearchAggregatorConfiguration.getInstance();
    private static final String SEPARATOR = ";";
    private static final String SITES_SEPARATOR = ",";
    private static SearchAggregatorUserConfig instance;

    public static SearchAggregatorUserConfig getInstance() {
        if (instance == null) {
            instance = new SearchAggregatorUserConfig();
        }
        return instance;
    }

    public ArrayList<String> getDefaultSourceFor(String username) {
        ArrayList<String> result = new ArrayList<>();
        String defaultSourcesFilePath = sac.getProperty(SearchAggregatorConfiguration.USERS_DEFAULT_SOURCE_FILE);
        if (defaultSourcesFilePath != null && defaultSourcesFilePath.trim().length() > 0) {
            BufferedReader defaultSourcesReader = null;
            try {
                defaultSourcesReader = new BufferedReader(new FileReader(defaultSourcesFilePath));
                String line = "";
                while ((line = defaultSourcesReader.readLine()) != null && result.size()==0) {
                    String[] fields = line.split(SEPARATOR);
                    if (fields.length == 2 && fields[0].equals(username)) {
                        if (fields[1] != null && fields[1].trim().length() > 0) {
                            String[] values = fields[1].split(SITES_SEPARATOR);
                            result.addAll(Arrays.asList(values));
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Couldn't read the aggregator default sources file located at " + defaultSourcesFilePath,
                        e);
            } finally {
                if (defaultSourcesReader != null) {
                    try {
                        defaultSourcesReader.close();
                    } catch (IOException e) {
                        LOGGER.error("Error closing the aggregator default sources file reader",e);
                    }
                }
            }
        }
        return result;
    }

    public ArrayList<String> getAllowedSourcesFor(String username) {
        ArrayList<String> result = null;
        String allowedSroucesFilePath = sac.getProperty(SearchAggregatorConfiguration.USERS_ALLOWED_SOURCES_FILE);
        if (allowedSroucesFilePath != null && allowedSroucesFilePath.trim().length() > 0) {
            BufferedReader allowedSourcesReader = null;
            try {
                allowedSourcesReader = new BufferedReader(new FileReader(allowedSroucesFilePath));
                String line = "";
                while ((line = allowedSourcesReader.readLine()) != null && result == null) {
                    String[] fields = line.split(SEPARATOR);
                    if (fields.length == 2 && fields[0].equals(username)) {
                        String[] sites = fields[1].split(SITES_SEPARATOR);
                        result = new ArrayList<>(Arrays.asList(sites));
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Couldn't read the aggregator allowed sources file located at " + allowedSroucesFilePath,
                        e);
            } finally {
                if (allowedSourcesReader != null) {
                    try {
                        allowedSourcesReader.close();
                    } catch (IOException e) {
                        LOGGER.error("Error closing the aggregator allowed sources file reader",e);
                    }
                }
            }
        }
        return result;
    }

}