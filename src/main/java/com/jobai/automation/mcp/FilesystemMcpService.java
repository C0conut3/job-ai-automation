package com.jobai.automation.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 简易的 Filesystem MCP 服务，提供基本的文件操作工具供 AI 使用。
 * 用于在通用问答 Agent 中实现基于文件系统的上下文管理。
 */
@Service
public class FilesystemMcpService {

    private static final Logger log = LoggerFactory.getLogger(FilesystemMcpService.class);
    private static final String BASE_DIR = "mcp_context";

    public FilesystemMcpService() {
        try {
            Files.createDirectories(Paths.get(BASE_DIR));
            log.info("MCP 上下文目录已初始化: {}", Paths.get(BASE_DIR).toAbsolutePath());
        } catch (IOException e) {
            log.error("无法创建 MCP 上下文目录", e);
        }
    }

    @Tool(description = "读取指定路径的文件内容，用于获取对话上下文或相关背景信息")
    public String read_file(String path) {
        log.info("MCP 工具调用: 读取文件 {}", path);
        try {
            Path filePath = Paths.get(BASE_DIR, path);
            if (!Files.exists(filePath)) {
                return "错误: 文件不存在: " + path;
            }
            return Files.readString(filePath);
        } catch (IOException e) {
            log.error("读取文件失败: {}", path, e);
            return "错误: 读取文件失败: " + e.getMessage();
        }
    }

    @Tool(description = "将内容写入指定文件，用于保存或更新对话上下文")
    public String write_file(String path, String content) {
        log.info("MCP 工具调用: 写入文件 {}", path);
        try {
            Path filePath = Paths.get(BASE_DIR, path);
            Path parentDir = filePath.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            } else {
                Files.createDirectories(Paths.get(BASE_DIR));
            }
            Files.writeString(filePath, content);
            return "成功: 内容已写入 " + path;
        } catch (IOException e) {
            log.error("写入文件失败: {}", path, e);
            return "错误: 写入文件失败: " + e.getMessage();
        }
    }

    @Tool(description = "列出指定目录下的文件和子目录")
    public List<String> list_directory(String path) {
        log.info("MCP 工具调用: 列出目录 {}", path);
        try {
            Path dirPath = (path == null || path.isEmpty() || path.equals(".")) 
                    ? Paths.get(BASE_DIR) 
                    : Paths.get(BASE_DIR, path);
            
            if (!Files.exists(dirPath)) {
                return List.of("错误: 目录不存在: " + path);
            }
            
            try (Stream<Path> stream = Files.list(dirPath)) {
                return stream
                        .map(p -> {
                            String fileName = p.getFileName().toString();
                            return Files.isDirectory(p) ? fileName + "/" : fileName;
                        })
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error("列出目录失败: {}", path, e);
            return List.of("错误: 列出目录失败: " + e.getMessage());
        }
    }
}
