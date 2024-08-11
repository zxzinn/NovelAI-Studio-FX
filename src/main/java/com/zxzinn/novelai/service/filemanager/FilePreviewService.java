package com.zxzinn.novelai.service.filemanager;

import javafx.scene.Node;
import lombok.extern.log4j.Log4j2;

import java.io.File;

@Log4j2
public class FilePreviewService {
    private final FilePreviewFactory filePreviewFactory;

    public FilePreviewService() {
        this.filePreviewFactory = new FilePreviewFactory();
    }

    public Node getPreview(File file) {
        return filePreviewFactory.createPreview(file);
    }
}