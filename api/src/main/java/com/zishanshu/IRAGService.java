package com.zishanshu;

import com.zishanshu.response.Response;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IRAGService {

    Response<List<String>> queryRagTagList() ;

    @RequestMapping(value = "delete_rag_tag", method = RequestMethod.DELETE)
    Response<String> deleteRagTag(@RequestParam("ragTag") String ragTag);

    Response<String> uploadFile(String ragTag, List<MultipartFile> files) throws Exception;

    Response<String> anlyzeGitRepository(String repoUrl, String username, String token) throws IOException, GitAPIException;
}
