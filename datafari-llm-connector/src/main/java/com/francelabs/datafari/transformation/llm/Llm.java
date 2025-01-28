/**
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.francelabs.datafari.transformation.llm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import com.francelabs.datafari.transformation.llm.services.ILlmService;
import com.francelabs.datafari.transformation.llm.services.LlmService;
import com.francelabs.datafari.transformation.llm.services.OpenAiLlmService;
import com.francelabs.datafari.transformation.llm.model.LlmSpecification;
import com.francelabs.datafari.transformation.llm.utils.ChunkUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.agents.interfaces.IOutputAddActivity;
import org.apache.manifoldcf.agents.interfaces.IOutputCheckActivity;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.agents.system.Logging;
import org.apache.manifoldcf.agents.transformation.BaseTransformationConnector;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.Specification;
import org.apache.manifoldcf.core.interfaces.SpecificationNode;
import org.apache.manifoldcf.core.interfaces.VersionContext;

import com.francelabs.datafari.transformation.llm.utils.storage.DestinationStorage;



/**
 * Connector to extract entities using a regular expression from document content and put them in metadata.
 *
 */
public class Llm extends BaseTransformationConnector {

  public static final String _rcsid = "@(#)$Id: "+ Llm.class.getName() + " $";
  public static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/";
  private static final int DEFAULT_MAXTOKENS = 500;

  private static final String EDIT_CONFIGURATION_JS = "editConfiguration.js";
  private static final String EDIT_CONFIGURATION_SERVER_HTML = "editConfiguration_llm.html";
  private static final String VIEW_CONFIGURATION_HTML = "viewConfiguration.html";
  private static final String EDIT_SPECIFICATION_JS = "editSpecification.js";
  private static final String EDIT_SPECIFICATION_HTML = "editSpecification_llm.html";
  private static final String VIEW_SPECIFICATION_HTML = "viewSpecification.html";

  
  protected static final String ACTIVITY_LLM = "LLM";
  protected static final String CONTENT = "content";
  protected static final String SEQNUM = "SEQNUM";

  protected static final String[] activitiesList = new String[] { ACTIVITY_LLM };
  private static final Logger LOGGER = LogManager.getLogger(Llm.class.getName());


  /**
   * Connect this connector. The configuration parameters are included.
   * 
   * @param configParams are the configuration parameters for this connection.
   */
  @Override
  public void connect(ConfigParams configParams) {
    super.connect(configParams);
  }

  /**
   * Close the connection. Call this before discarding the repository connector.
   */
  @Override
  public void disconnect() throws ManifoldCFException {
    super.disconnect();
  }

  /**
   * This method is periodically called for all connectors that are connected but
   * not in active use.
   */
  @Override
  public void poll() throws ManifoldCFException {
  }

  /**
   * This method is called to assess whether to count this connector instance should actually be counted as being connected.
   *
   * @return true if the connector instance is actually connected.
   */
  @Override
  public boolean isConnected() {
    return true;
  }


  /**
   * Return a list of activities that this connector generates. The connector does NOT need to be connected before this method is called.
   *
   * @return the set of activities.
   */
  @Override
  public String[] getActivitiesList() {
    return activitiesList;
  }

  /**
   * Output the configuration header section. This method is called in the head section of the connector's configuration page. Its purpose is to add the required tabs to the list, and to output any
   * javascript methods that might be needed by the configuration editing HTML.
   *
   * @param threadContext is the local thread context.
   * @param out           is the output to which any HTML should be sent.
   * @param parameters    are the configuration parameters, as they currently exist, for this connection being configured.
   * @param tabsArray     is an array of tab names. Add to this array any tab names that are specific to the connector.
   */
  @Override
  public void outputConfigurationHeader(final IThreadContext threadContext, final IHTTPOutput out, final Locale locale, final ConfigParams parameters, final List<String> tabsArray)
          throws ManifoldCFException, IOException {
    tabsArray.add(Messages.getString(locale, "llm.TabName"));
    Messages.outputResourceWithVelocity(out, locale, EDIT_CONFIGURATION_JS, null);
  }

