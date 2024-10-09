package com.francelabs.datafari.transformation.llm.connectors;

import com.francelabs.datafari.transformation.llm.model.LlmSpecification;
import org.junit.jupiter.api.Assertions;


import static org.junit.jupiter.api.Assertions.*;
class OpenAiLlmServiceTest {

    @org.junit.jupiter.api.Test
    void embeddings() {

        LlmSpecification spec = new LlmSpecification();
        spec.setEnableVectorEmbedding(true);
        String apikey = "sk-xxxxxxxxxxxxxxxxxxxxxxx";
        spec.setApiKey(apikey);
        spec.setLlmEndpoint("https://api.openai.com/v1/");
        spec.setEmbeddingsModel("text-embedding-3-small");
        spec.setVectorDimension(124);
        LlmService service = new OpenAiLlmService(spec);

        try {
            float[] vector = service.embeddings("« Canard » est un terme générique qui désigne des oiseaux aquatiques ansériformes, au cou court, au large bec jaune aplati, aux très courtes pattes palmées et aux longues ailes pointues, domestiqués ou non");
            System.out.println(vector);
            Assertions.assertTrue(vector.length > 100);
        } catch (Exception e) {
            System.out.println("Error in OpenAiLlmServiceTest.embeddings()" + e.getLocalizedMessage());
        }
    }

    @org.junit.jupiter.api.Test
    void summarize() {
    }

    @org.junit.jupiter.api.Test
    void categorize() {
    }
}