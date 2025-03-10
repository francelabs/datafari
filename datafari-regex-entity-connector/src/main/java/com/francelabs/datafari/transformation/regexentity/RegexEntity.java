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
package com.francelabs.datafari.transformation.regexentity;

import com.francelabs.datafari.transformation.regexentity.model.RegexEntitySpecification;
import com.francelabs.datafari.utils.DataEncoding;
import com.francelabs.datafari.utils.storage.DestinationStorage;
import org.apache.manifoldcf.agents.interfaces.IOutputAddActivity;
import org.apache.manifoldcf.agents.interfaces.IOutputCheckActivity;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.agents.system.Logging;
import org.apache.manifoldcf.agents.transformation.BaseTransformationConnector;
import org.apache.manifoldcf.core.interfaces.*;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Connector to extract entities using a regular expression from document content and put them in metadata.
 *
 */
public class RegexEntity extends BaseTransformationConnector {
  public static final String _rcsid = "@(#)$Id: "+ RegexEntity.class.getName() + " $";

  private static final String EDIT_SPECIFICATION_JS = "editSpecification.js";
  private static final String EDIT_SPECIFICATION_HTML = "editSpecification_RegexEntity.html";
  private static final String VIEW_SPECIFICATION_HTML = "viewSpecification.html";

  
  protected static final String ACTIVITY_REGEX = "RegexEntity";
  protected static final String CONTENT = "content";
  protected static final String SEQNUM = "SEQNUM";

  protected static final String[] activitiesList = new String[] { ACTIVITY_REGEX };
  

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
  public void outputConfigurationBody(IThreadContext threadContext, IHTTPOutput out, ConfigParams parameters,
      String tabName) throws ManifoldCFException, IOException {
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

    Specification spec = pipelineDescription.getSpecification();
    SpecificationNode specNodeRegex = spec.getChild(RegexEntityConfig.POS_NODE_REGEX);

    Iterator<String> itSpecifications = specNodeRegex.getAttributes();

    String sourceMetadata;
    boolean hasError = false;

    Map<String, Map<String, RegexEntitySpecification>> regexEntitySpecificationsMapBySource = new TreeMap<>();

    // Create a Map<String(Source), Map<String, RegexEntitySpecification>> with the SpecificationNodes
    regexSpecificationsExtraction(spec, itSpecifications, regexEntitySpecificationsMapBySource);

    final long startTime = System.currentTimeMillis();

    // Map of metadata associated to lines found : < metadata, Set<linesFound> >
    Map<String, List<String>> matchedMetadata = new HashMap<>();

    // Prepare storage for reading document content. A suitable storage depending on content size.
    DestinationStorage storage = DestinationStorage.getDestinationStorage(document.getBinaryLength(), getClass());
    try {
      Map<String, RegexEntitySpecification> regexEntitySpecificationsForContent = regexEntitySpecificationsMapBySource.get(CONTENT);
      // Reading file content
      if (regexEntitySpecificationsForContent != null && regexEntitySpecificationsForContent.size() > 0) {
        try {

              // Transfert document content to the storage
              long binaryLength = document.getBinaryStream().transferTo(storage.getOutputStream());

              // The input stream of the document has been totally read by previous instruction, so set a new one
              document.setBinary(storage.getInputStream(), binaryLength);


              // Prepare reading of document copied to extract metadata.
              //--------------------------------------------------------
              // First detect the data encoding to read and extract data with the good encoding Charset.
              InputStream inputStream = storage.getInputStream();
              Charset charset = DataEncoding.detect(inputStream);
              // Prepare the Reader with the good encoding.
              InputStreamReader inputStreamReader = new InputStreamReader(inputStream, charset);
              BufferedReader buffRead = new BufferedReader(inputStreamReader);

              // Read lines
              String line = buffRead.readLine();
              while (line != null && regexEntitySpecificationsForContent.size() > 0) {
                searchMatchesForMetadata(line, regexEntitySpecificationsForContent, matchedMetadata);
                line = buffRead.readLine();
              }
              buffRead.close();

              checkNoMatchRegex(regexEntitySpecificationsForContent, matchedMetadata);

        } catch (Exception e) {
          hasError = true;
          activities.recordActivity(startTime, ACTIVITY_REGEX, document.getBinaryLength(), documentURI, "KO", e.getMessage());
          Logging.ingest.error("Unable to browse document " + documentURI, e);
        }
      }

      // Apply regex verification to source metadata
      if (regexEntitySpecificationsMapBySource.size() > 0) {

        for (Map.Entry<String, Map<String, RegexEntitySpecification>> entry : regexEntitySpecificationsMapBySource.entrySet()) {
          sourceMetadata = entry.getKey();
          if (CONTENT.equals(sourceMetadata)) continue;

          String[] sourceMetadataContent = getFieldValues(document, sourceMetadata);

          if (sourceMetadataContent.length > 0) {
            for (String line : sourceMetadataContent) {
              searchMatchesForMetadata(line, entry.getValue(), matchedMetadata);
            }

          } else if ("url".equals(sourceMetadata)) {
            searchMatchesForMetadata(documentURI, entry.getValue(), matchedMetadata);
          }
          checkNoMatchRegex(entry.getValue(), matchedMetadata);
        }

      }

      if (!hasError) activities.recordActivity(startTime, ACTIVITY_REGEX, document.getBinaryLength(), documentURI, "OK", "");
      addMetadataFieldsToDocument(document, matchedMetadata);
      return activities.sendDocument(documentURI, document);

    } finally {
      // Clean storage (for instance, delete temporary file) after all treatment on document done (Solr indexing).
      storage.close();
    }

  }

