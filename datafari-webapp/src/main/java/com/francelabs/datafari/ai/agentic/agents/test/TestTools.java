package com.francelabs.datafari.ai.agentic.agents.test;

import com.francelabs.datafari.ai.agentic.agents.common.HumanInputKind;
import com.francelabs.datafari.ai.agentic.agents.common.HumanInputService;
import com.francelabs.datafari.ai.agentic.agents.common.HumanInputType;
import com.francelabs.datafari.ai.agentic.tools.AgenticToolException;
import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.ai.dto.AiRequest;
import com.francelabs.datafari.ai.dto.ApiContent;
import com.francelabs.datafari.ai.services.RagService;
import com.francelabs.datafari.ai.services.SummarizationService;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.ai.stream.ToolMeta;
import com.francelabs.datafari.utils.WebSocketHttpServletRequest;
import com.francelabs.datafari.utils.rag.SearchUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.invocation.InvocationContext;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;

public class TestTools {

    private static final Logger LOGGER = LogManager.getLogger(TestTools.class.getName());
    HttpServletRequest request;
    RagConfiguration config;
    private final SourcesAccumulator sourcesAcc;
    private final ChatStream stream;
    private final AiRequest params;

    public TestTools(HttpServletRequest request, AiRequest params, ChatStream stream, SourcesAccumulator sourcesAcc) {
        this.request = new WebSocketHttpServletRequest(request);
        this.stream = stream;
        this.sourcesAcc = sourcesAcc;
        this.params = params;
        config = RagConfiguration.getInstance();
    }

    private WebSocketHttpServletRequest newToolRequest() {
        if (request instanceof WebSocketHttpServletRequest wsRequest) {
            return wsRequest.copy();
        }
        return new WebSocketHttpServletRequest(request);
    }

    @ToolMeta(label = "Sending email...",
            i18nKey = "tool.sendEmail",
            icon = "mail",
            requiresHumanInput  = true,
            humanInputTitle  = "Confirm email sending",
            humanInputKind = HumanInputKind.TOOL_CONFIRMATION,
            humanInputType = HumanInputType.CONFIRMATION,
            humanInputMessage = "Do you want to send this email?",
            humanInputOptions = {"approved", "rejected"}
    )
    @Tool("Send an email.")
    String sendEmail(
            @P("destination (email address)") String destination,
            @P("title") String title,
            @P("message") String message,
            InvocationContext context
    ) {
        LOGGER.warn("AN EMAIL WAS SENT ! (NOT FOR REAL, JUST TESTING)");
        LOGGER.warn("title: {}", title);
        LOGGER.warn("message: {}", message);

        return "The message was successfully sent.";
    }

    @ToolMeta(
            label = "Export search...",
            i18nKey = "tool.exportSearch",
            icon = "search",
            requiresHumanInput = true,
            humanInputKind = HumanInputKind.TOOL_PARAMETER,
            humanInputType = HumanInputType.CHOICE,
            humanInputTitle = "Export format",
            humanInputMessage = "How do you want to export your files?",
            humanInputOptions = {"csv", "xlsx", "txt"}
    )
    @Tool("Export search results in a new file.")
    String exportSearch(
            @P("query") String query,
            InvocationContext context
    ) {
        String humanInputValue = context.invocationParameters().get("humanInputValue");
        LOGGER.warn("EXPORTING SEARCH RESULTS IN {}", humanInputValue);
        List<String> allowedExt = List.of("csv", "xlsx", "txt");

        if (allowedExt.contains(humanInputValue)) {
            return "The search was successfully exported in " + humanInputValue + ". The document is now beeing downloaded by the user.";
        }

        return "The export failed. Invalid file extension requested by user.";
    }

    @ToolMeta(
            label = "Testing personality...",
            i18nKey = "tool.personalityTest",
            icon = "person"
//            requiresHumanInput = true,
//            humanInputKind = HumanInputKind.TOOL_CONFIRMATION,
//            humanInputType = HumanInputType.CONFIRMATION,
//            humanInputTitle = "Personality test",
//            humanInputMessage = "Do you want to start the test?"
    )
    @Tool("Run a personality test for user.")
    String personalityTest(
            InvocationContext context
    ) {
        LOGGER.warn("Running personnality test");

        String nature = HumanInputService.ask(
                stream,
                HumanInputKind.USER_CLARIFICATION,
                HumanInputType.CHOICE,
                "Question 1",
                "How would you describe yourself?",
                List.of("normal", "mean", "nice"),
                Map.of("toolName", "personalityTest")
        );
        LOGGER.warn("nature={}", nature);
        String favoriteDrink = HumanInputService.ask(
                stream,
                HumanInputKind.USER_CLARIFICATION,
                HumanInputType.SECRET,
                "Question 2",
                "What is your favorite drink? (we won't tell anyone)",
                List.of(),
                Map.of("toolName", "personalityTest")
        );
        LOGGER.warn("favoriteDrink={}", favoriteDrink);
        String country = HumanInputService.ask(
                stream,
                HumanInputKind.USER_CLARIFICATION,
                HumanInputType.TEXT,
                "Question 3",
                "In which country do you live?",
                List.of(),
                Map.of("toolName", "personalityTest")
        );
        LOGGER.warn("country={}", country);
        String opinion = HumanInputService.ask(
                stream,
                HumanInputKind.USER_CLARIFICATION,
                HumanInputType.CONFIRMATION,
                "Question 4",
                "Do you like your country?",
                List.of(),
                Map.of("toolName", "personalityTest")
        );
        LOGGER.warn("opinion={}", opinion);

        String opinionStr = "approved".equals(opinion) ?
                "likes":"dislikes";

        return "Test results: User is a {{nature}} guy whose favorite drink is {{favoriteDrink}}. Also, he lives in {{country}} and he {{opinion}} this country."
                .replace("{{nature}}", nature) // nice/mean/normal
                .replace("{{favoriteDrink}}", favoriteDrink) // Hidden text
                .replace("{{country}}", country) // Free text
                .replace("{{opinion}}", opinionStr); // approved/rejected

    }

    @ToolMeta(
            label = "Asking user...",
            i18nKey = "tool.askUser",
            icon = "question"
    )
    @Tool("Ask the user for missing information when required to answer correctly.")
    String askUser(
            @P("Question to ask the user") String question,
            InvocationContext context
    ) {
        String answer = HumanInputService.ask(
                stream,
                HumanInputKind.USER_CLARIFICATION,
                HumanInputType.TEXT,
                "Additional information required",
                question,
                List.of(),
                Map.of("toolCallId", context.invocationId().toString())
        );

        return "The user answered: " + answer;
    }
}
