package com.zxzinn.novelai.service.filemanager;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ImagePreviewCreator {
    public Pane createImagePreview(@NotNull File file) {
        Image image = new Image(file.toURI().toString());
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);

        Pane pane = new Pane(imageView);
        pane.setStyle("-fx-background-color: transparent;");

        imageView.fitWidthProperty().bind(pane.widthProperty());
        imageView.fitHeightProperty().bind(pane.heightProperty());

        addZoomAndPanHandlers(pane, imageView);

        return pane;
    }

    private void addZoomAndPanHandlers(@NotNull Pane pane, @NotNull ImageView imageView) {
        final double[] dragDelta = new double[2];

        pane.setOnScroll(event -> {
            event.consume();
            double scaleFactor = (event.getDeltaY() > 0) ? 1.1 : 1 / 1.1;
            imageView.setScaleX(imageView.getScaleX() * scaleFactor);
            imageView.setScaleY(imageView.getScaleY() * scaleFactor);
        });

        imageView.setOnMousePressed(event -> {
            dragDelta[0] = imageView.getTranslateX() - event.getSceneX();
            dragDelta[1] = imageView.getTranslateY() - event.getSceneY();
            event.consume();
        });

        imageView.setOnMouseDragged(event -> {
            imageView.setTranslateX(event.getSceneX() + dragDelta[0]);
            imageView.setTranslateY(event.getSceneY() + dragDelta[1]);
            event.consume();
        });
    }
}