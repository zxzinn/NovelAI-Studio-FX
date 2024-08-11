package com.zxzinn.novelai.component;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;


public class HistoryImagesPane extends VBox {

    @FXML
    private VBox historyImagesContainer;


    @Setter
    private Consumer<File> onImageClickHandler;


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


    public void addImage(Image image, File imageFile) {

        double aspectRatio = image.getWidth() / image.getHeight();
        ImageView historyImageView = new ImageView(image);
        historyImageView.setPreserveRatio(true);
        historyImageView.setSmooth(true);
        historyImageView.setFitWidth(150);
        historyImageView.setFitHeight(150 / aspectRatio);


        historyImageView.setOnMouseClicked(event -> {
            if (onImageClickHandler != null) {
                onImageClickHandler.accept(imageFile);
            }
        });


        historyImagesContainer.getChildren().addFirst(historyImageView);
    }

}