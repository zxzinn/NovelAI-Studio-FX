package com.zxzinn.novelai.component;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class HistoryImagesPane extends VBox {

    @FXML
    private VBox historyImagesContainer;

    public HistoryImagesPane() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/HistoryImagesPane.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void addImage(Image image) {
        double aspectRatio = image.getWidth() / image.getHeight();
        ImageView historyImageView = new ImageView(image);
        historyImageView.setPreserveRatio(true);
        historyImageView.setSmooth(true);
        historyImageView.setFitWidth(150);
        historyImageView.setFitHeight(150 / aspectRatio);

        historyImagesContainer.getChildren().addFirst(historyImageView);
    }

    public void clear() {
        historyImagesContainer.getChildren().clear();
    }
}