  /**
   * Output the configuration body section. This method is called in the body section of the connector's configuration page. Its purpose is to present the required form elements for editing. The coder
   * can presume that the HTML that is output from this configuration will be within appropriate <html>, <body>, and <form> tags. The name of the form is "editconnection".
   *
   * @param threadContext is the local thread context.
   * @param out           is the output to which any HTML should be sent.
   * @param parameters    are the configuration parameters, as they currently exist, for this connection being configured.
   * @param tabName       is the current tab name.
   */
  @Override
  public void outputConfigurationBody(final IThreadContext threadContext, final IHTTPOutput out, final Locale locale, final ConfigParams parameters, final String tabName)
          throws ManifoldCFException, IOException {
    final Map<String, Object> velocityContext = new HashMap<>();
    velocityContext.put("TabName", tabName);
    fillInAPITab(velocityContext, out, parameters);
    Messages.outputResourceWithVelocity(out, locale, EDIT_CONFIGURATION_SERVER_HTML, velocityContext);
  }

  /**
   * Process a configuration post. This method is called at the start of the connector's configuration page, whenever there is a possibility that form data for a connection has been posted. Its
   * purpose is to gather form information and modify the configuration parameters accordingly. The name of the posted form is "editconnection".
   *
   * @param threadContext   is the local thread context.
   * @param variableContext is the set of variables available from the post, including binary file post information.
   * @param parameters      are the configuration parameters, as they currently exist, for this connection being configured.
   * @return null if all is well, or a string error message if there is an error that should prevent saving of the connection (and cause a redirection to an error page).
   */
  @Override
  public String processConfigurationPost(final IThreadContext threadContext, final IPostParameters variableContext, final Locale locale, final ConfigParams parameters) throws ManifoldCFException {

    if (variableContext.getParameter("llmService") != null) {
      parameters.setParameter(LlmConfig.NODE_LLM_SERVICE, variableContext.getParameter("llmService"));
    }
    if (variableContext.getParameter("endpointToUse") != null) {
      parameters.setParameter(LlmConfig.NODE_ENDPOINT, variableContext.getParameter("endpointToUse"));
    }
    if (variableContext.getParameter("llmToUse") != null) {
      parameters.setParameter(LlmConfig.NODE_LLM, variableContext.getParameter("llmToUse"));
    }
    if (variableContext.getParameter("embeddingsModelToUse") != null) {
      parameters.setParameter(LlmConfig.NODE_EMBEDDINGS_MODEL, variableContext.getParameter("embeddingsModelToUse"));
    }
    if (variableContext.getParameter("llmApiKey") != null) {
      parameters.setParameter(LlmConfig.NODE_APIKEY, variableContext.getParameter("llmApiKey"));
    }
    if (variableContext.getParameter("dimensions") != null) {
      parameters.setParameter(LlmConfig.NODE_VECTOR_DIMENSION, variableContext.getParameter("dimensions"));
    }

    return null;
  }

  /**
   * View configuration. This method is called in the body section of the connector's view configuration page. Its purpose is to present the connection information to the user. The coder can presume
   * that the HTML that is output from this configuration will be within appropriate <html> and <body> tags.
   *
   * @param threadContext is the local thread context.
   * @param out           is the output to which any HTML should be sent.
   * @param parameters    are the configuration parameters, as they currently exist, for this connection being configured.
   */
  @Override
  public void viewConfiguration(final IThreadContext threadContext, final IHTTPOutput out, final Locale locale, final ConfigParams parameters) throws ManifoldCFException, IOException {
    final Map<String, Object> velocityContext = new HashMap<>();
    fillInAPITab(velocityContext, out, parameters);
    Messages.outputResourceWithVelocity(out, locale, VIEW_CONFIGURATION_HTML, velocityContext);
  }


