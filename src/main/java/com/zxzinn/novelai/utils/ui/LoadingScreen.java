package com.zxzinn.novelai.utils.ui;

import com.zxzinn.novelai.utils.common.ResourcePaths;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.extern.log4j.Log4j2;

import java.net.URL;
import java.util.concurrent.CountDownLatch;

@Log4j2
public class LoadingScreen implements LoadingManager.LoadingObserver {
    private Stage loadingStage;
    private ProgressBar progressBar;
    private Label messageLabel;
    private final CountDownLatch showLatch = new CountDownLatch(1);

    public void show() {
        if (Platform.isFxApplicationThread()) {
            createAndShowStage();
        } else {
            Platform.runLater(this::createAndShowStage);
        }
        try {
            showLatch.await();
        } catch (InterruptedException e) {
            log.error("等待 loading screen 顯示時被中斷", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void onProgressUpdate(double progress, String message) {
        Platform.runLater(() -> {
            setProgress(progress);
            setMessage(message);
        });
    }

    private void createAndShowStage() {
        Rectangle background = new Rectangle(300, 200);
        background.setArcWidth(20);
        background.setArcHeight(20);
        background.setFill(Color.rgb(44, 62, 80));

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(260);
        progressBar.setStyle("-fx-accent: #3498db;");

        messageLabel = new Label("正在初始化...");
        messageLabel.setTextFill(Color.WHITE);
        messageLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        VBox loadingBox = new VBox(20, progressBar, messageLabel);
        loadingBox.setAlignment(Pos.CENTER);

        StackPane root = new StackPane(background, loadingBox);
        root.setEffect(new DropShadow(10, Color.BLACK));

        Scene scene = new Scene(root, 300, 200);
        scene.setFill(Color.TRANSPARENT);

        URL resource = getClass().getResource(ResourcePaths.STYLES_CSS);
        if (resource != null) {
            scene.getStylesheets().add(resource.toExternalForm());
        } else {
            log.warn("無法找到樣式文件: {}", ResourcePaths.STYLES_CSS);
        }

        loadingStage = new Stage(StageStyle.TRANSPARENT);
        loadingStage.setScene(scene);
        loadingStage.setAlwaysOnTop(true);
        loadingStage.show();

        // 添加淡入動畫
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        showLatch.countDown();
    }

    public void hide() {
        Platform.runLater(() -> {
            if (loadingStage != null) {
                FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), loadingStage.getScene().getRoot());
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(event -> loadingStage.close());
                fadeOut.play();
            }
        });
    }

    public void setProgress(double progress) {
        Platform.runLater(() -> {
            if (progressBar != null) {
                progressBar.setProgress(progress);
            }
        });
    }

    public void setMessage(String message) {
        Platform.runLater(() -> {
            if (messageLabel != null) {
                messageLabel.setText(message);
            }
        });
    }
}