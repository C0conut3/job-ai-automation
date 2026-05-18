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
            Path filePath = Paths.get(BASE_DIR, path);
            if (!Files.exists(filePath)) {
                return "Error: File not found: " + path;
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
            Path filePath = Paths.get(BASE_DIR, path);
            Path parentDir = filePath.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            } else {
                Files.createDirectories(Paths.get(BASE_DIR));
            }
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
            Path dirPath = (path == null || path.isEmpty() || path.equals("."))
                    ? Paths.get(BASE_DIR)
                    : Paths.get(BASE_DIR, path);

            if (!Files.exists(dirPath)) {
                return List.of("Error: Directory not found: " + path);
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
            log.error("Failed to list directory: {}", path, e);
            return List.of("Error: Failed to list directory: " + e.getMessage());
        }
    }
}