  private void regexSpecificationsExtraction(Specification spec, Iterator<String> itSpecifications, Map<String, Map<String, RegexEntitySpecification>> regexEntitySpecificationsMapBySource) {
    String regex;
    String keepOnlyOneString;
    RegexEntitySpecification regexEntitySpecification;
    String index;
    Map<String, RegexEntitySpecification> regexEntitySpecificationsMap;
    String sourceMetadata;
    String valueIfTrue;
    boolean keepOnlyOne;
    String extractRegexGroupsString;
    boolean extractRegexGroups;
    String valueIfFalse;
    String destinationMetadata;
    while (itSpecifications.hasNext()) {
      index = itSpecifications.next();
      sourceMetadata = readNodeValue(spec, RegexEntityConfig.POS_NODE_SOURCE_METADATA, index);
      regex = readNodeValue(spec, RegexEntityConfig.POS_NODE_REGEX, index);
      destinationMetadata = readNodeValue(spec, RegexEntityConfig.POS_NODE_DESTINATION_METADATA, index);
      valueIfTrue = readNodeValue(spec, RegexEntityConfig.POS_NODE_VALUE_IF_TRUE, index);
      valueIfFalse = readNodeValue(spec, RegexEntityConfig.POS_NODE_VALUE_IF_FALSE, index);
      keepOnlyOneString = readNodeValue(spec, RegexEntityConfig.POS_NODE_KEEP_ONLY_ONE, index);
      keepOnlyOne = Boolean.valueOf(keepOnlyOneString);
      extractRegexGroupsString = readNodeValue(spec, RegexEntityConfig.POS_NODE_EXTRACT_REGEX_GROUPS, index);
      extractRegexGroups = Boolean.valueOf(extractRegexGroupsString);

      regexEntitySpecification = new RegexEntitySpecification(sourceMetadata,
              regex, destinationMetadata, valueIfTrue,
              valueIfFalse, keepOnlyOne, extractRegexGroups);
      if (regexEntitySpecification.isTargetingContent()) {
        sourceMetadata = CONTENT;
      }
      if (regexEntitySpecification.isValidObject()) {
        regexEntitySpecificationsMap = regexEntitySpecificationsMapBySource.computeIfAbsent(sourceMetadata, k -> new TreeMap<>());
        regexEntitySpecificationsMap.put(index, regexEntitySpecification);
        regexEntitySpecificationsMapBySource.put(sourceMetadata, regexEntitySpecificationsMap);
      }
    }
  }


