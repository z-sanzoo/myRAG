//package com.zishanshu;
//
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.io.FileUtils;
//import org.eclipse.jgit.api.Git;
//import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.openai.OpenAiChatClient;
//import org.springframework.ai.reader.tika.TikaDocumentReader;
//import org.springframework.ai.transformer.splitter.TokenTextSplitter;
//import org.springframework.ai.vectorstore.PgVectorStore;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.core.io.PathResource;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.*;
//import java.nio.file.attribute.BasicFileAttributes;
//import java.util.List;
//import java.util.concurrent.locks.Lock;
//import java.util.concurrent.locks.ReentrantLock;
//
//@Slf4j
//@RunWith(SpringRunner.class)
//@SpringBootTest
//public class GitTest {
//
//    // github token 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
//
//
//    @Resource
//    private TokenTextSplitter tokenTextSplitter;
//
//    @Resource
//    private PgVectorStore pgVectorStore;
//
//    @Autowired
//    private OpenAiChatClient openAiChatClient;
//
//    @Test
//    public void upload() throws Exception{
//        String repoURL = "https://github.com/z-sanzoo/TestRepository";
//        String username = "z-sanzoo";
//        String token = "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";
//
//        String localPath = "./cloned-repo";
//
//        FileUtils.deleteDirectory(new File(localPath));
//        Git git =  Git.cloneRepository()
//                .setURI(repoURL)
//                .setDirectory(new File(localPath))
//                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, token))
//                .call();
//        git.close();
//    }
//    @Test
//    public void test_file() throws Exception{
//        Files.walkFileTree(Paths.get("./cloned-repo"), new SimpleFileVisitor<Path>() {
//            @Override
//            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                log.info("visitFile: " + file.toString());
//                PathResource pathResource = new PathResource(file);
//                TikaDocumentReader reader = new TikaDocumentReader(pathResource);
//
//                List<Document> documents = reader.get();
//                List<Document> documentsSplitterList = tokenTextSplitter.apply(documents);
//
//                documents.forEach(document -> document.getMetadata().put("knowledge", "git-repository"));
//                documentsSplitterList.forEach(document -> document.getMetadata().put("knowledge", "git-repository"));
//
//                pgVectorStore.accept(documentsSplitterList);
//                return FileVisitResult.CONTINUE;
//            }
//        });
//
//    }
//
//
//
//}
