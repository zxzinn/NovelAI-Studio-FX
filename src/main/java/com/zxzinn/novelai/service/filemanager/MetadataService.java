package com.zxzinn.novelai.service.filemanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Singleton;
import com.zxzinn.novelai.utils.common.ResourcePaths;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import lombok.extern.log4j.Log4j2;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

@Log4j2
@Singleton
public class MetadataService {

    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final Path executablePath;
    private final ExecutorService executorService;

    public MetadataService() {
        this.executablePath = getExecutablePath();
        this.executorService = Executors.newCachedThreadPool();
    }

    @NotNull
    private Path getExecutablePath() {
        String resourcePath = ResourcePaths.META_READER_PATH;
        try {
            URL resource = getClass().getResource(resourcePath);
            if (resource == null) {
                throw new IOException("無法找到資源：" + resourcePath);
            }

            Path tempDir = Files.createTempDirectory("novelai-metadata");
            Path exePath = tempDir.resolve("meta_reader.exe");

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

    private Optional<String> javaExtractor(@NotNull File file) {
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

    @NotNull
    private String executableExtractor(@NotNull File file) throws IOException, InterruptedException {
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

    private void processNode(@NotNull Node node, String indent, @NotNull StringBuilder metadata) {
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

    private Optional<String> formatCommentJson(@NotNull String metadata) {
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
            Optional<String> javaMetadata = javaExtractor(file);
            if (javaMetadata.isPresent()) {
                Optional<String> formattedComment = formatCommentJson(javaMetadata.get());
                if (formattedComment.isPresent()) {
                    return formatWithReflection(formattedComment.get());
                }
            }

            // 如果Java方法無法提取到有效的元數據，嘗試使用exe方法
            String exeMetadata = executableExtractor(file);
            if (!exeMetadata.isEmpty()) {
                return formatWithReflection(formatYamlOutput(exeMetadata));
            }

            return "無法提取元數據";
        } catch (Exception e) {
            log.error("無法讀取檔案元數據", e);
            return "無法讀取元數據：" + e.getMessage();
        }
    }

    private String formatWithReflection(String jsonString) throws IOException {
        JsonNode jsonNode = jsonMapper.readTree(jsonString);
        return new StructuredOutput(jsonNode).toString();
    }

    private static class StructuredOutput {
        private final StringBuilder output = new StringBuilder();

        public StructuredOutput(Object obj) {
            processObject(obj, 0);
        }

        private void processObject(Object obj, int indent) {
            if (obj == null) return;

            if (obj instanceof JsonNode) {
                processJsonNode((JsonNode) obj, indent);
            } else {
                for (Field field : obj.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    try {
                        Object value = field.get(obj);
                        appendIndent(indent);
                        output.append(field.getName()).append(": ");
                        if (value == null) {
                            output.append("null\n");
                        } else if (value instanceof JsonNode) {
                            output.append("\n");
                            processJsonNode((JsonNode) value, indent + 2);
                        } else if (value instanceof List) {
                            output.append("\n");
                            for (Object item : (List<?>) value) {
                                processObject(item, indent + 2);
                            }
                        } else if (!value.getClass().getName().startsWith("java.lang")) {
                            output.append("\n");
                            processObject(value, indent + 2);
                        } else {
                            output.append(value).append("\n");
                        }
                    } catch (IllegalAccessException e) {
                        log.error("Error accessing field", e);
                    }
                }
            }
        }

        private void processJsonNode(JsonNode node, int indent) {
            if (node.isObject()) {
                node.fields().forEachRemaining(entry -> {
                    appendIndent(indent);
                    output.append(entry.getKey()).append(": ");
                    if (entry.getValue().isValueNode()) {
                        output.append(entry.getValue().asText()).append("\n");
                    } else {
                        output.append("\n");
                        processJsonNode(entry.getValue(), indent + 2);
                    }
                });
            } else if (node.isArray()) {
                for (JsonNode element : node) {
                    processJsonNode(element, indent);
                }
            }
        }

        private void appendIndent(int indent) {
            output.append("  ".repeat(indent));
        }

        @Override
        public String toString() {
            return output.toString();
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

    public void updateMetadataList(File file, TextArea metadataTextArea) {
        if (file != null && file.isFile() && file.getName().toLowerCase().endsWith(".png")) {
            metadataTextArea.setText("正在讀取元數據...");

            CompletableFuture<String> futureMetadata = getFormattedMetadataAsync(file);
            futureMetadata.thenAcceptAsync(metadata -> {
                Platform.runLater(() -> {
                    metadataTextArea.setText(metadata);
                });
            }, executorService).exceptionally(ex -> {
                Platform.runLater(() -> {
                    metadataTextArea.setText("讀取元數據時發生錯誤: " + ex.getMessage());
                });
                return null;
            });
        } else {
            metadataTextArea.clear();
            if (file != null && file.isFile() && !file.getName().toLowerCase().endsWith(".png")) {
                metadataTextArea.setText("不支持的文件類型：僅支持 PNG 文件");
            }
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }
}