package com.zishanshu;


import com.alibaba.fastjson.JSON;
import com.zishanshu.domain.service.TestComputerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;

import org.springframework.ai.model.Media;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;


import java.io.IOException;
import java.util.*;

import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class OllamaTest {

    @Value("classpath:data/dog.png")
    private org.springframework.core.io.Resource imageResource;

    @Resource(name = "ollamaChatClient")
    private ChatClient ollamaChatClient;

    @Resource(name = "ollamaChatModel")
    OllamaChatModel ollamaChatModel;

    @Resource(name="ollamaPgVectorStore")
    private PgVectorStore pgVectorStore;

    @Resource
    private TokenTextSplitter tokenTextSplitter;

    @Resource
    private RedissonClient redisson;




    @Test
    public void test_model() {
        ChatOptions defaultOptions = ollamaChatModel.getDefaultOptions();
        System.out.println(defaultOptions.getModel());
    }
    @Test
    public void test_call() {
        ChatResponse response = ollamaChatModel.call(new Prompt("你好给我讲个故事"));

        log.info("测试结果(call):{}",response.getResults());
    }

    @Test
    public void test_stream() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Flux<ChatResponse> stream = ollamaChatModel.stream(new Prompt(
                "告诉我1+1等于多少"));

        stream.subscribe(
                chatResponse -> {
                    AssistantMessage output = chatResponse.getResult().getOutput();
                    log.info("测试结果(stream): {}", JSON.toJSONString(output));
                },
                Throwable::printStackTrace,
                () -> {
                    countDownLatch.countDown();
                    log.info("测试结果(stream): done!");
                }
        );

        countDownLatch.await();
    }
//    @Test
//    public void test_call_images() throws IOException {
//        // 构建请求信息
////        org.springframework.core.io.Resource resource = new ClassPathResource("data/dog.png");
//
//
//        UserMessage userMessage = new UserMessage("请描述这张图片的主要内容，并说明图中物品的可能用途。",
//                new Media(MimeType.valueOf(MimeTypeUtils.IMAGE_PNG_VALUE),));
//        ChatResponse response = ollamaChatModel.call(new Prompt(userMessage));
//
//        log.info("测试结果(images):{}", response.getResults());
//    }


    @Test
    public void upload() {
        TikaDocumentReader reader = new TikaDocumentReader("./data/file.txt");

        List<Document> documents = reader.get();
        List<Document> documentSplitterList = tokenTextSplitter.apply(documents);

        documents.forEach(doc -> doc.getMetadata().put("ragTag", "wdg"));
        documentSplitterList.forEach(doc -> doc.getMetadata().put("ragTag", "wdg"));

        pgVectorStore.accept(documentSplitterList);
        RList<String> list =  redisson.getList("ragTag");
        if(!list.contains("wdg")){//如果知识库已经有就不做添加
            list.add("wdg");
        }
        log.info("上传完成");
    }

    @Test
    public void chat() {
        String message = "王大瓜今年几岁";

        String SYSTEM_PROMPT = """
                Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
                If unsure, simply state that you don't know.
                Another thing you need to note is that your reply must be in Chinese!
                DOCUMENTS:
                    {documents}
                """;

        SearchRequest request = SearchRequest.builder()
                .query(message)
                .topK(5)
                .filterExpression("ragTag == 'wdg'")
                .build();

        List<Document> documents = pgVectorStore.similaritySearch(request);

        String documentsCollectors = documents.stream().map(Document::getText).collect(Collectors.joining());

        Message ragMessage = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentsCollectors));

        ArrayList<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(message));
        messages.add(ragMessage);

        ChatResponse chatResponse = ollamaChatModel.call(new Prompt(messages));

        log.info("测试结果:{}", chatResponse.getResults());
    }


    @Test
    public void client_test(){
        ChatResponse response = ollamaChatClient.prompt("你好,请你编一个中文名字出来").call().chatResponse();
        log.info("测试结果(client):{}",response.getResults());
        ChatResponse response2 = ollamaChatClient.prompt("你好,请你复述刚才的名字").call().chatResponse();
        log.info("测试结果(client):{}",response2.getResults());
    }
    @Test
    public void mcp_test(){// 花费50秒左右!
        ChatResponse response = ollamaChatClient.prompt("你可以提供什么工具").call().chatResponse();
        log.info("测试结果(client):{}",response.getResults());
    }
    // ollama好像不支持工具调用





}