  /**
   * This method checks all Regex Specifications. If no matches were found and
   * if the valueIsFalse is specified, then the Destination Metadata is filled
   * with the "valueIfFalse"
   *
   * @param specificationMap the map of specifications to check
   * @param matchedMetadata the line is added to the Set of lines associated with its metadata. A new metadata is put in the map if not exists yet.
   */
  void checkNoMatchRegex(Map<String, RegexEntitySpecification> specificationMap, Map<String, List<String>> matchedMetadata) {
    String entryValueIfFalse;
    boolean hasMatch;
    List<String> noMatchesFound;

    for (Map.Entry<String, RegexEntitySpecification> entry : specificationMap.entrySet()) {
      entryValueIfFalse = entry.getValue().getValueIfFalse();
      hasMatch = entry.getValue().getHasMatch();
      if (entryValueIfFalse != null && !"".equals(entryValueIfFalse) && !hasMatch) {
        // If no match, then the valueIfFalse is used
        noMatchesFound = matchedMetadata.computeIfAbsent(entry.getValue().getDestinationMetadata(), k -> new ArrayList<>());
        noMatchesFound.add(entryValueIfFalse);
      }
    }
  }

  /**
   * This method replace the getFieldAsStrings of the MCF document to make sure it does not return a null object
   *
   * @param document  MCF document
   * @param fieldName Name of the document field to find
   * @return An array of strings that is empty if no value is found
   */
  private String[] getFieldValues(final RepositoryDocument document, final String fieldName) throws IOException {
    final String[] fieldValues = document.getFieldAsStrings(fieldName);
    return Objects.requireNonNullElseGet(fieldValues, () -> new String[0]);
  }

  /**
   * Search in the line for regular expression contained in the specification node. Each regular expression is associated with a metadata.
   * Fill in the Map of metadata associated.
   * 
   * @param line one line of the document being read.
   * @param regexConfigurationList a Map containing all specificaton for the regular expressions to find in the document or metadata.
   * @param matchedMetadata the line is added to the Set of lines associated with its metadata. A new metadata is put in the map if not exists yet.
   */
  private void searchMatchesForMetadata(final String line, final Map<String, RegexEntitySpecification> regexConfigurationList, Map<String, List<String>> matchedMetadata) {

    String metadata;
    String regex;
    List<String> matchesFound;
    Matcher matcher;
    String valueIfTrue;
    boolean keepOnlyOne;
    boolean extractRegexGroups;

    for (Map.Entry<String, RegexEntitySpecification> entry : regexConfigurationList.entrySet()) {
      metadata = entry.getValue().getDestinationMetadata();
      regex = entry.getValue().getRegexValue();
      valueIfTrue = entry.getValue().getValueIfTrue();
      keepOnlyOne = entry.getValue().getKeepOnlyOne();
      extractRegexGroups = entry.getValue().getExtractRegexGroups();

      // If the regex specification has already encountered a match and is looking for a new one
      if (canRegexSpecificationBeIgnored(entry.getValue())) continue;

      matcher = Pattern.compile(regex).matcher(line);
      if (matcher.find()) {
        matchesFound = matchedMetadata.computeIfAbsent(metadata, k -> new ArrayList<>());
        entry.getValue().setHasMatch(true);

        matcher.reset();

        if (!"".equals(valueIfTrue) && valueIfTrue != null) {
          matchesFound.add(valueIfTrue);
        } else if (keepOnlyOne) {
          // Retrieve the first match if keepOnlyOne is set to true
          if (matcher.find()) {
            addMatchesFound(matchesFound, matcher, extractRegexGroups);
          }
        } else {
          // Retrieve all matches if keepOnlyOne is set to false
          while (matcher.find()) {
            addMatchesFound(matchesFound, matcher, extractRegexGroups);
          }
        }
      }
    }

    // Withdraw the matching regex from configuration
    regexConfigurationList.entrySet().removeIf(entry -> canRegexSpecificationBeIgnored(entry.getValue()));
  }

