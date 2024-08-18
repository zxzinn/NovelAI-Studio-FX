package com.zxzinn.novelai;

import atlantafx.base.theme.NordDark;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
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
import com.zxzinn.novelai.utils.common.SettingsManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Log4j2
public class Application extends javafx.application.Application {

    private SettingsManager settingsManager;
    private Stage primaryStage;
    private FilePreviewService filePreviewService;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.settingsManager = SettingsManager.getInstance();
        this.filePreviewService = new FilePreviewService();
        primaryStage.initStyle(StageStyle.UNDECORATED);
        showLoadingScreen();
        initializeComponents();

        primaryStage.setOnCloseRequest(event -> {
            settingsManager.shutdown();
            Platform.exit();
            System.exit(0);
        });
    }

    private void showLoadingScreen() {
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressIndicator.getStyleClass().add("custom-progress-indicator");

        Label loadingLabel = new Label("正在載入...");
        loadingLabel.getStyleClass().add("loading-label");

        VBox loadingBox = new VBox(20, progressIndicator, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);

        StackPane root = new StackPane(loadingBox);
        root.getStyleClass().add("loading-background");

        Scene scene = new Scene(root, 300, 200);

        String cssPath = "/com/zxzinn/novelai/loading-styles.css";
        URL resource = getClass().getResource(cssPath);
        if (resource != null) {
            scene.getStylesheets().add(resource.toExternalForm());
        } else {
            log.warn("無法找到樣式文件: {}", cssPath);
        }

        Stage loadingStage = new Stage(StageStyle.UNDECORATED);
        loadingStage.setScene(scene);
        loadingStage.show();

        primaryStage.setOnShown(e -> loadingStage.close());
    }

    private void initializeComponents() {
        closeLoadingScreen();
        CompletableFuture.runAsync(() -> {
            try {
                Gson gson = new Gson();
                APIClient apiClient = new APIClient(gson);
                EmbedProcessor embedProcessor = new EmbedProcessor();
                ImageUtils imageUtils = new ImageUtils();
                ImageGenerationService imageGenerationService = new ImageGenerationService(apiClient, imageUtils);
                WindowService windowService = new WindowService(settingsManager);
                FileManagerService fileManagerService = new FileManagerService(settingsManager);
                MetadataService metadataService = new MetadataService();
                AlertService alertService = new AlertService();

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

                Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/MainView.fxml"));
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
                        scene.getStylesheets().add(new PrimerDark().getUserAgentStylesheet());
                        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/zxzinn/novelai/styles.css")).toExternalForm());

                        primaryStage.setScene(scene);
                        primaryStage.setX(x);
                        primaryStage.setY(y);

                        mainController.setStage(primaryStage);

                        primaryStage.setTitle("圖像生成器");
                        primaryStage.show();

                        primaryStage.setOnCloseRequest(event -> {
                            settingsManager.shutdown();
                            Platform.exit();
                            System.exit(0);
                        });
                    } catch (Exception e) {
                        log.error("載入主要內容時發生錯誤", e);
                    }
                });
            } catch (IOException e) {
                log.error("初始化組件時發生錯誤", e);
            }
        });
    }

    private void closeLoadingScreen() {
        Platform.runLater(() -> {
            if (primaryStage.getScene() != null && primaryStage.getScene().getRoot() instanceof StackPane) {
                ((StackPane) primaryStage.getScene().getRoot()).getChildren().clear();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}