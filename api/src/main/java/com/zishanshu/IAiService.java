package com.zishanshu;


import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

public interface IAiService {
    String SYSTEM_PROMPT =
            """
            Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
            If unsure, simply state that you don't know.
            Another thing you need to note is that your reply must be in Chinese!
            DOCUMENTS:
                {documents}
            """;

    ChatResponse generate(String model,String message) ;
    Flux<ChatResponse> generateStream(String model, String message) ;
    Flux<ChatResponse> generateStreamRag(String model, String ragTag, String message);

}
