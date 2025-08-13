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

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.llm.texttovector.model.SolrTextToVectorModel;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TextToVectorUpdateProcessor extends UpdateRequestProcessor {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private IndexSchema schema;
    private final String inputField;
    private final String outputField;
    private SolrTextToVectorModel textToVector;
    boolean isAtomicUpdate = false;

    public TextToVectorUpdateProcessor(
            String inputField,
            String outputField,
            SolrTextToVectorModel textToVector,
            SolrQueryRequest req,
            UpdateRequestProcessor next) {
        super(next);
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
        isAtomicUpdate = false;

        if (!isNullOrEmpty(inputFieldContent)) {
            try {
                String textToVectorise;
                textToVectorise = getTextToVectorise(inputFieldContent);

                float[] vector = textToVector.vectorise(textToVectorise);
                List<Float> vectorAsList = new ArrayList<>(vector.length);
                for (float f : vector) {
                    vectorAsList.add(f);
                }

                if (isAtomicUpdate) {
                    Map<String, List<Float>> vectorAsOperation = new HashMap<>();
                    vectorAsOperation.put("set",vectorAsList);
                    doc.addField(outputField, vectorAsOperation);
                } else {
                    doc.addField(outputField, vectorAsList);
                }
            } catch (RuntimeException vectorisationFailure) {
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

    /**
     * @param inputFieldContent The Solr Input Field
     * @return The String text to vectorise
     */
    @Nullable
    private String getTextToVectorise(SolrInputField inputFieldContent) {
        String textToVectorise;
        Object inputFieldValue = inputFieldContent.getFirstValue();
        if (inputFieldValue instanceof Map) {
            isAtomicUpdate = true;
            // Atomic update is being used, extract the "set/add" value
            Map<?, ?> map = (Map<?, ?>) inputFieldValue;
            Object setVal = map.get("set");
            Object addVal = map.get("add");

            if (setVal != null) {
                textToVectorise = setVal.toString();
            } else if (addVal != null) {
                textToVectorise = addVal.toString();
            } else {
                textToVectorise = null;
            }
        } else {
            textToVectorise = inputFieldContent.getFirstValue().toString();
        }
        return textToVectorise;
    }

    protected boolean isNullOrEmpty(SolrInputField inputFieldContent) {
        return (inputFieldContent == null
                || inputFieldContent.getFirstValue() == null
                || inputFieldContent.getFirstValue().toString().isEmpty());
    }
}