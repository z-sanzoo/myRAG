package com.zishanshu.domain.service;


import com.zishanshu.domain.model.ComputerFunctionRequest;
import com.zishanshu.domain.model.ComputerFunctionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Slf4j
@Service
public class TestComputerService {
    @Tool(description = "获取电脑配置2")
    public ComputerFunctionResponse queryConfigTest(ComputerFunctionRequest request) {
        log.info("获取电脑配置信息 {}", request.getComputer());
        // 获取系统属性
        Properties properties = System.getProperties();

        // 操作系统名称
        String osName = properties.getProperty("os.name");
        // 操作系统版本
        String osVersion = properties.getProperty("os.version");
        // 操作系统架构
        String osArch = properties.getProperty("os.arch");
        // 用户的账户名称
        String userName = properties.getProperty("user.name");
        // 用户的主目录
        String userHome = properties.getProperty("user.home");
        // 用户的当前工作目录
        String userDir = properties.getProperty("user.dir");
        // Java 运行时环境版本
        String javaVersion = properties.getProperty("java.version");

        String osInfo = "";
        // 根据操作系统执行特定的命令来获取更多信息
        if (osName.toLowerCase().contains("win")) {
            // Windows特定的代码
            osInfo = getWindowsSpecificInfo();
        } else if (osName.toLowerCase().contains("mac")) {
            // macOS特定的代码
            osInfo = getMacSpecificInfo();
        } else if (osName.toLowerCase().contains("nix") || osName.toLowerCase().contains("nux")) {
            // Linux特定的代码
            osInfo = getLinuxSpecificInfo();
        }

        ComputerFunctionResponse response = new ComputerFunctionResponse();
        response.setOsName(osName);
        response.setOsVersion(osVersion);
        response.setOsArch(osArch);
        response.setUserName(userName);
        response.setUserHome(userHome);
        response.setUserDir(userDir);
        response.setJavaVersion(javaVersion);
        response.setOsInfo(osInfo);

        return response;
    }

    private String getWindowsSpecificInfo() {
        StringBuilder cache = new StringBuilder();
        // Windows特定的系统信息获取
        try {
            Process process = Runtime.getRuntime().exec("systeminfo");
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                cache.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cache.toString();
    }

    private static String getMacSpecificInfo() {
        StringBuilder cache = new StringBuilder();
        // macOS特定的系统信息获取
        try {
            Process process = Runtime.getRuntime().exec("system_profiler SPHardwareDataType");
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                cache.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cache.toString();
    }

    private static String getLinuxSpecificInfo() {
        StringBuilder cache = new StringBuilder();
        // Linux特定的系统信息获取
        try {
            Process process = Runtime.getRuntime().exec("lshw -short");
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                cache.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cache.toString();
    }

}
