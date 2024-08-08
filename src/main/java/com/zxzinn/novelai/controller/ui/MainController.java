package com.zxzinn.novelai.controller.ui;

import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.controller.filemanager.FileManagerController;
import com.zxzinn.novelai.controller.generation.text2img.ImageGeneratorController;
import com.zxzinn.novelai.service.generation.ImageGenerationService;
import com.zxzinn.novelai.utils.common.SettingsManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public class MainController {
    @FXML private TabPane mainTabPane;
    @FXML private Tab generatorTab;
    @FXML private Tab img2ImgTab;
    @FXML private Tab fileManagerTab;
    @FXML private Button minimizeButton;
    @FXML private Button maximizeButton;
    @FXML private Button closeButton;
    @FXML private VBox titleBar;

    private final SettingsManager settingsManager;
    private final APIClient apiClient;
    private final EmbedProcessor embedProcessor;
    private final ImageGenerationService imageGenerationService;
    private final ImageUtils imageUtils;
    @Setter
    private Stage stage;
    private double xOffset = 0;
    private double yOffset = 0;
    private Rectangle2D restoreBounds;
    private boolean isMaximized = false;
    private AnimationTimer resizeAnimationTimer;


    public MainController(SettingsManager settingsManager, APIClient apiClient, EmbedProcessor embedProcessor,
                          ImageGenerationService imageGenerationService, ImageUtils imageUtils) {
        this.settingsManager = settingsManager;
        this.apiClient = apiClient;
        this.embedProcessor = embedProcessor;
        this.imageGenerationService = imageGenerationService;
        this.imageUtils = imageUtils;
    }

    @FXML
    public void initialize() {
        setupWindowControls();
        loadTabContent();
        setupDraggableWindow();
    }

    public void setStageAndInit(Stage stage) {
        this.stage = stage;
        setupResizeableWindow();
    }

    private void setupWindowControls() {
        minimizeButton.setOnAction(event -> stage.setIconified(true));
        maximizeButton.setOnAction(event -> toggleMaximize());
        closeButton.setOnAction(event -> {
            settingsManager.shutdown();
            stage.close();
            Platform.exit();
            System.exit(0);
        });
    }

    private void toggleMaximize() {
        if (!isMaximized) {
            // 保存當前位置和大小
            restoreBounds = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());
            isMaximized = true;
        } else {
            if (restoreBounds != null) {
                stage.setX(restoreBounds.getMinX());
                stage.setY(restoreBounds.getMinY());
                stage.setWidth(restoreBounds.getWidth());
                stage.setHeight(restoreBounds.getHeight());
            }
            isMaximized = false;
        }
    }

    private void setupDraggableWindow() {
        titleBar.setOnMousePressed(this::handleMousePressed);
        titleBar.setOnMouseDragged(this::handleMouseDragged);
        titleBar.setOnMouseReleased(this::handleMouseReleased);
    }

    private void handleMousePressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    private void handleMouseReleased(MouseEvent event) {
        if (event.getScreenY() <= 0) {
            toggleMaximize();
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        if (!isMaximized) {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        } else {
            // 如果窗口是最大化的，拖動時應該恢復到正常大小
            double percentX = event.getScreenX() / Screen.getPrimary().getVisualBounds().getWidth();
            restoreBounds = new Rectangle2D(
                    event.getScreenX() - stage.getWidth() * percentX,
                    event.getScreenY(),
                    restoreBounds.getWidth(),
                    restoreBounds.getHeight());
            toggleMaximize();

            // 確保鼠標仍然在標題欄上
            xOffset = stage.getWidth() * percentX;
            yOffset = event.getSceneY();
        }
    }

    private void loadTabContent() {
        try {
            loadGeneratorTab();
            loadImg2ImgTab();
            loadFileManagerTab();
        } catch (IOException e) {
            log.error("載入標籤內容時發生錯誤", e);
        }
    }

    private void loadGeneratorTab() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/ImageGenerator.fxml"));
        loader.setControllerFactory(param -> new ImageGeneratorController(apiClient, embedProcessor, settingsManager, imageGenerationService, imageUtils));
        BorderPane content = loader.load();
        generatorTab.setContent(content);
    }

    private void loadImg2ImgTab() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/ImageGenerator.fxml"));
        loader.setControllerFactory(param -> new ImageGeneratorController(apiClient, embedProcessor, settingsManager, imageGenerationService, imageUtils));
        BorderPane content = loader.load();
        generatorTab.setContent(content);
    }

    private void loadFileManagerTab() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/FileManager.fxml"));
        FileManagerController controller = new FileManagerController(settingsManager);
        loader.setController(controller);
        BorderPane content = loader.load();
        fileManagerTab.setContent(content);
    }

    private void setupResizeableWindow() {
        Cursor defaultCursor = Cursor.DEFAULT;
        Cursor resizeCursor = Cursor.SE_RESIZE;
        AtomicBoolean resizing = new AtomicBoolean(false);

        class ResizeAnimationTimer extends AnimationTimer {
            private double targetWidth;
            private double targetHeight;

            @Override
            public void handle(long now) {
                double currentWidth = stage.getWidth();
                double currentHeight = stage.getHeight();

                stage.setWidth(currentWidth + (targetWidth - currentWidth) * 0.2);
                stage.setHeight(currentHeight + (targetHeight - currentHeight) * 0.2);

                if (Math.abs(stage.getWidth() - targetWidth) < 0.1 &&
                        Math.abs(stage.getHeight() - targetHeight) < 0.1) {
                    stop();
                }
            }

            public void setTarget(double width, double height) {
                this.targetWidth = width;
                this.targetHeight = height;
            }
        }

        ResizeAnimationTimer resizeAnimationTimer = new ResizeAnimationTimer();

        stage.getScene().setOnMouseMoved(event -> {
            if (isInResizeZone(event)) {
                stage.getScene().setCursor(resizeCursor);
            } else {
                stage.getScene().setCursor(defaultCursor);
            }
        });

        stage.getScene().setOnMousePressed(event -> {
            if (isInResizeZone(event)) {
                resizing.set(true);
                event.consume();
            }
        });

        stage.getScene().setOnMouseDragged(event -> {
            if (resizing.get()) {
                double newWidth = Math.max(stage.getMinWidth(), event.getSceneX());
                double newHeight = Math.max(stage.getMinHeight(), event.getSceneY());

                resizeAnimationTimer.stop();
                resizeAnimationTimer.setTarget(newWidth, newHeight);
                resizeAnimationTimer.start();

                event.consume();
            }
        });

        stage.getScene().setOnMouseReleased(event -> {
            resizing.set(false);
            stage.getScene().setCursor(defaultCursor);
        });
    }

    private boolean isInResizeZone(MouseEvent event) {
        return event.getSceneX() > stage.getWidth() - 20 && event.getSceneY() > stage.getHeight() - 20;
    }
}