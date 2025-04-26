package com.zishanshu.http;

import com.zishanshu.IAiService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("api/v1/ollama")
public class OllamaController implements IAiService {

    @Resource
    @Qualifier("MacbookChatClient")
    private OllamaChatClient ollamaMacbookchatClient;

    @Resource
    @Qualifier("4090ChatClient")
    private OllamaChatClient ollama4090ChatClient;

    @Resource
    private PgVectorStore pgVectorStore;


    @RequestMapping(value = "generate",method = RequestMethod.GET)
    @Override
    public ChatResponse generate(@RequestParam String model,@RequestParam String message) {
        if(model.equals("deepseek-r1:1.5b")) {
            log.info("deepseek-r1:1.5b 被调用");
            return ollamaMacbookchatClient.call(new Prompt(message, OllamaOptions.create().withModel(model)));
        }
        else if(model.equals("deepseek-r1:7b")) {
            log.info("deepseek-r1:7b 被调用");
            return ollama4090ChatClient.call(new Prompt(message, OllamaOptions.create().withModel(model)));
        }
        return null;
    }



    @GetMapping(value = "generate_stream")
    @Override
    public Flux<ChatResponse> generateStream(@RequestParam String model,@RequestParam String message) {
        if(model.equals("deepseek-r1:1.5b")) {
            log.info("deepseek-r1:1.5b 被流式调用");
            return ollamaMacbookchatClient.stream(new Prompt(message, OllamaOptions.create().withModel(model)));
        }
        else if(model.equals("deepseek-r1:7b")) {
            log.info("deepseek-r1:7b 被流式调用");
            return ollama4090ChatClient.stream(new Prompt(message, OllamaOptions.create().withModel(model)));
        }
        return null;
    }

    @GetMapping(value = "generate_stream_rag")
    @Override
    public Flux<ChatResponse> generateStreamRag(@RequestParam  String model,@RequestParam String ragTag, @RequestParam  String message) {
        log.info("映射到: generateStreamRag: " + message);
        OllamaChatClient ollamaChatClient = null;
        if(model.equals("deepseek-r1:1.5b")) {
            ollamaChatClient =  ollamaMacbookchatClient;
        }
        else if(model.equals("deepseek-r1:7b")) {
            ollamaChatClient =  ollama4090ChatClient;
        }
        else{
            return null;
        }
        log.info(ragTag);

        SearchRequest vectoreQuest = SearchRequest
                .query(message)
                .withTopK(5)
                .withFilterExpression("ragTag == '" + ragTag + "'" );

        List<Document> documents = pgVectorStore.similaritySearch(vectoreQuest);

        String documentsCollectors = documents.stream().map(Document::getContent).collect(Collectors.joining());
//        log.info(documentsCollectors);

        Message ragMessage = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentsCollectors));

        ArrayList<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(message));
        messages.add(ragMessage);
        Prompt prompt = new Prompt(messages, OllamaOptions.create().withModel(model));
        return ollamaChatClient.stream(prompt);
    }
}