  protected static void fillInAPITab(final Map<String, Object> velocityContext, final IHTTPOutput out, final ConfigParams parameters) throws ManifoldCFException {

    String endpointToUse = (parameters.getParameter(LlmConfig.NODE_ENDPOINT) != null) ? parameters.getParameter(LlmConfig.NODE_ENDPOINT) : DEFAULT_ENDPOINT;
    String llmService = (parameters.getParameter(LlmConfig.NODE_LLM_SERVICE) != null) ? parameters.getParameter(LlmConfig.NODE_LLM_SERVICE) : "openai";
    String llmToUse = (parameters.getParameter(LlmConfig.NODE_LLM) != null) ? parameters.getParameter(LlmConfig.NODE_LLM) : "";
    String embeddingsModelToUse = (parameters.getParameter(LlmConfig.NODE_EMBEDDINGS_MODEL) != null) ? parameters.getParameter(LlmConfig.NODE_EMBEDDINGS_MODEL) : "";
    String llmApiKey = (parameters.getParameter(LlmConfig.NODE_APIKEY) != null) ? parameters.getParameter(LlmConfig.NODE_APIKEY) : "";
    String dimensions = (parameters.getParameter(LlmConfig.NODE_VECTOR_DIMENSION) != null) ? parameters.getParameter(LlmConfig.NODE_VECTOR_DIMENSION) : "250";


    // Fill in context
    velocityContext.put("ENDPOINT", endpointToUse);
    velocityContext.put("LLMTYPE", llmService);
    velocityContext.put("EMBEDDINGMODEL", embeddingsModelToUse);
    velocityContext.put("LLM", llmToUse);
    velocityContext.put("APIKEY", llmApiKey);
    velocityContext.put("DIMENSIONS", dimensions);
  }

  /**
   * Get an output version string, given an output specification. The output version string is used to uniquely describe the pertinent details of the output specification and the configuration, to
   * allow the Connector Framework to determine whether a document will need to be output again. Note that the contents of the document cannot be considered by this method, and that a different
   * version string (defined in IRepositoryConnector) is used to describe the version of the actual document.
   *
   * This method presumes that the connector object has been configured, and it is thus able to communicate with the output data store should that be necessary.
   *
   * @param spec is the current output specification for the job that is doing the crawling.
   * @return a string, of unlimited length, which uniquely describes output configuration and specification in such a way that if two such strings are equal, the document will not need to be sent
   *         again to the output data store.
   */
  @Override
  public VersionContext getPipelineDescription(final Specification spec) throws ManifoldCFException, ServiceInterruption {
    String versionString = getVersionString(spec);
    return new VersionContext(versionString, params, spec);
  }

  // ------------------------------------------------------------------------------------------------------------------------------------------------
  // We intercept checks pertaining to the document format and send modified checks further down
  // ------------------------------------------------------------------------------------------------------------------------------------------------

  /**
   * Detect if a mime type is acceptable or not. This method is used to determine whether it makes sense to fetch a document in the first place.
   *
   * @param pipelineDescription is the document's pipeline version string, for this connection.
   * @param mimeType            is the mime type of the document.
   * @param checkActivity       is an object including the activities that can be performed by this method.
   * @return true if the mime type can be accepted by this connector.
   */
  @Override
  public boolean checkMimeTypeIndexable(final VersionContext pipelineDescription, final String mimeType, final IOutputCheckActivity checkActivity) throws ManifoldCFException, ServiceInterruption {
    return true;
  }

  /**
   * Pre-determine whether a document (passed here as a File object) is acceptable or not. This method is used to determine whether a document needs to be actually transferred. This hook is provided
   * mainly to support search engines that only handle a small set of accepted file types.
   *
   * @param pipelineDescription is the document's pipeline version string, for this connection.
   * @param localFile           is the local file to check.
   * @param checkActivity       is an object including the activities that can be done by this method.
   * @return true if the file is acceptable, false if not.
   */
  @Override
  public boolean checkDocumentIndexable(final VersionContext pipelineDescription, final File localFile, final IOutputCheckActivity checkActivity) throws ManifoldCFException, ServiceInterruption {
    // Document contents are not germane anymore, unless it looks like Tika
    // won't accept them.
    // Not sure how to check that...
    return true;
  }

  /**
   * Pre-determine whether a document's length is acceptable. This method is used to determine whether to fetch a document in the first place.
   *
   * @param pipelineDescription is the document's pipeline version string, for this connection.
   * @param length              is the length of the document.
   * @param checkActivity       is an object including the activities that can be done by this method.
   * @return true if the file is acceptable, false if not.
   */
  @Override
  public boolean checkLengthIndexable(final VersionContext pipelineDescription, final long length, final IOutputCheckActivity checkActivity) throws ManifoldCFException, ServiceInterruption {
    // Always true
    return true;
  }
  
  // End Checks -------------------------------------------------------------------------------------------------------------------------------------

