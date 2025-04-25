package com.zishanshu;


import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.ChatOptionsBuilder;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


import java.util.*;

import java.util.stream.Collectors;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class RAGTest {

    @Resource
    private TokenTextSplitter tokenTextSplitter;

    @Resource
    private PgVectorStore pgVectorStore;

    @Autowired
    private OpenAiChatClient openAiChatClient;

    @Test
    public void upload() {
        // TikaDocumentReader可以读一个文件夹但是读到的好像就是文件夹下文件的名字
        TikaDocumentReader reader = new TikaDocumentReader("./data/lkx.txt");
        List<Document> documents = reader.get();
//        documents.forEach(document -> {System.out.println(document.getContent());});

        // 下面这个函数可以将长的documents分割成短的document, 但是如果一个文件本身比较短,则不会分割还是保持一个document的形式
        List<Document> documentsSplitterList = tokenTextSplitter.apply(documents);
//        documentsSplitterList.forEach(document -> {System.out.println(document.getContent()+" 分隔");});

        // 下面是初始化 每个doucument的元数据
//        documents.forEach(document -> document.getMetadata().put("knowledge","知识库名称"));
        documentsSplitterList.forEach(document -> document.getMetadata().put("knowledge","知识库名称"));
        // 下面是把这个持久化到pg向量数据库中
        pgVectorStore.accept(documentsSplitterList);
        log.info("上传完成");


    }
    @Test
    public void chat(){
        String message = "告诉我骆可星的各种信息";

        String SYSTEM_PROMPT = """
                Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
                If unsure, simply state that you don't know.
                Another thing you need to note is that your reply must be in Chinese!
                DOCUMENTS:
                    {documents}
                """;


        SearchRequest request = SearchRequest.query(message).withTopK(5).withFilterExpression("knowledge == '知识库名称'");
//        System.out.println(request);

        List<Document> documents = pgVectorStore.similaritySearch(request);
//        documents.forEach(document -> {System.out.println("相似信息:"+document.getContent());});

        String documentsCollectors = documents.stream().map(Document::getContent).collect(Collectors.joining());
//        System.out.println(documentsCollectors);
        // Map.of是用来创建不可变map的,最多支持10个键值对
        org.springframework.ai.chat.messages.Message ragMessage = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentsCollectors));

        ArrayList<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(message));
        messages.add(ragMessage);
        Prompt prompt = new Prompt(messages, OpenAiChatOptions.builder().withModel("deepseek-chat").build());
        ChatResponse chatResponse = openAiChatClient.call(prompt);

        log.info("测试结果:{}", JSON.toJSONString(chatResponse));
    }

}
