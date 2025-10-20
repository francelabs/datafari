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
import java.lang.invoke.MethodHandles;
import java.util.*;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.llm.textvectorisation.model.SolrTextToVectorModel;
import org.apache.solr.llm.textvectorisation.store.rest.ManagedTextToVectorModelStore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TextToVectorUpdateProcessor extends UpdateRequestProcessor {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
        SolrInputField inputFieldContent = doc.getField(inputField);

        Object vectorizeObj = doc.getFieldValue("embeddingsAtIndexing");
        boolean vectorize = (vectorizeObj != null && vectorizeObj.toString().equals("true")) || forceEmbeddings;

        // Only process embeddings if "vectorize" is true or if "/update/embed" handler is used
        if (!isNullOrEmpty(inputFieldContent) && vectorize) {
            if (textToVector == null) {
                if (log.isErrorEnabled()) log.error(
                        "Could not vectorise '{}': Model not found.",
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
                if (log.isErrorEnabled()) {
                    SchemaField uniqueKeyField = schema.getUniqueKeyField();
                    String uniqueKeyFieldName = uniqueKeyField.getName();
                    log.error(
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

    protected boolean isNullOrEmpty(SolrInputField inputFieldContent) {
        return (inputFieldContent == null
                || inputFieldContent.getFirstValue() == null
                || inputFieldContent.getFirstValue().toString().isEmpty());
    }
}
