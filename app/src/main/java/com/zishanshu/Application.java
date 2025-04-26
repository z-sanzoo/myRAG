package com.zishanshu;

import org.springframework.ai.autoconfigure.ollama.OllamaAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

//注意被标注为appliaction的文件,所在的文件夹,在这个文件夹之下的内容都会被spring扫描到
@SpringBootApplication(exclude = {OllamaAutoConfiguration.class})
@Configuration
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
