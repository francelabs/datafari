package com.francelabs.datafari.transformation.regexentity;

public class RegexEntityConfig {
  // Configuration parameters

  // Specification nodes and values
  public static final String NODE_METADATA_REGEX = "regexEntityNode";
  public static final String NODE_METADATA_SOLR_FIELD = "regexEntityNodeSolrField";
  /** Position for Regex Entity node */
  public static final int POS_NODE_REGEX_ENTITY = 0;
  public static final int POS_NODE_REGEX_ENTITY_SOLR_FIELD = 1;
  
  // Basic field names in html pages. Can be completed by a suffix and/or a prefix
  public static final String ATTRIBUTE_REGEX_FIELD = "regexField";
  public static final String ATTRIBUTE_METADATA_FIELD = "metadataField";
  public static final String ATTRIBUTE_SOLR_FIELD = "solrField";

  public static final String ATTRIBUTE_METADATA_REGEX_MAP = "METADATA_REGEX_MAP";
  public static final String ATTRIBUTE_METADATA_SOLR_FIELD_MAP = "METADATA_SOLR_FIELD_MAP";
}
