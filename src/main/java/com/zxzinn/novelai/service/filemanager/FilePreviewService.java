package com.zxzinn.novelai.service.filemanager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.scene.Node;
import lombok.extern.log4j.Log4j2;

import java.io.File;

@Log4j2
@Singleton
public class FilePreviewService {
    private final FilePreviewFactory filePreviewFactory;

    @Inject
    public FilePreviewService(FilePreviewFactory filePreviewFactory) {
        this.filePreviewFactory = filePreviewFactory;
    }

    public Node getPreview(File file) {
        return filePreviewFactory.createPreview(file);
    }
}