  /**
   * Adds the regex match in the list of matchesFound. Two options: add the complete match or only the captured groups.
   * That means that if you have this regex: <tag>(.*?)</tag> you can extract what is between <tag> and </tag>. Another example,
   * <tag>(.*?)between(.*?)</tag>, if the line was <tag>what is between the tags</tag>, the values added in matchesFound are
   * "what is " and " the tags".
   *
   * @param matchesFound the matches to be added to the metadata (Solr field at the end)
   * @param matcher the {@link Matcher} object which is the result of a Regex compile method: Pattern.compile(regex).matcher(String).
   * @param extractRegexGroups true if only the groups captured should be added.
   */
  private void addMatchesFound(List<String> matchesFound, Matcher matcher, boolean extractRegexGroups){
    if (!extractRegexGroups) {
      addMatchesFoundFormated(matchesFound, matcher.group());
    } else {
      int nbGroups = matcher.groupCount();
      for (int i=1; i <= nbGroups; i++){
        addMatchesFoundFormated(matchesFound, matcher.group(i));
      }
    }
  }

  private void addMatchesFoundFormated(List<String> matchesFound, String match){
    if (match != null){
      matchesFound.add(match.trim());
    }
  }
  /**
   * Check if a regex specification can be removed from the list.
   *
   * @param regexEntitySpecification the regegex specification
   *
   */
  private boolean canRegexSpecificationBeIgnored(RegexEntitySpecification regexEntitySpecification) {
    return regexEntitySpecification.getHasMatch()
            && ((!"".equals(regexEntitySpecification.getValueIfTrue()) && regexEntitySpecification.getValueIfTrue() != null)
            || regexEntitySpecification.getKeepOnlyOne());
  }
  
