package com.zxzinn.novelai;

import com.google.gson.Gson;
import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.component.NotificationPane;
import com.zxzinn.novelai.controller.ui.MainController;
import com.zxzinn.novelai.service.filemanager.FileManagerService;
import com.zxzinn.novelai.service.filemanager.FilePreviewService;
import com.zxzinn.novelai.service.filemanager.MetadataService;
import com.zxzinn.novelai.service.generation.ImageGenerationService;
import com.zxzinn.novelai.service.ui.AlertService;
import com.zxzinn.novelai.service.ui.NotificationService;
import com.zxzinn.novelai.service.ui.WindowService;
import com.zxzinn.novelai.utils.common.ResourcePaths;
import com.zxzinn.novelai.utils.common.SettingsManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import com.zxzinn.novelai.utils.ui.LoadingScreen;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Log4j2
public class Application extends javafx.application.Application {

    private SettingsManager settingsManager;
    private Stage primaryStage;
    private FilePreviewService filePreviewService;
    private LoadingScreen loadingScreen;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.settingsManager = SettingsManager.getInstance();
        this.filePreviewService = new FilePreviewService();
        this.loadingScreen = new LoadingScreen();

        primaryStage.initStyle(StageStyle.UNDECORATED);

        CompletableFuture.runAsync(() -> {
            loadingScreen.show();
            Platform.runLater(this::initializeComponents);
        });

        primaryStage.setOnCloseRequest(event -> {
            settingsManager.shutdown();
            Platform.exit();
            System.exit(0);
        });
    }

    private void initializeComponents() {
        try {
            loadingScreen.setProgress(0.2);
            loadingScreen.setMessage("正在初始化服務...");
            Gson gson = new Gson();
            APIClient apiClient = new APIClient(gson);
            EmbedProcessor embedProcessor = new EmbedProcessor();
            ImageUtils imageUtils = new ImageUtils();

            loadingScreen.setProgress(0.4);
            loadingScreen.setMessage("正在設置圖像生成服務...");
            ImageGenerationService imageGenerationService = new ImageGenerationService(apiClient, imageUtils);
            WindowService windowService = new WindowService(settingsManager);

            loadingScreen.setProgress(0.6);
            loadingScreen.setMessage("正在準備檔案管理服務...");
            FileManagerService fileManagerService = new FileManagerService(settingsManager);
            MetadataService metadataService = new MetadataService();
            AlertService alertService = new AlertService();

            loadingScreen.setProgress(0.8);
            loadingScreen.setMessage("正在載入主界面...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource(ResourcePaths.MAIN_VIEW_FXML));
            MainController mainController = new MainController(
                    settingsManager,
                    apiClient,
                    embedProcessor,
                    imageGenerationService,
                    imageUtils,
                    windowService,
                    filePreviewService,
                    fileManagerService,
                    metadataService,
                    alertService
            );
            loader.setController(mainController);
            Parent root = loader.load();

            StackPane rootPane = new StackPane();
            rootPane.getChildren().add(root);

            NotificationPane notificationPane = new NotificationPane();
            rootPane.getChildren().add(notificationPane);

            NotificationService.initialize(notificationPane);

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            double width = screenBounds.getWidth() * 0.8;
            double height = screenBounds.getHeight() * 0.8;
            double x = (screenBounds.getWidth() - width) / 2;
            double y = (screenBounds.getHeight() - height) / 2;

            Scene scene = new Scene(rootPane, width, height);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(ResourcePaths.STYLES_CSS)).toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.setX(x);
            primaryStage.setY(y);

            mainController.setStage(primaryStage);

            primaryStage.setTitle("圖像生成器");

            loadingScreen.setProgress(1.0);
            loadingScreen.setMessage("載入完成");

            CompletableFuture.delayedExecutor(1, java.util.concurrent.TimeUnit.SECONDS).execute(() -> {
                Platform.runLater(() -> {
                    loadingScreen.hide();
                    primaryStage.show();
                    FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), rootPane);
                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);
                    fadeIn.play();
                });
            });

        } catch (Exception e) {
            log.error("初始化組件時發生錯誤", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}