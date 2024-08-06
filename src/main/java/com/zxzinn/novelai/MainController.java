package com.zxzinn.novelai;

import com.google.gson.Gson;
import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.NovelAIAPIClient;
import com.zxzinn.novelai.controller.FileManagerController;
import com.zxzinn.novelai.controller.ImageGeneratorController;
import com.zxzinn.novelai.controller.Img2ImgGeneratorController;
import com.zxzinn.novelai.service.ImageGenerationService;
import com.zxzinn.novelai.utils.SettingsManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


@Log4j2
public class MainController {
    @FXML private AnchorPane rootPane;
    @FXML private HBox titleBar;
    @FXML private VBox mainVBox;
    @FXML private Tab generatorTab;
    @FXML private Tab Img2ImgTab;
    @FXML private Tab fileManagerTab;
    @FXML private Label titleLabel;
    @FXML private Button minimizeButton;
    @FXML private Button maximizeButton;
    @FXML private Button closeButton;

    private double xOffset = 0;
    private double yOffset = 0;
    private Rectangle2D previousBounds;

    private final SettingsManager settingsManager;
    private final APIClient apiClient;
    private final EmbedProcessor embedProcessor;
    private final ImageGenerationService imageGenerationService;
    private final ImageUtils imageUtils;

    private static final long ANIMATION_DURATION = 150_000_000L; // 150ms in nanoseconds

    public MainController(SettingsManager settingsManager, APIClient apiClient, EmbedProcessor embedProcessor,
                          ImageGenerationService imageGenerationService, ImageUtils imageUtils) {
        this.settingsManager = settingsManager;
        this.apiClient = apiClient;
        this.embedProcessor = embedProcessor;
        this.imageGenerationService = imageGenerationService;
        this.imageUtils = imageUtils;
    }

    @FXML
    private void initialize() {
        Platform.runLater(this::setupUI);
    }

    private void setupUI() {
        if (rootPane == null || titleBar == null) {
            log.error("錯誤：rootPane或titleBar為null。請檢查FXML中的fx:id設置是否正確。");
            return;
        }

        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            if (stage == null) {
                log.error("錯誤：無法獲取Stage。請確保已設置Scene。");
                return;
            }

            loadTabContent();
            setupTabListeners();
        } catch (Exception e) {
            log.error("設置UI時發生錯誤", e);
        }
    }

    private void loadTabContent() throws IOException {
        FXMLLoader generatorLoader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/ImageGenerator.fxml"));
        generatorLoader.setControllerFactory(param -> new ImageGeneratorController(apiClient, embedProcessor, settingsManager, imageGenerationService, imageUtils));
        BorderPane generatorContent = generatorLoader.load();
        generatorTab.setContent(generatorContent);

        FXMLLoader img2ImgLoader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/Img2ImgGenerator.fxml"));
        img2ImgLoader.setControllerFactory(param -> new Img2ImgGeneratorController(apiClient, embedProcessor, imageGenerationService, imageUtils, settingsManager));
        BorderPane img2ImgContent = img2ImgLoader.load();
        Img2ImgTab.setContent(img2ImgContent);

        FXMLLoader fileManagerLoader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/FileManager.fxml"));
        BorderPane fileManagerContent = fileManagerLoader.load();
        fileManagerTab.setContent(fileManagerContent);
    }

    private void setupTabListeners() {
        generatorTab.setOnSelectionChanged(event -> {
            if (generatorTab.isSelected()) {
                ImageGeneratorController controller = (ImageGeneratorController) generatorTab.getContent().getUserData();
                if (controller != null) {
                    controller.setMainWindow(rootPane.getScene().getWindow());
                }
            }
        });
    }

    @FXML
    private void handleMousePressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    private void handleMouseDragged(MouseEvent event) {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    @FXML
    private void handleMouseClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            handleMaximize();
        }
    }

    @FXML
    private void handleMinimize() {
        ((Stage) rootPane.getScene().getWindow()).setIconified(true);
    }

    @FXML
    private void handleMaximize() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        Scene scene = stage.getScene();
        if (scene == null) return;

        if (!stage.isMaximized()) {
            previousBounds = new Rectangle2D(stage.getX(), stage.getY(), scene.getWidth(), scene.getHeight());
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            animateResize(screenBounds.getMinX(), screenBounds.getMinY(),
                    screenBounds.getWidth(), screenBounds.getHeight(), true);
        } else {
            animateResize(previousBounds.getMinX(), previousBounds.getMinY(),
                    previousBounds.getWidth(), previousBounds.getHeight(), false);
        }
    }

    @FXML
    private void handleClose() {
        settingsManager.shutdown();
        Platform.exit();
        System.exit(0);
    }

    private void animateResize(double toX, double toY, double toWidth, double toHeight, boolean maximize) {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        final double startX = stage.getX();
        final double startY = stage.getY();
        final double startWidth = stage.getWidth();
        final double startHeight = stage.getHeight();
        final long startTime = System.nanoTime();

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                long elapsedTime = now - startTime;
                if (elapsedTime > ANIMATION_DURATION) {
                    stage.setX(toX);
                    stage.setY(toY);
                    stage.setWidth(toWidth);
                    stage.setHeight(toHeight);
                    stage.setMaximized(maximize);
                    this.stop();
                    log.info("Animation completed in {} ms", elapsedTime / 1_000_000.0);
                } else {
                    double fraction = (double) elapsedTime / ANIMATION_DURATION;
                    stage.setX(startX + (toX - startX) * fraction);
                    stage.setY(startY + (toY - startY) * fraction);
                    stage.setWidth(startWidth + (toWidth - startWidth) * fraction);
                    stage.setHeight(startHeight + (toHeight - startHeight) * fraction);
                }
            }
        }.start();
    }
}