  /**
   * Add metadata to a document using Matched Metadata Map.
   * 
   * @param document the document fill with metadata
   * @param matchedMetadata Map of metadata associated with their matched lines (< metadata, Set<linesFound> >)
   * 
   * @throws ManifoldCFException ManifoldCF Exception
   */
  private void addMetadataFieldsToDocument(RepositoryDocument document, Map<String, List<String>> matchedMetadata) throws ManifoldCFException {
    List<String> linesFound;
    String metadata;
    for (Map.Entry<String, List<String>> entry : matchedMetadata.entrySet()) {
      metadata = entry.getKey();
      linesFound = entry.getValue();
      document.addField(metadata, linesFound.toArray(new String[0]));
    }
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

    tabsArray.add(Messages.getString(locale, "RegexEntity.TabName"));

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

    fillInVelocityContextParam(paramMap, spec);

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
    
    // Fill in Specification object used to propagate posted variables to the edit or view pages.
    // First create a Node
    SpecificationNode specNodeSourceMetadata = new SpecificationNode(RegexEntityConfig.NODE_SOURCE_METADATA);
    SpecificationNode specNodeRegex = new SpecificationNode(RegexEntityConfig.NODE_REGEX);
    SpecificationNode specNodeDestinationMetadata = new SpecificationNode(RegexEntityConfig.NODE_DESTINATION_METADATA);
    SpecificationNode specNodeValueIfTrue = new SpecificationNode(RegexEntityConfig.NODE_VALUE_IF_TRUE);
    SpecificationNode specNodeValueIfFalse = new SpecificationNode(RegexEntityConfig.NODE_VALUE_IF_FALSE);
    SpecificationNode specNodeKeepOnlyOne = new SpecificationNode(RegexEntityConfig.NODE_KEEP_ONLY_ONE);
    SpecificationNode specNodeExtractRegexGroups = new SpecificationNode(RegexEntityConfig.NODE_EXTRACT_REGEX_GROUPS);

    spec.addChild(RegexEntityConfig.POS_NODE_SOURCE_METADATA, specNodeSourceMetadata);
    spec.addChild(RegexEntityConfig.POS_NODE_REGEX, specNodeRegex);
    spec.addChild(RegexEntityConfig.POS_NODE_DESTINATION_METADATA, specNodeDestinationMetadata);
    spec.addChild(RegexEntityConfig.POS_NODE_VALUE_IF_TRUE, specNodeValueIfTrue);
    spec.addChild(RegexEntityConfig.POS_NODE_VALUE_IF_FALSE, specNodeValueIfFalse);
    spec.addChild(RegexEntityConfig.POS_NODE_KEEP_ONLY_ONE, specNodeKeepOnlyOne);
    spec.addChild(RegexEntityConfig.POS_NODE_EXTRACT_REGEX_GROUPS, specNodeExtractRegexGroups);

    String nbMetaRegexParam = variableContext.getParameter(seqPrefix + "metaRegex_count");
    if (nbMetaRegexParam != null &&!nbMetaRegexParam.isEmpty()) {
      final int nbMetaRegex = Integer.parseInt(nbMetaRegexParam);
      final String prefix = seqPrefix + "metaRegex_";
      final String keyPrefix = "metaRegex_";
      String operation;
      String count;
      String sourceMetadataField;
      String regexField;
      String destinationMetadataField;
      String valueIfTrueField;
      String valueIfFalseField;
      String keepOnlyOneField;
      String extractRegexGroups;

      for (int i = 0; i < nbMetaRegex; i++) {
        operation = variableContext.getParameter(prefix + "op" + i);
        if(!"Delete".equals(operation)) {
          // Gather metadata/regex couple
          destinationMetadataField = variableContext.getParameter(seqPrefix + RegexEntityConfig.ATTRIBUTE_DESTINATION_METADATA_FIELD + i);
          regexField = variableContext.getParameter(seqPrefix + RegexEntityConfig.ATTRIBUTE_REGEX_FIELD + i);
          sourceMetadataField = variableContext.getParameter(seqPrefix + RegexEntityConfig.ATTRIBUTE_SOURCE_METADATA_FIELD + i);
          valueIfTrueField = variableContext.getParameter(seqPrefix + RegexEntityConfig.ATTRIBUTE_VALUE_IF_TRUE_FIELD + i);
          valueIfFalseField = variableContext.getParameter(seqPrefix + RegexEntityConfig.ATTRIBUTE_VALUE_IF_FALSE_FIELD + i);
          keepOnlyOneField = variableContext.getParameter(seqPrefix + RegexEntityConfig.ATTRIBUTE_KEEP_ONLY_ONE_FIELD + i);
          extractRegexGroups = variableContext.getParameter(seqPrefix + RegexEntityConfig.ATTRIBUTE_EXTRACT_REGEX_GROUPS_FIELD + i);

          specNodeSourceMetadata.setAttribute(keyPrefix + i, sourceMetadataField);
          specNodeRegex.setAttribute(keyPrefix + i, regexField);
          specNodeDestinationMetadata.setAttribute(keyPrefix + i, destinationMetadataField);
          specNodeValueIfTrue.setAttribute(keyPrefix + i, valueIfTrueField);
          specNodeValueIfFalse.setAttribute(keyPrefix + i, valueIfFalseField);
          specNodeKeepOnlyOne.setAttribute(keyPrefix + i, keepOnlyOneField);
          specNodeExtractRegexGroups.setAttribute(keyPrefix + i, extractRegexGroups);
        }
      }
      
      operation = variableContext.getParameter(prefix + "op");
      count = variableContext.getParameter(prefix + "count");
      if (operation != null && operation.equals("Add")) {
        destinationMetadataField = variableContext.getParameter(seqPrefix + RegexEntityConfig.ATTRIBUTE_DESTINATION_METADATA_FIELD);
        regexField = variableContext.getParameter(seqPrefix + RegexEntityConfig.ATTRIBUTE_REGEX_FIELD);
        sourceMetadataField = variableContext.getParameter(seqPrefix + RegexEntityConfig.ATTRIBUTE_SOURCE_METADATA_FIELD);
        valueIfTrueField = variableContext.getParameter(seqPrefix + RegexEntityConfig.ATTRIBUTE_VALUE_IF_TRUE_FIELD);
        valueIfFalseField = variableContext.getParameter(seqPrefix + RegexEntityConfig.ATTRIBUTE_VALUE_IF_FALSE_FIELD);
        keepOnlyOneField = variableContext.getParameter(seqPrefix + RegexEntityConfig.ATTRIBUTE_KEEP_ONLY_ONE_FIELD);
        extractRegexGroups = variableContext.getParameter(seqPrefix + RegexEntityConfig.ATTRIBUTE_EXTRACT_REGEX_GROUPS_FIELD);

        specNodeSourceMetadata.setAttribute(keyPrefix + count, sourceMetadataField);
        specNodeRegex.setAttribute(keyPrefix + count, regexField);
        specNodeDestinationMetadata.setAttribute(keyPrefix + count, destinationMetadataField);
        specNodeValueIfTrue.setAttribute(keyPrefix + count, valueIfTrueField);
        specNodeValueIfFalse.setAttribute(keyPrefix + count, valueIfFalseField);
        specNodeKeepOnlyOne.setAttribute(keyPrefix + count, keepOnlyOneField);
        specNodeExtractRegexGroups.setAttribute(keyPrefix + count, extractRegexGroups);
      }

    }
    

    return null;
    
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

    fillInVelocityContextParam(paramMap, spec);

    Messages.outputResourceWithVelocity(out, locale, VIEW_SPECIFICATION_HTML, paramMap);

  }

