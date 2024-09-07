package com.zxzinn.novelai.viewmodel;

import com.zxzinn.novelai.model.HistoryImage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

public class HistoryImagesPaneViewModel {

    private static final int MAX_HISTORY_SIZE = 100;

    @Getter
    private final ObservableList<HistoryImage> historyImages = FXCollections.observableArrayList();
    private final Deque<HistoryImage> historyDeque = new ArrayDeque<>();
    @Setter
    private Consumer<File> onImageClickHandler;

    public void addImage(Image image, File imageFile) {
        HistoryImage historyImage = new HistoryImage(image, imageFile);
        historyDeque.addFirst(historyImage);
        if (historyDeque.size() > MAX_HISTORY_SIZE) {
            historyDeque.removeLast();
        }
        updateObservableList();
    }

    private void updateObservableList() {
        historyImages.setAll(historyDeque);
    }

    public void handleImageClick(HistoryImage historyImage) {
        if (onImageClickHandler != null && historyImage != null) {
            onImageClickHandler.accept(historyImage.file());
        }
    }
}