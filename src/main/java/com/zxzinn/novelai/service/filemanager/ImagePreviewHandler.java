package com.zxzinn.novelai.service.filemanager;

import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import lombok.Getter;

import java.io.File;

public class ImagePreviewHandler {
    @Getter
    private final ImageView imageView;
    private final ScrollPane scrollPane;
    private double currentScale = 1.0;

    public ImagePreviewHandler(ScrollPane scrollPane) {
        this.scrollPane = scrollPane;
        this.imageView = new ImageView();
        setupImageView();
        setupScrollPane();
    }

    private void setupImageView() {
        imageView.setPreserveRatio(true);
        imageView.fitWidthProperty().bind(scrollPane.widthProperty());
        imageView.fitHeightProperty().bind(scrollPane.heightProperty());
    }

    private void setupScrollPane() {
        scrollPane.setContent(imageView);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        scrollPane.addEventFilter(ScrollEvent.ANY, event -> {
            if (event.isControlDown()) {
                event.consume();
                double zoomFactor = event.getDeltaY() > 0 ? 1.1 : 0.9;
                zoom(zoomFactor);
            }
        });
    }

    public void loadImage(File file) {
        Image image = new Image(file.toURI().toString());
        imageView.setImage(image);
        resetZoom();
    }

    public void zoom(double factor) {
        currentScale *= factor;
        imageView.setScaleX(currentScale);
        imageView.setScaleY(currentScale);
    }

    public void fitImage() {
        resetZoom();
        imageView.setFitWidth(scrollPane.getWidth());
        imageView.setFitHeight(scrollPane.getHeight());
    }

    public void showOriginalSize() {
        resetZoom();
        imageView.setFitWidth(0);
        imageView.setFitHeight(0);
    }

    private void resetZoom() {
        currentScale = 1.0;
        imageView.setScaleX(1);
        imageView.setScaleY(1);
    }
}