  /**
   * Add (or replace) a document in the output data store using the connector. This method presumes that the connector object has been configured, and it is thus able to communicate with the output
   * data store should that be necessary. The OutputSpecification is *not* provided to this method, because the goal is consistency, and if output is done it must be consistent with the output
   * description, since that was what was partly used to determine if output should be taking place. So it may be necessary for this method to decode an output description string in order to determine
   * what should be done.
   * </br></br>
   * This override method's fonctionnality: 
   *
   * @param documentURI         is the URI of the document. The URI is presumed to be the unique identifier which the output data store will use to process and serve the document. This URI is
   *                            constructed by the repository connector which fetches the document, and is thus universal across all output connectors.
   * @param pipelineDescription is the description string that was constructed for this document by the getOutputDescription() method.
   * @param document            is the document data to be processed (handed to the output data store).
   * @param authorityNameString is the name of the authority responsible for authorizing any access tokens passed in with the repository document. May be null.
   * @param activities          is the handle to an object that the implementer of a pipeline connector may use to perform operations, such as logging processing activity, or sending a modified
   *                            document to the next stage in the pipeline.
   * @return the document status (accepted or permanently rejected).
   * @throws IOException only if there's a stream error reading the document data.
   */
  @Override
  public int addOrReplaceDocumentWithException(final String documentURI, final VersionContext pipelineDescription, final RepositoryDocument document, final String authorityNameString,
      final IOutputAddActivity activities) throws ManifoldCFException, ServiceInterruption, IOException {


    final LlmSpecification spec = new LlmSpecification(pipelineDescription.getSpecification(), getConfiguration());

    boolean hasError = false;
    final long startTime = System.currentTimeMillis();

    // Prepare storage for reading document content. A suitable storage depending on content size.
    DestinationStorage storage = DestinationStorage.getDestinationStorage(document.getBinaryLength(), getClass());
    StringBuilder contentBuilder = new StringBuilder();
    try {
      // Reading file content
      try {
              // Transfert document content to the storage
              long binaryLength = document.getBinaryStream().transferTo(storage.getOutputStream());

              // The input stream of the document has been totally read by previous instruction, so set a new one
              document.setBinary(storage.getInputStream(), binaryLength);

              // Prepare reading of document copied to extract metadata
              BufferedReader buffRead = new BufferedReader(new InputStreamReader(storage.getInputStream()));

              // Read lines
              String line = buffRead.readLine();
              while (line != null ) {
                contentBuilder.append(line);
                line = buffRead.readLine();
              }
              buffRead.close();

      } catch (Exception e) {
          hasError = true;
          activities.recordActivity(startTime, ACTIVITY_LLM, document.getBinaryLength(), documentURI, "KO", e.getMessage());
          Logging.ingest.error("Unable to browse document " + documentURI, e);
      }

      // Get content
      String content = contentBuilder.toString();

      // If content is empty, stop here
      if (content.isBlank()) {
        return activities.sendDocument(documentURI, document);
      }

      // Chunking
      List<TextSegment> chunks = ChunkUtils.chunkRepositoryDocument(content, document, spec);

      // Select the proper service depending on the LLM
      LlmService service;
      switch (spec.getTypeOfLlm()) {
        case "openai":
        case "datafari":
        default:
          service = new OpenAiLlmService(spec);
          break;
      }


      // SUMMARIZE DOCUMENTS
      if (spec.getEnableSummarize()) {
        try {
          String summary = service.summarizeRecursively(chunks, spec);
          if (summary.isEmpty()) throw new RuntimeException("Could not generate a summary for document: " + documentURI);
          document.addField("llm_summary", summary);
        } catch (Exception e) {
          LOGGER.warn("Could not generate a summary for document: {}", documentURI);
        }
      }


      // CATEGORIZE DOCUMENTS
      // Invoice, Call for Tenders, Request for Quotations, Technical paper, Presentation, Resumes, Others
      if (spec.getEnableCategorize() && !spec.getCategories().isEmpty()) {
        try {
          List<String> allowedCategories = spec.getCategories();
          String llmResponse = service.categorize(content, spec);
          List<String> docCategories = extractCategories(llmResponse, allowedCategories);
          if (docCategories.isEmpty()) throw new RuntimeException("Could not generate a summary for document: " + documentURI);
          document.addField("llm_categories", docCategories.toArray(new String[0]));
        } catch (Exception e) {
          LOGGER.warn("Could not find category for document: {}", documentURI);
        }
      }


      if (!hasError) activities.recordActivity(startTime, ACTIVITY_LLM, document.getBinaryLength(), documentURI, "OK", "");
      return activities.sendDocument(documentURI, document);

    } finally {
      // Clean storage (for instance, delete temporary file) after all treatment on document done (Solr indexing).
      storage.close();
    }

  }

