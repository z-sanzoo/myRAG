package com.zishanshu.http;

import com.zishanshu.IAiService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("api/v1/openai")
public class OpenaiController implements IAiService {

    @Resource
    private OpenAiChatClient chatClient;

    @Resource
    private PgVectorStore pgVectorStore;

    @GetMapping(value = "generate")
    @Override
    public ChatResponse generate(@RequestParam String message, @RequestParam String model) {
        return chatClient.call(new Prompt(message, OpenAiChatOptions.builder().withModel(model).build()));
    }

    @GetMapping(value = "generate_stream")
    @Override
    public Flux<ChatResponse> generateStream(@RequestParam String message, @RequestParam String model) {
        log.info("映射到: generateStream: " + message);
        return chatClient.stream(new Prompt(message,OpenAiChatOptions.builder().withModel(model).build()));
    }

    @GetMapping(value = "generate_stream_rag")
    @Override
    public Flux<ChatResponse> generateStreamRag(String model, String ragTag, String message) {
        SearchRequest vectoreQuest = SearchRequest
                .query(message)
                .withTopK(5)
                .withFilterExpression("ragTag == '" + ragTag + "'" );

        List<Document> documents = pgVectorStore.similaritySearch(vectoreQuest);

        String documentsCollectors = documents.stream().map(Document::getContent).collect(Collectors.joining());

        Message ragMessage = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentsCollectors));

        ArrayList<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(message));
        messages.add(ragMessage);
        Prompt prompt = new Prompt(messages, OpenAiChatOptions.builder().withModel(model).build());
        return chatClient.stream(prompt);
    }
}
