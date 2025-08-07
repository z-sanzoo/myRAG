package com.zishanshu;


import com.alibaba.fastjson.JSON;
import com.zishanshu.domain.service.TestComputerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class OpenaiTest {


    @Resource(name = "openaiChatClient")
    private ChatClient openaiChatClient;

    @Resource
    OpenAiChatModel openaiChatModel;

    @Resource(name="ollamaPgVectorStore")
    private PgVectorStore pgVectorStore;

    @Resource
    private TokenTextSplitter tokenTextSplitter;

    @Resource
    private RedissonClient redisson;



    @Test
    public void test_model() {
        ChatOptions defaultOptions = openaiChatModel.getDefaultOptions();
        System.out.println(defaultOptions.getModel());
    }
    @Test
    public void test_call() {
        ChatResponse response = openaiChatModel.call(new Prompt("你好给我讲个故事"));

        log.info("测试结果(call):{}",response.getResults());
    }

    @Test
    public void test_stream() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Flux<ChatResponse> stream = openaiChatModel.stream(new Prompt(
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

        ChatResponse chatResponse = openaiChatModel.call(new Prompt(messages));

        log.info("测试结果:{}", chatResponse.getResults());
    }


    @Test
    public void memory_test(){
        ChatResponse response = openaiChatClient.prompt("你好,请你编一个中文名字出来").call().chatResponse();
        log.info("测试结果(client):{}",response.getResults());
        ChatResponse response2 = openaiChatClient.prompt("你好,请你复述刚才的名字").call().chatResponse();
        log.info("测试结果(client):{}",response2.getResults());
    }


    @Test
    public void mcp_test(){// 花费50秒左右!
        ChatResponse response = openaiChatClient.prompt("帮我我电脑的配置写在一个名为111.txt的文件中").call().chatResponse();
        log.info("测试结果(client):{}",response.getResults());
    }

//    @Test
//    public  void mcp_test2(){
//        ChatResponse response = openaiChatClient.prompt("帮我我电脑的配置写在一个名为222.txt的文件中").tools(new TestComputerService()).call().chatResponse();
//        log.info("测试结果(client):{}",response.getResults());
//    }

}
