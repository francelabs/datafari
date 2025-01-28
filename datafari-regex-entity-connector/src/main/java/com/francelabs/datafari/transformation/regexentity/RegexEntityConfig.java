package com.francelabs.datafari.transformation.regexentity;

public class RegexEntityConfig {
  // Configuration parameters

  // Specification nodes and values
  public static final String NODE_SOURCE_METADATA = "regexEntityNodeSourceMetadata";
  public static final String NODE_REGEX = "regexEntityNodeRegex";
  public static final String NODE_DESTINATION_METADATA = "regexEntityNodeDestinationMetadata";
  public static final String NODE_VALUE_IF_TRUE = "regexEntityNodeValueIfTrue";
  public static final String NODE_VALUE_IF_FALSE = "regexEntityNodeValueIfFalse";
  public static final String NODE_KEEP_ONLY_ONE = "regexEntityNodeKeepOnlyOne";
  public static final String NODE_EXTRACT_REGEX_GROUPS = "regexEntityNodeExtractRegexGroups";
  /** Position for Regex Entity node */
  public static final int POS_NODE_SOURCE_METADATA = 0;
  public static final int POS_NODE_REGEX = 1;
  public static final int POS_NODE_DESTINATION_METADATA = 2;
  public static final int POS_NODE_VALUE_IF_TRUE = 3;
  public static final int POS_NODE_VALUE_IF_FALSE = 4;
  public static final int POS_NODE_KEEP_ONLY_ONE = 5;
  public static final int POS_NODE_EXTRACT_REGEX_GROUPS = 6;

  // Basic field names in html pages. Can be completed by a suffix and/or a prefix
  public static final String ATTRIBUTE_DESTINATION_METADATA_FIELD = "destinationMetadataField";
  public static final String ATTRIBUTE_REGEX_FIELD = "regexField";
  public static final String ATTRIBUTE_SOURCE_METADATA_FIELD = "sourceMetadataField";
  public static final String ATTRIBUTE_VALUE_IF_TRUE_FIELD = "valueIfTrueField";
  public static final String ATTRIBUTE_VALUE_IF_FALSE_FIELD = "valueIfFalseField";
  public static final String ATTRIBUTE_KEEP_ONLY_ONE_FIELD = "keepOnlyOneField";
  public static final String ATTRIBUTE_EXTRACT_REGEX_GROUPS_FIELD = "extractRegexGroupsField";
  public static final String ATTRIBUTE_SPECIFICATION_MAP = "SPECIFICATION_MAP";

  private RegexEntityConfig(){}
}
