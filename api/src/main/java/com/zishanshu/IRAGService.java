package com.zishanshu;

import com.zishanshu.response.Response;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IRAGService {

    Response<List<String>> queryRagTagList() ;

    Response<String> uploadFile(String ragTag, List<MultipartFile> files) throws Exception;

    Response<String> anlyzeGitRepository(String repoUrl, String username, String token) throws IOException, GitAPIException;
}
