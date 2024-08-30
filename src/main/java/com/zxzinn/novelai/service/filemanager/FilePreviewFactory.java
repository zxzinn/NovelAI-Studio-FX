package com.zxzinn.novelai.service.filemanager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.web.WebView;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Log4j2
@Singleton
public class FilePreviewFactory {
    private final Tika tika;
    private final ImagePreviewCreator imagePreviewCreator;

    @Inject
    public FilePreviewFactory(ImagePreviewCreator imagePreviewCreator) {
        this.tika = new Tika();
        this.imagePreviewCreator = imagePreviewCreator;
    }

    public Node createPreview(@NotNull File file) {
        if (file.isFile()) {
            try {
                String mimeType = tika.detect(file);
                if (mimeType.startsWith("image/")) {
                    return imagePreviewCreator.createImagePreview(file);
                } else if (mimeType.startsWith("text/") || mimeType.equals("application/pdf")) {
                    return createTextPreview(file);
                } else {
                    return new Label("不支援的文件格式：" + mimeType);
                }
            } catch (Exception e) {
                log.error("無法載入預覽", e);
                return new Label("無法載入預覽：" + e.getMessage());
            }
        } else {
            return new Label("請選擇一個文件");
        }
    }

    @NotNull
    private Node createTextPreview(@NotNull File file) throws IOException {
        String content = Files.readString(file.toPath());
        String extension = FilenameUtils.getExtension(file.getName());
        String highlightLanguage = getHighlightLanguage(extension);

        WebView webView = new WebView();
        webView.getEngine().loadContent(createTextPreviewHtml(content, highlightLanguage));
        webView.setPrefSize(800, 600);

        return webView;
    }

    @NotNull
    private String createTextPreviewHtml(String content, String highlightLanguage) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.8.0/styles/default.min.css">
                <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.8.0/highlight.min.js"></script>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        padding: 20px;
                        background-color: #f0f0f0;
                    }
                    pre {
                        background-color: #ffffff;
                        border: 1px solid #ddd;
                        border-radius: 4px;
                        padding: 16px;
                        font-size: 14px;
                    }
                    code {
                        font-family: 'Courier New', Courier, monospace;
                    }
                </style>
            </head>
            <body>
                <pre><code class="%s">%s</code></pre>
                <script>hljs.highlightAll();</script>
            </body>
            </html>
            """, highlightLanguage, escapeHtml(content));
    }

    @NotNull
    @Contract(pure = true)
    private String getHighlightLanguage(@NotNull String extension) {
        return switch (extension.toLowerCase()) {
            case "java" -> "java";
            case "py" -> "python";
            case "js" -> "javascript";
            case "html" -> "html";
            case "css" -> "css";
            case "json" -> "json";
            case "xml" -> "xml";
            default -> "plaintext";
        };
    }

    @NotNull
    private String escapeHtml(@NotNull String content) {
        return content.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}