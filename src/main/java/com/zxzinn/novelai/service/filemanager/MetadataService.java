package com.zxzinn.novelai.service.filemanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

@Log4j2
public class MetadataService {

    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final Path executablePath;

    public MetadataService() {
        this.executablePath = extractExecutable();
    }

    private Path extractExecutable() {
        String resourcePath = "/com/zxzinn/novelai/executable/metareader.exe";
        try {
            URL resource = getClass().getResource(resourcePath);
            if (resource == null) {
                throw new IOException("無法找到資源：" + resourcePath);
            }

            Path tempDir = Files.createTempDirectory("novelai-metadata");
            Path exePath = tempDir.resolve("metareader.exe");

            try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
                Files.copy(in, exePath, StandardCopyOption.REPLACE_EXISTING);
            }

            exePath.toFile().deleteOnExit();
            tempDir.toFile().deleteOnExit();

            return exePath;
        } catch (IOException e) {
            log.error("無法提取可執行文件", e);
            throw new RuntimeException("無法提取可執行文件", e);
        }
    }

    public List<String> getMetadata(File file) {
        List<String> metadataList = new ArrayList<>();
        if (file == null || !file.exists()) {
            metadataList.add("文件不存在或無法訪問");
            return metadataList;
        }

        if (file.isDirectory()) {
            metadataList.add("名稱: " + file.getName());
            metadataList.add("路徑: " + file.getAbsolutePath());
            metadataList.add("類型: 目錄");
            metadataList.add("最後修改時間: " + new java.util.Date(file.lastModified()));
            return metadataList;
        }

        try {
            String metadata = extractMetadata(file);
            if (metadata != null && !metadata.isEmpty()) {
                metadataList.add("原始元數據:");
                metadataList.addAll(formatYamlOutput(metadata));
            } else {
                metadataList.add("無法提取元數據");
            }
        } catch (Exception e) {
            log.error("無法讀取檔案元數據", e);
            metadataList.add("無法讀取元數據：" + e.getMessage());
        }

        return metadataList;
    }

    private String extractMetadata(File file) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(executablePath.toString(), file.getAbsolutePath());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Metadata extraction failed with exit code: " + exitCode);
        }

        return output.toString();
    }

    private List<String> formatYamlOutput(String yamlString) {
        List<String> formattedOutput = new ArrayList<>();
        try {
            JsonNode jsonNode = yamlMapper.readTree(yamlString);
            formattedOutput.addAll(formatJsonNode(jsonNode, 0));
        } catch (IOException e) {
            log.error("無法解析YAML輸出", e);
            formattedOutput.add("無法解析YAML輸出：" + e.getMessage());
        }
        return formattedOutput;
    }

    private List<String> formatJsonNode(JsonNode node, int indent) {
        List<String> lines = new ArrayList<>();
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                lines.add(getIndent(indent) + key + ":");
                lines.addAll(formatJsonNode(value, indent + 2));
            });
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                lines.add(getIndent(indent) + "-");
                lines.addAll(formatJsonNode(element, indent + 2));
            }
        } else {
            lines.add(getIndent(indent) + node.asText());
        }
        return lines;
    }

    private String getIndent(int indent) {
        return " ".repeat(indent);
    }
}