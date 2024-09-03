package com.zxzinn.novelai.service.filemanager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.scene.Node;
import javafx.scene.control.Label;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;

@Log4j2
@Singleton
public class FilePreviewService {
    private final ImagePreviewCreator imagePreviewCreator;

    @Inject
    public FilePreviewService(ImagePreviewCreator imagePreviewCreator) {
        this.imagePreviewCreator = imagePreviewCreator;
    }

    public Node getPreview(File file) {
        return this.createPreview(file);
    }

    public Node createPreview(@NotNull File file) {
        if (file.isFile()) {
            try {
                String mimeType = Files.probeContentType(file.toPath());
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                if (mimeType.startsWith("image/")) {
                    return imagePreviewCreator.createImagePreview(file);
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
}