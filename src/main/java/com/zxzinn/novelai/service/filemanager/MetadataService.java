package com.zxzinn.novelai.service.filemanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zxzinn.novelai.utils.common.ResourcePaths;
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

    @Inject
    public MetadataService() {
        this.executablePath = extractExecutable();
        this.executorService = Executors.newCachedThreadPool();
    }

    @NotNull
    private Path extractExecutable() {
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

    private Optional<String> extractMetadataJava(@NotNull File file) {
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

    @NotNull
    private String extractMetadataExe(@NotNull File file) throws IOException, InterruptedException {
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

    public void shutdown() {
        executorService.shutdown();
    }
}