  /**
   * Extract the category from the LLM response
   *
   * @param message  The LLM esponse
   * @return The category
   */
  public List<String> extractCategories(String message, List<String> allowedCategories) {
    List<String> categories = new ArrayList<>();
    for (String category : allowedCategories) {
      if (message.contains(category)) categories.add(category);
    }
    if (categories.isEmpty()) categories.add("Others");
    return categories;
  }

  
  /**
   * Obtain the name of the form check javascript method to call.
   *
   * @param connectionSequenceNumber is the unique number of this connection within the job.
   * @return the name of the form check javascript method.
   */
  @Override
  public String getFormCheckJavascriptMethodName(final int connectionSequenceNumber) {
    return "s" + connectionSequenceNumber + "_checkSpecification";
  }

  /**
   * Obtain the name of the form presave check javascript method to call.
   *
   * @param connectionSequenceNumber is the unique number of this connection within the job.
   * @return the name of the form presave check javascript method.
   */
  @Override
  public String getFormPresaveCheckJavascriptMethodName(final int connectionSequenceNumber) {
    return "s" + connectionSequenceNumber + "_checkSpecificationForSave";
  }

  /**
   * Output the specification header section. This method is called in the head section of a job page which has selected a pipeline connection of the current type. Its purpose is to add the required
   * tabs to the list, and to output any javascript methods that might be needed by the job editing HTML.
   *
   * @param out                      is the output to which any HTML should be sent.
   * @param locale                   is the preferred local of the output.
   * @param spec                       is the current pipeline specification for this connection.
   * @param connectionSequenceNumber is the unique number of this connection within the job.
   * @param tabsArray                is an array of tab names. Add to this array any tab names that are specific to the connector.
   */
  @Override
  public void outputSpecificationHeader(final IHTTPOutput out, final Locale locale, final Specification spec, final int connectionSequenceNumber, final List<String> tabsArray)
      throws ManifoldCFException, IOException {
    final Map<String, Object> paramMap = new HashMap<>();
    paramMap.put(SEQNUM, Integer.toString(connectionSequenceNumber));

    tabsArray.add(Messages.getString(locale, "llm.TabName"));

    // Fill in the specification header map, using data from all tabs.
    fillInLlmSpecificationMap(paramMap, spec);

    Messages.outputResourceWithVelocity(out, locale, EDIT_SPECIFICATION_JS, paramMap);
  }

  /**
   * Output the specification body section. This method is called in the body section of a job page which has selected a pipeline connection of the current type. Its purpose is to present the required
   * form elements for editing. The coder can presume that the HTML that is output from this configuration will be within appropriate <html>, <body>, and <form> tags. The name of the form is
   * "editjob".
   *
   * @param out                      is the output to which any HTML should be sent.
   * @param locale                   is the preferred local of the output.
   * @param spec                       is the current pipeline specification for this job.
   * @param connectionSequenceNumber is the unique number of this connection within the job.
   * @param actualSequenceNumber     is the connection within the job that has currently been selected.
   * @param tabName                  is the current tab name.
   */
  @Override
  public void outputSpecificationBody(final IHTTPOutput out, final Locale locale, final Specification spec, final int connectionSequenceNumber, final int actualSequenceNumber, final String tabName)
      throws ManifoldCFException, IOException {
    final Map<String, Object> paramMap = new HashMap<>();

    // Set the tab name
    paramMap.put("TABNAME", tabName);
    paramMap.put(SEQNUM, Integer.toString(connectionSequenceNumber));
    paramMap.put("SELECTEDNUM", Integer.toString(actualSequenceNumber));

    // Fill in the field mapping tab data
    fillInLlmSpecificationMap(paramMap, spec);


    Messages.outputResourceWithVelocity(out, locale, EDIT_SPECIFICATION_HTML, paramMap);
  }