  /**
   * Fill in parameters to be display in Connector View or filled in edit formular. 
   * 
   * @param paramMap the map request parameters used in html pages.
   * @param spec the Specification Object filled with configuration attributes for job running with this connector. 
   */
  protected void fillInVelocityContextParam(final Map<String, Object> paramMap, final Specification spec) {
    String destinationMetadata;
    String regex;
    String sourceMetadata;
    String valueIfTrue;
    String valueIfFalse;
    String keepOnlyOneString;
    boolean keepOnlyOne;
    String extractRegexGroupsString;
    boolean extractRegexGroups;
    String index;
    RegexEntitySpecification regexEntitySpecification;

    if (spec.getChildCount() == 0){
      return;
    }

    final SpecificationNode specNode = spec.getChild(1);
    Iterator<String> itMetadata = specNode.getAttributes();

    Map<String, RegexEntitySpecification> metadataSourceMetadataAttribute = new TreeMap<>();

    while (itMetadata.hasNext()) {
      index = itMetadata.next();
      sourceMetadata = readNodeValue(spec, RegexEntityConfig.POS_NODE_SOURCE_METADATA, index);
      regex = readNodeValue(spec, RegexEntityConfig.POS_NODE_REGEX, index);
      destinationMetadata = readNodeValue(spec, RegexEntityConfig.POS_NODE_DESTINATION_METADATA, index);
      valueIfTrue = readNodeValue(spec, RegexEntityConfig.POS_NODE_VALUE_IF_TRUE, index);
      valueIfFalse = readNodeValue(spec, RegexEntityConfig.POS_NODE_VALUE_IF_FALSE, index);
      keepOnlyOneString = readNodeValue(spec, RegexEntityConfig.POS_NODE_KEEP_ONLY_ONE, index);
      keepOnlyOne = Boolean.valueOf(keepOnlyOneString);
      extractRegexGroupsString = readNodeValue(spec, RegexEntityConfig.POS_NODE_EXTRACT_REGEX_GROUPS, index);
      extractRegexGroups = Boolean.valueOf(extractRegexGroupsString);

      regexEntitySpecification = new RegexEntitySpecification(sourceMetadata,
              regex, destinationMetadata, valueIfTrue,
              valueIfFalse, keepOnlyOne, extractRegexGroups);

      metadataSourceMetadataAttribute.put(index, regexEntitySpecification);
    }

    
    paramMap.put(RegexEntityConfig.ATTRIBUTE_SPECIFICATION_MAP, metadataSourceMetadataAttribute);
  }


  /**
   * Read a specifictionNode and returns the value if it exists
   *
   * @param spec the specification object associated with this connector.
   * @param posNodeRegex the position of the node to read.
   * @param index the position of the attribute to get from the node.
   *
   * @return the attribute value
   */
  private String readNodeValue(Specification spec, int posNodeRegex, String index) {
    SpecificationNode specNode = spec.getChild(posNodeRegex);
    return (specNode != null && specNode.getAttributes() != null && specNode.getAttributeValue(index) != null) ? specNode.getAttributeValue(index) : "";
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
