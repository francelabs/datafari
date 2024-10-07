package com.francelabs.datafari.transformation.llm.connectors;

import com.francelabs.datafari.transformation.llm.model.LlmSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import static org.junit.jupiter.api.Assertions.*;
class OpenAiLlmServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(OpenAiLlmService.class.getName());


    @org.junit.jupiter.api.Test
    void embeddings() {

        LlmSpecification spec = new LlmSpecification();
        spec.setEnableVectorEmbedding(true);
        LlmService service = new OpenAiLlmService(spec);
        try {
            float[] vector = service.embeddings("« Canard » est un terme générique qui désigne des oiseaux aquatiques ansériformes, au cou court, au large bec jaune aplati, aux très courtes pattes palmées et aux longues ailes pointues, domestiqués ou non");
            LOGGER.info(vector);
        } catch (Exception e) {
            LOGGER.error("Error in OpenAiLlmServiceTest.embeddings(): ", e);
        }
    }

    @org.junit.jupiter.api.Test
    void summarize() {
    }

    @org.junit.jupiter.api.Test
    void categorize() {
    }
}