package com.zishanshu.http;

import com.zishanshu.IRAGService;
import com.zishanshu.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("api/v1/rag")
public class RAGController implements IRAGService {

    @Resource
    private TokenTextSplitter tokenTextSplitter;

    @Resource
    private PgVectorStore pgVectorStore;

    @Resource
    private OpenAiChatClient openAiChatClient;

    @Resource
    private RedissonClient redisson;

    @RequestMapping(value = "query_rag_tag_list", method = RequestMethod.GET)
    @Override
    public Response<List<String>> queryRagTagList() {
        RList<String> elements = redisson.getList("ragTag");
        return Response.<List<String>>builder().code(200).message("调用成功").data(elements).build();

    }

    @RequestMapping(value = "file/upload", method = RequestMethod.POST, headers = "content-type=multipart/form-data")
    @Override
    public Response<String> uploadFile(String ragTag, List<MultipartFile> files) {
        log.info("上传知识库开始{}",ragTag);
        for(MultipartFile file:files){
            TikaDocumentReader reader = new TikaDocumentReader(file.getResource());
            List<Document> documents = reader.get();
            List<Document> documentSplitterList = tokenTextSplitter.apply(documents);
            documents.forEach(document -> {document.getMetadata().put("ragTag", ragTag);});
            documentSplitterList.forEach(document -> {document.getMetadata().put("ragTag", ragTag);});
            pgVectorStore.accept(documentSplitterList);//上传数据库

            RList<String> elements = redisson.getList("ragTag");
            if(!elements.contains(ragTag)){//如果知识库已经有就不做添加
                elements.add(ragTag);
            }
        }
        log.info("上传知识库完成{}",ragTag);

        return Response.<String>builder()
                .code(200)
                .message("上传知识库完成")
                .data(ragTag)
                .build();
    }
}
