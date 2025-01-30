package com.francelabs.datafari.transformation.llm.services;

import com.francelabs.datafari.transformation.llm.model.LlmSpecification;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;

class OpenAiLlmServiceTest {

    @org.junit.jupiter.api.Test
    void embeddings() {
    }

    @org.junit.jupiter.api.Test
    void summarize() throws IOException {

        LlmSpecification spec = new LlmSpecification();
        spec.setEnableVectorEmbedding(true);
        String apikey = "sk-xxxxxxxxxxxxxxxxxxxxxxx";
        spec.setApiKey(apikey);
        spec.setLlmEndpoint("https://api.openai.com/v1/");
        LlmService service = new OpenAiLlmService(spec);
        Metadata metadata = new Metadata().put("filename", "canards.pdf");
        TextSegment chunk = new TextSegment ("« Canard » est un terme générique qui désigne des oiseaux aquatiques ansériformes, au cou court, au large bec jaune aplati, aux très courtes pattes palmées et aux longues ailes pointues, domestiqués ou non", metadata);

        String summary = service.summarize(chunk, spec);

    }

    @org.junit.jupiter.api.Test
    void categorize() {
    }
}