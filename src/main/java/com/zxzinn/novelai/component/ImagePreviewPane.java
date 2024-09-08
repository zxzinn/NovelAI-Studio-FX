package com.zxzinn.novelai.component;

import com.google.inject.Singleton;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.nio.file.Files;
import java.util.Optional;

@Log4j2
@Singleton
public class ImagePreviewPane extends StackPane {
    private final ScrollPane scrollPane;

    public ImagePreviewPane() {
        this.scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        getChildren().add(scrollPane);
    }

    public void updatePreview(File file) {
        Node previewNode = Optional.ofNullable(file)
                .filter(File::isFile)
                .map(this::createPreview)
                .orElseGet(() -> new Label("請選擇一個文件"));

        scrollPane.setContent(previewNode);

        if (previewNode instanceof Pane pane) {
            pane.prefWidthProperty().bind(widthProperty());
            pane.prefHeightProperty().bind(heightProperty());
        }
    }

    private Node createPreview(File file) {
        try {
            String mimeType = Optional.ofNullable(Files.probeContentType(file.toPath()))
                    .orElse("application/octet-stream");

            return mimeType.startsWith("image/")
                    ? createImagePreview(file)
                    : new Label("不支援的文件格式：" + mimeType);
        } catch (Exception e) {
            log.error("無法載入預覽", e);
            return new Label("無法載入預覽：" + e.getMessage());
        }
    }

    private Pane createImagePreview(File file) {
        ImageView imageView = new ImageView(new Image(file.toURI().toString()));
        imageView.setPreserveRatio(true);

        Pane pane = new Pane(imageView);
        pane.setStyle("-fx-background-color: transparent;");

        imageView.fitWidthProperty().bind(pane.widthProperty());
        imageView.fitHeightProperty().bind(pane.heightProperty());

        addZoomAndPanHandlers(pane, imageView);

        return pane;
    }

    private void addZoomAndPanHandlers(Pane pane, ImageView imageView) {
        pane.setOnScroll(event -> {
            double scaleFactor = event.getDeltaY() > 0 ? 1.1 : 1 / 1.1;
            imageView.setScaleX(imageView.getScaleX() * scaleFactor);
            imageView.setScaleY(imageView.getScaleY() * scaleFactor);
            event.consume();
        });

        imageView.setOnMousePressed(event -> {
            imageView.setUserData(new double[]{
                    imageView.getTranslateX() - event.getSceneX(),
                    imageView.getTranslateY() - event.getSceneY()
            });
            event.consume();
        });

        imageView.setOnMouseDragged(event -> {
            double[] dragDelta = (double[]) imageView.getUserData();
            imageView.setTranslateX(event.getSceneX() + dragDelta[0]);
            imageView.setTranslateY(event.getSceneY() + dragDelta[1]);
            event.consume();
        });
    }
}