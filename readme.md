## v1版本
用springboot构建了一个基本的应用
一个简单的入口类
```java
@SpringBootApplication
@Configuration
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

做了一个简单的调用ollama的Controller,这个controller实现了AI的服务接口,主要是有非流式服务和流式服务两种:
chatClient.call:
* 返回类型是 ChatResponse
* 同步执行，等待完整响应后一次性返回
* 适用于短文本生成场景
* 客户端需要等待全部内容生成完才能看到响应
chatClient.stream:
返回类型是 Flux<ChatResponse>
* 异步执行，使用响应式流处理
* 适用于长文本生成场景
* 客户端可以实时看到生成的内容（类似 ChatGPT 的打字效果）
* 需要客户端支持流式处理（如 Server-Sent Events）


```java
@RestController
@CrossOrigin("*")
@RequestMapping("api/v1/ollama")
public class OllamaController implements IAiService {

    @Resource
    private OllamaChatClient chatClient;


    @RequestMapping(value = "generate", method = RequestMethod.GET)
    @Override
    public ChatResponse generate(@RequestParam String model,@RequestParam String message) {
        return chatClient.call(new Prompt(message, OllamaOptions.create().withModel(model)));
    }

    @RequestMapping(value = "generate_stream", method = RequestMethod.GET)
    @Override
    public Flux<ChatResponse> generateStream(String model, String message) {
        return chatClient.stream(new Prompt(message, OllamaOptions.create().withModel(model)));
    }
}
```
其中OllamaChatClient的注入是通过在configuration

```java
@Configuration
public class OllamaConfig {

    @Bean
    public OllamaApi ollamaApi(@Value("${spring.ai.ollama.base-url}") String baseUrl) {
        return new OllamaApi(baseUrl);
    }
    
    @Bean
    public OllamaChatClient ollamaChatClient(OllamaApi ollamaApi) {
        return new OllamaChatClient(ollamaApi);
    }

}

```
可以通过http请求的方式调用ollama的服务
```bash
curl -X GET "http://127.0.0.1:8090/api/v1/ollama/generate?model=deepseek-r1:1.5b&message=hello"
```
流式调用
```bash
curl -N -X GET "http://127.0.0.1:8090/api/v1/ollama/generate_stream?model=deepseek-r1:1.5b&message=hello"
```

配置maven的profile的时候可以指定jvm参数
```xml
<profile>
<id>dev</id>
<activation>
<activeByDefault>true</activeByDefault>
</activation>
<properties>
<java_jvm>-Xms1G -Xmx1G -server  -XX:MaxPermSize=256M -Xss256K -Dspring.profiles.active=test -XX:+DisableExplicitGC -XX:+UseG1GC  -XX:LargePageSizeInBytes=128m -XX:+UseFastAccessorMethods -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/export/Logs/xfg-frame-archetype-lite-boot -Xloggc:/export/Logs/xfg-frame-archetype-lite-boot/gc-xfg-frame-archetype-lite-boot.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps</java_jvm>
<profileActive>dev</profileActive>
</properties>
</profile>
```

内存配置：
-Xms1G: 初始堆内存大小为 1GB
-Xmx1G: 最大堆内存大小为 1GB
-XX:MaxPermSize=256M: 永久代最大大小为 256MB（注意：JDK8 后被 Metaspace 替代）
-Xss256K: 每个线程的栈大小为 256KB



## v2版本 
生成前端,通过AI生成前端页面
首先整理问题:

根据以下信息编写,HTML的UI以对接服务端的的接口

我们实现了流式的GET请求接口
@RestController
@CrossOrigin("*")
@RequestMapping("api/v1/ollama")
public class OllamaController implements IAiService {
    @Resource
    private OllamaChatClient chatClient;
    @RequestMapping(value = "generate", method = RequestMethod.GET)
    @Override
    public ChatResponse generate(@RequestParam String model,@RequestParam String message) {
        return chatClient.call(new Prompt(message, OllamaOptions.create().withModel(model)));
    }
    @RequestMapping(value = "generate_stream", method = RequestMethod.GET)
    @Override
    public Flux<ChatResponse> generateStream(String model, String message) {
        return chatClient.stream(new Prompt(message, OllamaOptions.create().withModel(model)));
    }
}
通过 GET  http://127.0.0.1:8090/api/v1/ollama/generate_stream?model=deepseek-r1:1.5b&message=hello
我们在前端获得了流式应答:
[
{
"result": {
"output": {
"messageType": "ASSISTANT",
"properties": {
"id": "chatcmpl-B3HPw95SsqmhoWeJ8azGLxK1Vf4At",
"role": "ASSISTANT",
"finishReason": ""
},
"content": "1",
"media": []
},
"metadata": {
"finishReason": null,
"contentFilterMetadata": null
}
}
},
{
"result": {
"output": {
"messageType": "ASSISTANT",
"properties": {
"id": "chatcmpl-B3HPw95SsqmhoWeJ8azGLxK1Vf4At",
"role": "ASSISTANT",
"finishReason": ""
},
"content": " +",
"media": []
},
"metadata": {
"finishReason": null,
"contentFilterMetadata": null
}
}
},
{
"result": {
"output": {
"messageType": "ASSISTANT",
"properties": {
"id": "chatcmpl-B3HPw95SsqmhoWeJ8azGLxK1Vf4At",
"role": "ASSISTANT",
"finishReason": ""
},
"content": " ",
"media": []
},
"metadata": {
"finishReason": null,
"contentFilterMetadata": null
}
}
},
{
"result": {
"output": {
"messageType": "ASSISTANT",
"properties": {
"id": "chatcmpl-B3HPw95SsqmhoWeJ8azGLxK1Vf4At",
"role": "ASSISTANT",
"finishReason": ""
},
"content": "1",
"media": []
},
"metadata": {
"finishReason": null,
"contentFilterMetadata": null
}
}
},
{
"result": {
"output": {
"messageType": "ASSISTANT",
"properties": {
"id": "chatcmpl-B3HPw95SsqmhoWeJ8azGLxK1Vf4At",
"role": "ASSISTANT",
"finishReason": ""
},
"content": " equals",
"media": []
},
"metadata": {
"finishReason": null,
"contentFilterMetadata": null
}
}
},
{
"result": {
"output": {
"messageType": "ASSISTANT",
"properties": {
"id": "chatcmpl-B3HPw95SsqmhoWeJ8azGLxK1Vf4At",
"role": "ASSISTANT",
"finishReason": ""
},
"content": "2",
"media": []
},
"metadata": {
"finishReason": null,
"contentFilterMetadata": null
}
}
},
{
"result": {
"output": {
"messageType": "ASSISTANT",
"properties": {
"id": "chatcmpl-B3HPw95SsqmhoWeJ8azGLxK1Vf4At",
"role": "ASSISTANT",
"finishReason": "STOP"
},
"content": null,
"media": []
},
"metadata": {
"finishReason": "STOP",
"contentFilterMetadata": null
}
}
}
]
根据上述的说明,帮我编写一款简单的AI对话页面
1. 输入内容,点击发送按钮,调用业务流失请求,前端渲染页面
2. 以html,js代码实现,css样式采用tailwind来编写
3. 通过 const eventSource = new EventSource(ApiUrl),调用api接口
4. 从result.output.content获取应答的文本展示,注意content可能为空
5. 从result.metadata.finishReason获取应答的结束标志,如果是STOP,则停止请求
6. 整体样式要求美观
结果效果也不好有bug直接用小福哥的了

## V3版本

