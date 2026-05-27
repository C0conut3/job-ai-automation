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
 * Lightweight Filesystem MCP service providing basic file operations for AI tool calling.
 * Used for context management in the Common Agent.
 * Note: This is Spring AI Tool Calling, which works with MCP through Spring AI's integration.
 */
@Service
public class FilesystemMcpService {

    private static final Logger log = LoggerFactory.getLogger(FilesystemMcpService.class);
    private static final String BASE_DIR = "mcp_context";

    public FilesystemMcpService() {
        try {
            Files.createDirectories(Paths.get(BASE_DIR));
            log.info("MCP context directory initialized: {}", Paths.get(BASE_DIR).toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to create MCP context directory", e);
        }
    }

    @Tool(description = "Read file content for getting conversation context or background information")
    public String read_file(String path) {
        log.info("MCP tool called: read_file {}", path);
        try {
            Path filePath;
            if (path.startsWith(BASE_DIR + "/") || path.startsWith(BASE_DIR + "\\")) {
                filePath = Paths.get(path);
            } else {
                filePath = Paths.get(BASE_DIR, path);
            }

            if (!Files.exists(filePath)) {
                return "Error: File not found: " + filePath.toString();
            }
            return Files.readString(filePath);
        } catch (IOException e) {
            log.error("Failed to read file: {}", path, e);
            return "Error: Failed to read file: " + e.getMessage();
        }
    }

    @Tool(description = "Write content to specified file for saving or updating conversation context")
    public String write_file(String path, String content) {
        log.info("MCP tool called: write_file {}", path);
        try {
            Path filePath;
            if (path.startsWith(BASE_DIR + "/") || path.startsWith(BASE_DIR + "\\")) {
                filePath = Paths.get(path);
            } else {
                filePath = Paths.get(BASE_DIR, path);
            }

            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content);
            return "Success: Content written to " + path;
        } catch (IOException e) {
            log.error("Failed to write file: {}", path, e);
            return "Error: Failed to write file: " + e.getMessage();
        }
    }

    @Tool(description = "List files and subdirectories in specified directory")
    public List<String> list_directory(String path) {
        log.info("MCP tool called: list_directory {}", path);
        try {
            Path dirPath;
            if (path == null || path.isBlank()) {
                dirPath = Paths.get(BASE_DIR);
            } else if (path.startsWith(BASE_DIR + "/") || path.startsWith(BASE_DIR + "\\")) {
                dirPath = Paths.get(path);
            } else {
                dirPath = Paths.get(BASE_DIR, path);
            }

            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            try (Stream<Path> stream = Files.list(dirPath)) {
                return stream.map(p -> Files.isDirectory(p) ? p.getFileName().toString() + "/" : p.getFileName().toString())
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error("Failed to list directory: {}", path, e);
            return List.of("Error: Failed to list directory: " + e.getMessage());
        }
    }
}
