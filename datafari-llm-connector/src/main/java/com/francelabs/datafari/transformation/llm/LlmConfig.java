package com.francelabs.datafari.transformation.llm;

public class LlmConfig {

  // Configuration parameters
  public static final String NODE_ENDPOINT = "llmNodeEndpoint";
  public static final String NODE_LLM = "llmNodeLlmToUse";
  public static final String NODE_EMBEDDINGS_MODEL = "llmNodeEmbeddingsModel";
  public static final String NODE_APIKEY = "llmNodeApiKey";
  public static final String NODE_VECTOR_DIMENSION = "llmNodeVectorDimension";
  public static final String NODE_MAX_CHUNK_SIZE_IN_CHAR = "llmNodeMaxChunkSizeInChar";
  public static final String NODE_MAX_CHUNK_SIZE_IN_TOKENS = "llmNodeMaxChunkSizeInTokens";
  public static final String NODE_MAX_ITERATIONS = "llmNodeMaxIterationsForSummarization";

  // Specification nodes and values
  public static final String NODE_LLM_SERVICE = "llmService";
  public static final String NODE_ENABLE_SUMMARIZE = "enableSummarize";
  public static final String NODE_ENABLE_CATEGORIZE = "enableCategorize";
  public static final String NODE_ENABLE_EMBEDDINGS = "enableEmbeddings";
  public static final String NODE_MAXTOKENS = "maxTokens";
  public static final String NODE_SUMMARIES_LANGUAGE = "summariesLanguage";
  public static final String NODE_CATEGORIES = "categories";
  public static final String ATTRIBUTE_VALUE = "value";
}
