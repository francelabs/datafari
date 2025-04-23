package com.francelabs.datafari.transformation.binary.services;

import com.francelabs.datafari.transformation.binary.model.BinarySpecification;
import dev.ai4j.openai4j.OpenAiHttpException;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;

import java.time.Duration;

public class DatakeenExternalService extends ExternalService implements IExternalService {

    private static final Logger LOGGER = LogManager.getLogger(DatakeenExternalService.class.getName());

    public DatakeenExternalService(BinarySpecification spec) {
        super(spec);
    }

    public String invoke(String prompt) throws ManifoldCFException {
        return "";
    }
}