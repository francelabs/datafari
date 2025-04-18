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
package com.francelabs.datafari.transformation.binary;

import java.io.*;
import java.util.*;

import com.francelabs.datafari.transformation.binary.services.DatafariAiAgentExternalService;
import com.francelabs.datafari.transformation.binary.services.DatakeenExternalService;
import com.francelabs.datafari.transformation.binary.services.ExternalService;
import com.francelabs.datafari.transformation.binary.services.OpenAiExternalService;
import com.francelabs.datafari.transformation.binary.model.BinarySpecification;
import dev.langchain4j.data.segment.TextSegment;
import org.apache.commons.io.IOUtils;
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

import com.francelabs.datafari.transformation.binary.utils.storage.DestinationStorage;



/**
 * Connector to extract entities using a regular expression from document content and put them in metadata.
 *
 */
public class Binary extends BaseTransformationConnector {

  public static final String _rcsid = "@(#)$Id: "+ Binary.class.getName() + " $";

  private static final String EDIT_SPECIFICATION_JS = "editSpecification.js";
  private static final String EDIT_SPECIFICATION_HTML = "editSpecification.html";
  private static final String VIEW_SPECIFICATION_HTML = "viewSpecification.html";

  
  protected static final String ACTIVITY_BINARY = "BINARY";
  protected static final String SEQNUM = "SEQNUM";

  protected static final String[] activitiesList = new String[] { ACTIVITY_BINARY };
  private static final Logger LOGGER = LogManager.getLogger(Binary.class.getName());


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
    // TODO : filter by MimeType ?
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
    // TODO : specific filter here ?
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
    // TODO : filter by size here ?
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


    final BinarySpecification spec = new BinarySpecification(pipelineDescription.getSpecification(), getConfiguration());

    boolean hasError = false;
    final long startTime = System.currentTimeMillis();
    // Prepare storage for reading document content. A suitable storage depending on content size.
//    DestinationStorage storage = DestinationStorage.getDestinationStorage(document.getBinaryLength(), getClass());
//    StringBuilder contentBuilder = new StringBuilder();

    try {
/*
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
          activities.recordActivity(startTime, ACTIVITY_BINARY, document.getBinaryLength(), documentURI, "KO", e.getMessage());
          Logging.ingest.error("Unable to browse document " + documentURI, e);
      }*/

      InputStream binaryStream = document.getBinaryStream();
      byte[] imageBytes = IOUtils.toByteArray(binaryStream);
      String base64Image = Base64.getEncoder().encodeToString(imageBytes);



      // If content is empty, stop here
      if (base64Image.isBlank()) {
        return activities.sendDocument(documentURI, document);
      }

      // Select the proper service depending on the External API
      ExternalService service;
      // TODO : pick service
      /*switch (spec.getStringProperty(BinaryConfig.NODE_TYPE_OF_SERVICE)) {
        case "datafari":
          service = new DatafariAiAgentExternalService(spec);
          break;
        case "datakeen":
          service = new DatakeenExternalService(spec);
          break;
        case "openai":
        default:
          service = new OpenAiExternalService(spec);
          break;
      }*/
      service = new OpenAiExternalService(spec);

      // Get image description
      getImageDescription(documentURI, document, activities, service, base64Image, startTime);

/*
      // SUMMARIZE DOCUMENTS
      summarize(documentURI, document, activities, spec, service, chunks, startTime);


      // CATEGORIZE DOCUMENTS
      categorize(documentURI, document, activities, spec, service, chunks, startTime);*/


      if (!hasError) activities.recordActivity(startTime, ACTIVITY_BINARY, document.getBinaryLength(), documentURI, "OK", "");
      return activities.sendDocument(documentURI, document);

    } finally {
      // Clean storage (for instance, delete temporary file) after all treatment on document done (Solr indexing).
      //storage.close();
    }

  }

  private void getImageDescription(String documentURI, RepositoryDocument document, IOutputAddActivity activities, ExternalService service, String base64Image, long startTime) throws ManifoldCFException {
    // Get content for OpenAI
    if (isSupportedImageMimeType(document.getMimeType()) && !base64Image.isBlank()) {
      LOGGER.info("Image detected for document {}. Trying to generate a text content.", documentURI);
      try {
        String response = service.invoke(base64Image);
        if (response.isEmpty()) throw new RuntimeException("Image identification failed: " + documentURI);
        document.addField("embedded_content", response);
        LOGGER.info("EBE - Content generated : {}", response);
      } catch (ManifoldCFException e) {
        // If the error is a ManifoldCFException, the job should stop
        LOGGER.warn("Fatal error while processing {}: {}", documentURI, e.getLocalizedMessage());
        activities.recordActivity(startTime, ACTIVITY_BINARY, document.getBinaryLength(), documentURI, "ERROR", e.getLocalizedMessage());
        throw new ManifoldCFException("Error in Binary Connector.", e);
      } catch (Exception e) {
        LOGGER.warn("Error while processing {}: {}", documentURI, e.getLocalizedMessage());
        activities.recordActivity(startTime, ACTIVITY_BINARY, document.getBinaryLength(), documentURI, "WARNING", "Document could not be categorized");
      }
    }
  }

  private boolean isSupportedImageMimeType(String mimeType) {
    return mimeType != null && (
            mimeType.equalsIgnoreCase("image/jpeg") ||
                    mimeType.equalsIgnoreCase("image/png") ||
                    mimeType.equalsIgnoreCase("image/gif") ||
                    mimeType.equalsIgnoreCase("image/webp")
    );
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

    tabsArray.add(Messages.getString(locale, "binary.TabName"));

    // Fill in the specification header map, using data from all tabs.
    fillInSpecificationMap(paramMap, spec);

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
    fillInSpecificationMap(paramMap, spec);


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

    BinarySpecification binarySpecification = new BinarySpecification();
    Map<String, Object> params = binarySpecification.getSpec();
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      addChildToSpec(variableContext, spec, seqPrefix + entry.getKey(), entry.getKey());
    }

    return null;
  }

  private static void addChildToSpec(IPostParameters variableContext, Specification spec, String fieldName, String nodeName) {
    final SpecificationNode node = new SpecificationNode(nodeName);
    final String value = variableContext.getParameter(fieldName);
    if (value != null) {
      node.setAttribute(BinaryConfig.ATTRIBUTE_VALUE, value);
    } else {
      node.setAttribute(BinaryConfig.ATTRIBUTE_VALUE, "");
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
    fillInSpecificationMap(paramMap, spec);

    Messages.outputResourceWithVelocity(out, locale, VIEW_SPECIFICATION_HTML, paramMap);

  }

  protected static void fillInSpecificationMap(final Map<String, Object> paramMap, final Specification os) {
    // Prep for field mappings
    BinarySpecification binarySpecification = new BinarySpecification(os, new ConfigParams());

    for (Map.Entry<String, Object> entry : binarySpecification.getSpec().entrySet()) {
      paramMap.put(entry.getKey(), entry.getValue());
    }
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
