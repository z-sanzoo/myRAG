package com.zishanshu.config;

import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.OllamaEmbeddingClient;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiEmbeddingClient;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class OllamaConfig {

    @Bean
    @Qualifier("macbook")
    public OllamaApi ollamaApiMacbook(@Value("${spring.ai.ollama.base-url-macbook}") String baseUrl) {
        return new OllamaApi(baseUrl);
    }

    @Bean
    @Qualifier("4090")
    public OllamaApi ollamaApi4090(@Value("${spring.ai.ollama.base-url-4090}") String baseUrl) {
        return new OllamaApi(baseUrl);
    }


    @Bean
    public OpenAiApi openaiApi(@Value("${spring.ai.openai.base-url}") String baseUrl, @Value("${spring.ai.openai.api-key}") String apikey) {
        return new OpenAiApi(baseUrl, apikey);
    }



    @Bean
    @Qualifier("MacbookChatClient")
    public OllamaChatClient ollamaMacbookChatClient(@Qualifier("macbook") OllamaApi ollamaApi) {
        return new OllamaChatClient(ollamaApi);
    }


    @Bean
    @Qualifier("4090ChatClient")
    public OllamaChatClient ollama4090ChatClient(@Qualifier("4090") OllamaApi ollamaApi) {
        return new OllamaChatClient(ollamaApi);
    }


    @Bean
    public OpenAiChatClient openAiChatClient(OpenAiApi openAiApi) {return new OpenAiChatClient(openAiApi);}



    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }

//     下面这个函数是存储没有持久化的数据库,一旦程序断开rag的知识就会丢失
//    @Bean
//    public  SimpleVectorStore vectorStore(@Value("${spring.ai.rag.embed}") String model, OllamaApi ollamaApi) {
//        OllamaEmbeddingClient embeddingClient = new OllamaEmbeddingClient(ollamaApi);
//        embeddingClient.withDefaultOptions(OllamaOptions.create().withModel(model));
//        return new SimpleVectorStore(embeddingClient);
//    }

//    @Bean
//    @Qualifier("4090Embedding")
//    public OllamaEmbeddingClient ollamaMacbookEmbeddingClient( @Value("${spring.ai.rag.embed}") String model,
//                                                                          @Qualifier("4090") OllamaApi ollamaApi) {
//        OllamaEmbeddingClient embeddingClient = new OllamaEmbeddingClient(ollamaApi);
//        embeddingClient.withDefaultOptions(OllamaOptions.create().withModel(model));
//        return embeddingClient;
//    }
//    @Bean
//    public PgVectorStore pgVectorStore(
//            @Qualifier("4090Embedding") OllamaEmbeddingClient embeddingClient,
//            JdbcTemplate jdbcTemplate
//    ) {
//        return new PgVectorStore(jdbcTemplate, embeddingClient);
//    }


    @Bean
    public PgVectorStore pgVectorStore(@Value("${spring.ai.rag.embed}") String model, @Qualifier("4090") OllamaApi ollamaApi, OpenAiApi openAiApi, JdbcTemplate jdbcTemplate) {
        if ("nomic-embed-text".equalsIgnoreCase(model)) {
            OllamaEmbeddingClient embeddingClient = new OllamaEmbeddingClient(ollamaApi);
            embeddingClient.withDefaultOptions(OllamaOptions.create().withModel("nomic-embed-text"));
            return new PgVectorStore(jdbcTemplate, embeddingClient);
        } else {
            OpenAiEmbeddingClient embeddingClient = new OpenAiEmbeddingClient(openAiApi);
            return new PgVectorStore(jdbcTemplate, embeddingClient);
        }
    }


}
