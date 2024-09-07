package com.zxzinn.novelai.component;

import com.zxzinn.novelai.model.HistoryImage;
import com.zxzinn.novelai.viewmodel.HistoryImagesPaneViewModel;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.function.Consumer;

public class HistoryImagesPane extends VBox {

    private static final double THUMBNAIL_WIDTH = 90;

    private final ListView<HistoryImage> historyListView;
    private final HistoryImagesPaneViewModel viewModel;

    public HistoryImagesPane() {
        this.viewModel = new HistoryImagesPaneViewModel();

        Label titleLabel = new Label("歷史圖片");
        titleLabel.getStyleClass().add("history-panel-title");

        historyListView = new ListView<>();
        historyListView.setItems(viewModel.getHistoryImages());
        historyListView.setCellFactory(this::createListCell);
        historyListView.setOnMouseClicked(event -> {
            HistoryImage selectedItem = historyListView.getSelectionModel().getSelectedItem();
            viewModel.handleImageClick(selectedItem);
        });

        setupListViewStyle();

        getChildren().addAll(titleLabel, historyListView);
        getStyleClass().add("history-panel");
        setSpacing(5.0);
    }

    private void setupListViewStyle() {
        historyListView.setPrefWidth(THUMBNAIL_WIDTH + 10);
        historyListView.setMaxWidth(THUMBNAIL_WIDTH + 10);
        historyListView.setMinWidth(THUMBNAIL_WIDTH + 10);
        historyListView.getStyleClass().add("history-list-view");
        VBox.setVgrow(historyListView, Priority.ALWAYS);
    }

    private ListCell<HistoryImage> createListCell(ListView<HistoryImage> listView) {
        return new ListCell<>() {
            private final ImageView imageView = new ImageView();

            @Override
            protected void updateItem(HistoryImage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    imageView.setImage(item.image());
                    imageView.setFitWidth(THUMBNAIL_WIDTH);
                    imageView.setFitHeight(THUMBNAIL_WIDTH / item.image().getWidth() * item.image().getHeight());
                    imageView.setPreserveRatio(true);
                    imageView.setSmooth(true);
                    setGraphic(imageView);
                }
            }
        };
    }

    public void setOnImageClickHandler(Consumer<File> handler) {
        viewModel.setOnImageClickHandler(handler);
    }

    public void addImage(Image image, java.io.File imageFile) {
        viewModel.addImage(image, imageFile);
    }
}