  /**
   * Process a specification post. This method is called at the start of job's edit or view page, whenever there is a possibility that form data for a connection has been posted. Its purpose is to
   * gather form information and modify the transformation specification accordingly. The name of the posted form is "editjob".
   *
   * @param variableContext          contains the post data, including binary file-upload information.
   * @param locale                   is the preferred local of the output.
   * @param spec                     is the current pipeline specification for this job.
   * @param connectionSequenceNumber is the unique number of this connection within the job.
   * @return null if all is well, or a string error message if there is an error that should prevent saving of the job (and cause a redirection to an error page).
   */
  @Override
  public String processSpecificationPost(final IPostParameters variableContext, final Locale locale, final Specification spec, final int connectionSequenceNumber) throws ManifoldCFException {

    final String seqPrefix = "s" + connectionSequenceNumber + "_";

    // Categories
    String x;
    x = variableContext.getParameter(seqPrefix + "category_count");
    if (x != null && !x.isEmpty()) {
      // About to gather the category nodes, so get rid of the old ones.
      int i = 0;
      while (i < spec.getChildCount()) {
        final SpecificationNode node = spec.getChild(i);
        if (node.getType().equals(LlmConfig.NODE_CATEGORIES)) {
          spec.removeChild(i);
        } else {
          i++;
        }
      }

      final int count = Integer.parseInt(x);
      i = 0;
      while (i < count) {
        final String prefix = seqPrefix + "category_";
        final String suffix = "_" + i;
        final String op = variableContext.getParameter(prefix + "op" + suffix);
        if (op == null || !op.equals("Delete")) {
          // Gather the categories etc.
          final String category = variableContext.getParameter(prefix + LlmConfig.ATTRIBUTE_VALUE + suffix);
          final SpecificationNode node = new SpecificationNode(LlmConfig.NODE_CATEGORIES);
          node.setAttribute(LlmConfig.ATTRIBUTE_VALUE, category);
          spec.addChild(spec.getChildCount(), node);
        }
        i++;
      }

      final String addop = variableContext.getParameter(seqPrefix + "category_op");
      if (addop != null && addop.equals("Add")) {
        final String category = variableContext.getParameter(seqPrefix + "category_value");
        final SpecificationNode node = new SpecificationNode(LlmConfig.NODE_CATEGORIES);
        node.setAttribute(LlmConfig.ATTRIBUTE_VALUE, category);
        spec.addChild(spec.getChildCount(), node);
      }
    }

    addChildToSpec(variableContext, spec, seqPrefix + "enableSummarize", LlmConfig.NODE_ENABLE_SUMMARIZE);
    addChildToSpec(variableContext, spec, seqPrefix + "enableCategorize", LlmConfig.NODE_ENABLE_CATEGORIZE);
    addChildToSpec(variableContext, spec, seqPrefix + "enableEmbeddings", LlmConfig.NODE_ENABLE_EMBEDDINGS);
    addChildToSpec(variableContext, spec, seqPrefix + "maxTokens", LlmConfig.NODE_MAXTOKENS);
    addChildToSpec(variableContext, spec, seqPrefix + "summariesLanguage", LlmConfig.NODE_SUMMARIES_LANGUAGE);

    return null;
  }

  private static void addChildToSpec(IPostParameters variableContext, Specification spec, String fieldName, String nodeName) {
    final SpecificationNode node = new SpecificationNode(nodeName);
    final String value = variableContext.getParameter(fieldName);
    if (value != null) {
      node.setAttribute(LlmConfig.ATTRIBUTE_VALUE, value);
    } else {
      node.setAttribute(LlmConfig.ATTRIBUTE_VALUE, "");
    }
    spec.addChild(spec.getChildCount(), node);
  }


  /**
   * View specification. This method is called in the body section of a job's view page. Its purpose is to present the pipeline specification information to the user. The coder can presume that the
   * HTML that is output from this configuration will be within appropriate <html> and <body> tags.
   *
   * @param out                      is the output to which any HTML should be sent.
   * @param locale                   is the preferred local of the output.
   * @param connectionSequenceNumber is the unique number of this connection within the job.
   * @param spec                       is the current pipeline specification for this job.
   */
  @Override
  public void viewSpecification(final IHTTPOutput out, final Locale locale, final Specification spec, final int connectionSequenceNumber) throws ManifoldCFException, IOException {
    final Map<String, Object> paramMap = new HashMap<>();
    paramMap.put(SEQNUM, Integer.toString(connectionSequenceNumber));

    // Fill in the map with data from all tabs
    fillInLlmSpecificationMap(paramMap, spec);

    Messages.outputResourceWithVelocity(out, locale, VIEW_SPECIFICATION_HTML, paramMap);

  }

