package com.zxzinn.novelai.service.filemanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.log4j.Log4j2;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

@Log4j2
public class MetadataService {

    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final Path executablePath;
    private final ExecutorService executorService;

    public MetadataService() {
        this.executablePath = extractExecutable();
        this.executorService = Executors.newCachedThreadPool();
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
                assert in != null;
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

    public CompletableFuture<List<String>> getMetadataAsync(File file) {
        return CompletableFuture.supplyAsync(() -> getMetadata(file), executorService);
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

        if (!file.getName().toLowerCase().endsWith(".png")) {
            metadataList.add("不支持的文件類型：僅支持 PNG 文件");
            return metadataList;
        }

        try {
            Optional<String> metadata = extractMetadataJava(file);
            if (metadata.isPresent()) {
                return formatOutput(metadata.get());
            } else {
                String exeMetadata = extractMetadataExe(file);
                if (!exeMetadata.isEmpty()) {
                    return Collections.singletonList(formatYamlOutput(exeMetadata));
                } else {
                    metadataList.add("無法提取元數據");
                }
            }
        } catch (Exception e) {
            log.error("無法讀取檔案元數據", e);
            metadataList.add("無法讀取元數據：" + e.getMessage());
        }

        return metadataList;
    }

    private Optional<String> extractMetadataJava(File file) {
        log.debug("Extracting metadata from file using Java: {}", file.getAbsolutePath());
        StringBuilder metadata = new StringBuilder();

        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(iis);
                    IIOMetadata imageMetadata = reader.getImageMetadata(0);
                    String[] names = imageMetadata.getMetadataFormatNames();
                    for (String name : names) {
                        Node root = imageMetadata.getAsTree(name);
                        processNode(root, "", metadata);
                    }
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException e) {
            log.warn("Unable to extract metadata from file using Java: {}. Error: {}", file.getName(), e.getMessage());
            return Optional.empty();
        }

        return Optional.of(metadata.toString());
    }

    private void processNode(Node node, String indent, StringBuilder metadata) {
        metadata.append(indent).append(node.getNodeName());
        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attr = attributes.item(i);
                metadata.append(" ").append(attr.getNodeName()).append("=\"").append(attr.getNodeValue()).append("\"");
            }
        }
        metadata.append("\n");

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            processNode(children.item(i), indent + "  ", metadata);
        }
    }

    private String extractMetadataExe(File file) throws IOException, InterruptedException {
        log.debug("Extracting metadata from file using exe: {}", file.getAbsolutePath());
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

    private List<String> formatOutput(String metadata) {
        List<String> formattedOutput = new ArrayList<>();
        formattedOutput.add("元數據:");
        Arrays.stream(metadata.split("\n"))
                .forEach(line -> formattedOutput.add("  " + line));

        Optional<String> formattedComment = formatCommentJson(metadata);
        if (formattedComment.isPresent()) {
            formattedOutput.add("格式化的 Comment JSON:");
            Arrays.stream(formattedComment.get().split("\n"))
                    .forEach(line -> formattedOutput.add("  " + line));
        }

        return formattedOutput;
    }

    private Optional<String> formatCommentJson(String metadata) {
        String commentLine = Arrays.stream(metadata.split("\n"))
                .filter(line -> line.contains("keyword=\"Comment\""))
                .findFirst()
                .orElse(null);

        if (commentLine == null) {
            return Optional.empty();
        }

        try {
            int startIndex = commentLine.indexOf("value=\"") + 7;
            int endIndex = commentLine.lastIndexOf("\"");
            String jsonString = commentLine.substring(startIndex, endIndex);

            JsonNode jsonNode = jsonMapper.readTree(jsonString);
            return Optional.of(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode));
        } catch (IOException e) {
            log.warn("Unable to format comment JSON. Error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public CompletableFuture<String> getFormattedMetadataAsync(File file) {
        return CompletableFuture.supplyAsync(() -> getFormattedMetadata(file), executorService);
    }

    public String getFormattedMetadata(File file) {
        if (file == null || !file.exists() || !file.isFile() || !file.getName().toLowerCase().endsWith(".png")) {
            return "不支持的文件類型或文件不存在：僅支持 PNG 文件";
        }

        try {
            Optional<String> javaMetadata = extractMetadataJava(file);
            if (javaMetadata.isPresent()) {
                Optional<String> formattedComment = formatCommentJson(javaMetadata.get());
                if (formattedComment.isPresent()) {
                    return formattedComment.get();
                }
            }

            // 如果Java方法無法提取到有效的元數據，嘗試使用exe方法
            String exeMetadata = extractMetadataExe(file);
            if (!exeMetadata.isEmpty()) {
                return formatYamlOutput(exeMetadata);
            }

            return "無法提取元數據";
        } catch (Exception e) {
            log.error("無法讀取檔案元數據", e);
            return "無法讀取元數據：" + e.getMessage();
        }
    }

    private String formatYamlOutput(String yamlString) {
        try {
            JsonNode jsonNode = yamlMapper.readTree(yamlString);
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (IOException e) {
            log.error("無法解析YAML輸出", e);
            return "無法解析YAML輸出：" + e.getMessage();
        }
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

    public void shutdown() {
        executorService.shutdown();
    }
}