/*******************************************************************************
 *  * Copyright 2025 France Labs
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *  * Based on https://github.com/apache/solr/blob/releases/solr/9.9.0/solr/modules/llm/src/java/org/apache/solr/llm/textvectorisation/update/processor/TextToVectorUpdateProcessorFactory.java
 *******************************************************************************/

package com.francelabs.datafari.updateprocessor;

import java.io.IOException;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.languagemodels.textvectorisation.model.SolrTextToVectorModel;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;

class TextToVectorUpdateProcessor extends UpdateRequestProcessor {
  private static final Logger LOGGER = LogManager.getLogger(TextToVectorUpdateProcessor.class.getName());

    private IndexSchema schema;
    private final String inputField;
    private final String outputField;
    private final boolean forceEmbeddings;
    private final SolrTextToVectorModel textToVector;

    public TextToVectorUpdateProcessor(
            String inputField,
            String outputField,
            boolean forceEmbeddings,
            SolrTextToVectorModel textToVector,
            SolrQueryRequest req,
            UpdateRequestProcessor next) {
        super(next);
        this.forceEmbeddings = forceEmbeddings;
        this.schema = req.getSchema();
        this.inputField = inputField;
        this.outputField = outputField;
        this.textToVector = textToVector;
    }

    /**
     * @param cmd the update command in input containing the Document to process
     * @throws IOException If there is a low-level I/O error
     */
    @Override
    public void processAdd(AddUpdateCommand cmd) throws IOException {
        SolrInputDocument doc = cmd.getSolrInputDocument();
        SolrInputField inputFieldContent = getInputFieldContent(doc);

        Object vectorizeObj = doc.getFieldValue("embeddings_at_indexing");
        boolean embeddingsAtIndexing = (vectorizeObj != null) && Boolean.parseBoolean(vectorizeObj.toString());

        boolean vectorize = embeddingsAtIndexing || forceEmbeddings;

        // Only process embeddings if "vectorize" is true or if "/update/embed" handler is used
        if (!isNullOrEmpty(inputFieldContent) && vectorize) {
            if (textToVector == null) {
                LOGGER.error("Could not vectorise '{}': Model not found.",
                        doc.getFieldValue(schema.getUniqueKeyField().getName()));
                super.processAdd(cmd);
                return;
            }
            try {
                String textToVectorise;
                textToVectorise = inputFieldContent.getFirstValue().toString();

                float[] vector = textToVector.vectorise(textToVectorise);
                List<Float> vectorAsList = new ArrayList<>(vector.length);
                for (float f : vector) {
                    vectorAsList.add(f);
                }

                doc.setField(outputField, vectorAsList);

                // Retrieve the "has_vector" multivalued field
                Object hasVectorValue = doc.getFieldValue("has_vector");
                List<Object> hasVector = new ArrayList<>();
                if (hasVectorValue instanceof Collection<?>) {
                    hasVector.addAll((Collection<?>) hasVectorValue);
                } else if (hasVectorValue != null) {
                    hasVector.add(hasVectorValue);
                }

                // Add the name of the outputField in has_vector, if not already there
                if (!hasVector.contains(outputField)) {
                    doc.addField("has_vector", outputField);
                }

            } catch (Exception vectorisationFailure) {
                if (LOGGER.isErrorEnabled()) {
                    SchemaField uniqueKeyField = schema.getUniqueKeyField();
                    String uniqueKeyFieldName = uniqueKeyField.getName();
                    LOGGER.error(
                            "Could not vectorise: {} for the document with {}: {}",
                            inputField,
                            uniqueKeyFieldName,
                            doc.getFieldValue(uniqueKeyFieldName),
                            vectorisationFailure);
                }
            }
        }
        super.processAdd(cmd);
    }

  private SolrInputField getInputFieldContent(SolrInputDocument doc) {
    if ("embedded_content".equals(inputField)) {
        // Search for any content_* field
        SolrInputField inputFieldContentEn = doc.getField("content_en");
        SolrInputField inputFieldContentFr = doc.getField("content_fr");
        SolrInputField inputFieldContentEs = doc.getField("content_es");
        SolrInputField inputFieldContentDe = doc.getField("content_de");
        if (inputFieldContentEn != null) {
            return inputFieldContentEn;
        } else if (inputFieldContentFr != null) {
            return inputFieldContentFr;
        } else if (inputFieldContentEs != null) {
            return inputFieldContentEs;
        }   else if (inputFieldContentDe != null) {
            return inputFieldContentDe;
        }
    }

    return doc.getField(inputField);
  }

  protected boolean isNullOrEmpty(SolrInputField inputFieldContent) {
        return (inputFieldContent == null
                || inputFieldContent.getFirstValue() == null
                || inputFieldContent.getFirstValue().toString().isEmpty());
    }
}
