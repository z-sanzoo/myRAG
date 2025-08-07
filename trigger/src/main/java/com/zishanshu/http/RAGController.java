package com.zishanshu.http;

import com.zishanshu.IRAGService;
import com.zishanshu.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;

import org.springframework.core.io.PathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
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
    private JdbcTemplate jdbcTemplate;

    @Resource
    private RedissonClient redisson;

    @RequestMapping(value = "query_rag_tag_list", method = RequestMethod.GET)
    @Override
    public Response<List<String>> queryRagTagList() {
        log.info("收到请求 query_rag_tag_list");
        RList<String> elements = redisson.getList("ragTag");
        return Response.<List<String>>builder().code(200).message("调用成功").data(elements).build();

    }

    @RequestMapping(value = "delete_rag_tag", method = RequestMethod.DELETE)
    @Override
    public Response<String> deleteRagTag(@RequestParam("ragTag") String ragTag) {
        log.info("删除知识库开始: {}", ragTag);

        // 删除数据库中对应的知识库数据
        String deleteSql = "DELETE FROM vector_store WHERE metadata->>'ragTag' = ?";
        int rowsAffected = jdbcTemplate.update(deleteSql, ragTag);

        // 从 Redis 中删除知识库标签
        RList<String> elements = redisson.getList("ragTag");
        elements.remove(ragTag);

        log.info("删除知识库完成: {}, 删除的行数: {}", ragTag, rowsAffected);
        return Response.<String>builder()
                .code(200)
                .message("删除知识库完成")
                .data(ragTag)
                .build();
    }

    @RequestMapping(value = "file/upload", method = RequestMethod.POST, headers = "content-type=multipart/form-data")
    @Override
    public Response<String> uploadFile(@RequestParam("ragTag") String ragTag,@RequestParam("file") List<MultipartFile> files) {
        log.info("上传知识库开始{}",ragTag);
        for(MultipartFile file:files){
            TikaDocumentReader reader = new TikaDocumentReader(file.getResource());
            List<Document> documents = reader.get();
//            for(Document document:documents){
//                log.info(document.toString());
//            }
            List<Document> documentSplitterList = tokenTextSplitter.apply(documents);
            documents.forEach(document -> {document.getMetadata().put("ragTag", ragTag);});
            documentSplitterList.forEach(document -> {document.getMetadata().put("ragTag", ragTag);});
//            for(Document document:documentSplitterList){
//                log.info(document.toString());
//            }
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

    @PostMapping(value = "analyze_git_repository")
    @Override
    public Response<String> anlyzeGitRepository(@RequestParam String repoUrl, @RequestParam String userName, @RequestParam  String token) throws IOException, GitAPIException {
        String localPath = "./git-cloned-repo";
        String repoProjectName = extractProjectName(repoUrl);
        FileUtils.deleteDirectory(new File(localPath));
        Git git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(new File(localPath))
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, token))
                    .call();


        Files.walkFileTree(Paths.get(localPath), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try{
                    log.info("visitFile: " + file.toString());
                    PathResource pathResource = new PathResource(file);
                    TikaDocumentReader reader = new TikaDocumentReader(pathResource);

                    List<Document> documents = reader.get();
                    List<Document> documentsSplitterList = tokenTextSplitter.apply(documents);

                    documents.forEach(document -> document.getMetadata().put("ragTag", repoProjectName));
                    documentsSplitterList.forEach(document -> document.getMetadata().put("ragTag", repoProjectName));

                    pgVectorStore.accept(documentsSplitterList);
                    RList<String> elements = redisson.getList("ragTag");
                    if(!elements.contains(repoProjectName)){//如果知识库已经有就不做添加
                        elements.add(repoProjectName);
                    }

                } catch (Exception e) {
                    log.error("遍历解析路径，上传知识库失败:{}", file.getFileName());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                log.info("Failed to access file: {} - {}", file.toString(), exc.getMessage());
                return FileVisitResult.CONTINUE;
            }

        });

        FileUtils.deleteDirectory(new File(localPath));
        RList<String> elements = redisson.getList("ragTag");
        if(!elements.contains(repoProjectName)){
            elements.add(repoProjectName);
        }

        git.close();
        log.info("上传知识库完成 {}", repoProjectName);
        return Response.<String>builder()
                .code(200)
                .message("上传知识库完成")
                .data(repoProjectName )
                .build();
    }

    private String extractProjectName(String repoUrl) {
        String[] split = repoUrl.split("/");
        String projectNameWithGit = split[split.length - 1];
        return projectNameWithGit.replace(".git", "");
    }

}