  protected static void fillInLlmSpecificationMap(final Map<String, Object> paramMap, final Specification os) {
    // Prep for field mappings
    String enableSummarize = "false";
    String enableCategorize = "false";
    String enableEmbeddings = "false";
    int maxTokens = 400;
    String summariesLanguage = "";

    // Default categories : Invoice, Call for Tenders, Request for Quotations, Technical paper, Presentation, Resumes
    List<String> categories = new ArrayList<>();
    if (os.getChildCount() == 0) {
      categories.add("Invoice");
      categories.add("Call for Tenders");
      categories.add("Request for Quotations");
      categories.add("Technical paper");
      categories.add("Presentation");
      categories.add("Resumes");
    }

    for (int i = 0; i < os.getChildCount(); i++) {
      final SpecificationNode sn = os.getChild(i);
      if (sn.getType().equals(LlmConfig.NODE_ENABLE_SUMMARIZE)) {
        enableSummarize = sn.getAttributeValue(LlmConfig.ATTRIBUTE_VALUE);
      } else if (sn.getType().equals(LlmConfig.NODE_ENABLE_CATEGORIZE)) {
        enableCategorize = sn.getAttributeValue(LlmConfig.ATTRIBUTE_VALUE);
      } else if (sn.getType().equals(LlmConfig.NODE_ENABLE_EMBEDDINGS)) {
        enableEmbeddings = sn.getAttributeValue(LlmConfig.ATTRIBUTE_VALUE);
      } else if (sn.getType().equals(LlmConfig.NODE_MAXTOKENS)) {
        try {
          maxTokens = Integer.parseInt(sn.getAttributeValue(LlmConfig.ATTRIBUTE_VALUE));
        } catch (NumberFormatException ex) {
          maxTokens = DEFAULT_MAXTOKENS;
        }
      } else if (sn.getType().equals(LlmConfig.NODE_SUMMARIES_LANGUAGE)) {
        summariesLanguage = sn.getAttributeValue(LlmConfig.ATTRIBUTE_VALUE);
      } else if (sn.getType().equals(LlmConfig.NODE_CATEGORIES)) {
        final String category = sn.getAttributeValue(LlmConfig.ATTRIBUTE_VALUE);
        if (category != null) {
          categories.add(category);
        }
      }
    }
    paramMap.put("ENABLESUMMARIZE", enableSummarize);
    paramMap.put("ENABLECATEGORIZE", enableCategorize);
    paramMap.put("ENABLEEMBEDDINGS", enableEmbeddings);
    paramMap.put("MAXTOKENS", maxTokens);
    paramMap.put("SUMMARIESLANGUAGE", summariesLanguage);
    paramMap.put("CATEGORIES", categories);
  }

  /**
   * Create a Version String for this connector configuration. To be used by getPipelineDescription().
   * 
   * @param spec the specification object associated with this connector.
   * @return the Version String
   */
  protected String getVersionString(Specification spec) {
    StringBuilder versionString = new StringBuilder();
    
    // Browse specification nodes and their attributes
    int nbNodes = spec.getChildCount();
    SpecificationNode specNode;
    Iterator<String> itAttributesName;
    String attributeValue;
    String attributeName;
    for (int i=0; i < nbNodes; i++) {
      specNode = spec.getChild(i);
      if (i > 0) {
        versionString.append('+');
      }
      versionString.append(specNode.getType());
      
      itAttributesName = specNode.getAttributes();
      while (itAttributesName.hasNext()) {
        attributeName = itAttributesName.next();
        attributeValue = specNode.getAttributeValue(attributeName);
        
        if (!attributeValue.isEmpty()) {
          versionString.append('+');
          versionString.append(attributeName);
          versionString.append(':');
          versionString.append(attributeValue);
        }
      }
    }
    return versionString.toString();
  }
  
}
