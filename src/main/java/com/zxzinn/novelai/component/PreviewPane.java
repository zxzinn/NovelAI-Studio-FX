package com.zxzinn.novelai.component;

import com.zxzinn.novelai.service.filemanager.FilePreviewService;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import lombok.extern.log4j.Log4j2;

import java.io.File;

@Log4j2
public class PreviewPane extends StackPane {
    private final ScrollPane scrollPane;
    private final ImageView imageView;
    private final FilePreviewService filePreviewService;

    public PreviewPane(FilePreviewService filePreviewService) {
        this.filePreviewService = filePreviewService;
        this.scrollPane = new ScrollPane();
        this.imageView = new ImageView();

        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        imageView.setPreserveRatio(true);
        imageView.fitWidthProperty().bind(widthProperty());
        imageView.fitHeightProperty().bind(heightProperty());

        getChildren().add(scrollPane);
    }

    public void updatePreview(File file) {
        if(file != null && file.isFile()) {
            Node previewNode = filePreviewService.getPreview(file);
            scrollPane.setContent(previewNode);

            if(previewNode instanceof ImageView imageview) {
                imageview.setPreserveRatio(true);
                imageview.fitWidthProperty().bind(widthProperty());
                imageview.fitHeightProperty().bind(heightProperty());
            }
        } else {
            scrollPane.setContent(null);
        }
    }
}