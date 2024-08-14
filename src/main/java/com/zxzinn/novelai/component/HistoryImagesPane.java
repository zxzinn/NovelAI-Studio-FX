package com.zxzinn.novelai.component;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

public class HistoryImagesPane extends VBox {

    private static final int MAX_HISTORY_SIZE = 100;
    private static final double THUMBNAIL_WIDTH = 150;

    @FXML private ListView<HistoryImage> historyListView;

    private final Deque<HistoryImage> historyImages = new ArrayDeque<>();

    @Setter private Consumer<File> onImageClickHandler;

    public HistoryImagesPane() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/HistoryImagesPane.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        setupListView();
    }

    private void setupListView() {
        historyListView.setCellFactory(param -> new ListCell<>() {
            private final ImageView imageView = new ImageView();

            @Override
            protected void updateItem(HistoryImage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    imageView.setImage(item.image);
                    imageView.setFitWidth(THUMBNAIL_WIDTH);
                    imageView.setFitHeight(THUMBNAIL_WIDTH / item.image.getWidth() * item.image.getHeight());
                    imageView.setPreserveRatio(true);
                    imageView.setSmooth(true);
                    setGraphic(imageView);
                }
            }
        });

        historyListView.setOnMouseClicked(event -> {
            HistoryImage selectedItem = historyListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && onImageClickHandler != null) {
                onImageClickHandler.accept(selectedItem.file);
            }
        });

        historyListView.setPrefWidth(THUMBNAIL_WIDTH + 20);
        historyListView.setMaxWidth(THUMBNAIL_WIDTH + 20);
        historyListView.setMinWidth(THUMBNAIL_WIDTH + 20);
    }

    public void addImage(Image image, File imageFile) {
        Platform.runLater(() -> {
            HistoryImage historyImage = new HistoryImage(image, imageFile);
            historyImages.addFirst(historyImage);
            if (historyImages.size() > MAX_HISTORY_SIZE) {
                historyImages.removeLast();
            }
            historyListView.getItems().setAll(historyImages);
            historyListView.scrollTo(0);
        });
    }

    private record HistoryImage(Image image, File file) {}
}