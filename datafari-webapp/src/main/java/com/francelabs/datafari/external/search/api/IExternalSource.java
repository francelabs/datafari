package com.francelabs.datafari.external.search.api;

import java.util.Map;

public interface IExternalSource {

  public ResultDocumentList executeQuery(final String query, final int start, final int rows, final Map<String, String[]> additionnalParams) throws Exception;

}
