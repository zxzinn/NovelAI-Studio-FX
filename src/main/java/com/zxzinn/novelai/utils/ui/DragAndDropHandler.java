package com.zxzinn.novelai.utils.ui;

import com.zxzinn.novelai.service.ui.NotificationService;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.File;
import java.util.function.Consumer;

public class DragAndDropHandler {

    private final Consumer<File> onFileDrop;
    private StackPane overlay;

    public DragAndDropHandler(Consumer<File> onFileDrop) {
        this.onFileDrop = onFileDrop;
    }

    public void enableDragAndDrop(StackPane root) {
        createOverlay(root);
        root.setOnDragEntered(this::handleDragEntered);
        root.setOnDragExited(this::handleDragExited);
        root.setOnDragOver(this::handleDragOver);
        root.setOnDragDropped(this::handleDragDropped);
    }

    private void createOverlay(StackPane root) {
        overlay = new StackPane();
        overlay.setVisible(false);
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");

        Rectangle rect = new Rectangle(root.getWidth(), root.getHeight());
        rect.setFill(Color.TRANSPARENT);
        rect.setStroke(Color.WHITE);
        rect.setStrokeWidth(2);
        rect.getStrokeDashArray().addAll(10.0, 10.0);

        Label label = new Label("拖放文件到這裡");
        label.setStyle("-fx-font-size: 24px; -fx-text-fill: white;");

        overlay.getChildren().addAll(rect, label);

        root.getChildren().add(overlay);

        // Bind the overlay size to the root size
        overlay.prefWidthProperty().bind(root.widthProperty());
        overlay.prefHeightProperty().bind(root.heightProperty());
    }

    private void handleDragEntered(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            showOverlay(true);
        }
        event.consume();
    }

    private void handleDragExited(DragEvent event) {
        showOverlay(false);
        event.consume();
    }

    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    private void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            File file = db.getFiles().get(0);
            onFileDrop.accept(file);
            success = true;
            showNotification("文件已成功拖放: " + file.getName());
        }
        event.setDropCompleted(success);
        showOverlay(false);
        event.consume();
    }

    private void showOverlay(boolean show) {
        FadeTransition ft = new FadeTransition(Duration.millis(300), overlay);
        ft.setFromValue(show ? 0 : 1);
        ft.setToValue(show ? 1 : 0);
        ft.setOnFinished(e -> overlay.setVisible(show));
        overlay.setVisible(true);
        ft.play();
    }

    private void showNotification(String message) {
        Platform.runLater(() -> NotificationService.showNotification(message, Duration.seconds(3)));
    }
}