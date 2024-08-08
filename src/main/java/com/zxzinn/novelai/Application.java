package com.zxzinn.novelai;

import atlantafx.base.theme.PrimerDark;
import com.google.gson.Gson;
import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.NovelAIAPIClient;
import com.zxzinn.novelai.controller.FileManagerController;
import com.zxzinn.novelai.service.ImageGenerationService;
import com.zxzinn.novelai.utils.SettingsManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Log4j2
public class Application extends javafx.application.Application {

    private SettingsManager settingsManager;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.settingsManager = SettingsManager.getInstance(); // 確保在這裡初始化
        primaryStage.initStyle(StageStyle.UNDECORATED);
        showLoadingScreen();
        initializeComponents();
    }

    private void showLoadingScreen() {
        ProgressIndicator progressIndicator = new ProgressIndicator();
        StackPane root = new StackPane(progressIndicator);
        Scene scene = new Scene(root, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void initializeComponents() {
        Task<Void> initTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 使用已初始化的 settingsManager
                OkHttpClient httpClient = new OkHttpClient.Builder()
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .build();
                Gson gson = new Gson();
                APIClient apiClient = new NovelAIAPIClient(httpClient, gson);
                EmbedProcessor embedProcessor = new EmbedProcessor();
                ImageUtils imageUtils = new ImageUtils();
                ImageGenerationService imageGenerationService = new ImageGenerationService(apiClient, imageUtils);

                MainController mainController = new MainController(settingsManager, apiClient, embedProcessor, imageGenerationService, imageUtils);

                Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/MainView.fxml"));
                        loader.setController(mainController);
                        Parent root = loader.load();

                        Scene scene = new Scene(root, 800, 600);
                        primaryStage.setScene(scene);

                        mainController.initializeUI();

                        primaryStage.setTitle("圖像生成器");
                        primaryStage.show();

                        primaryStage.setOnCloseRequest(event -> {
                            settingsManager.shutdown();
                            System.exit(0);
                        });
                    } catch (Exception e) {
                        log.error("載入主要內容時發生錯誤", e);
                    }
                });

                return null;
            }
        };

        initTask.setOnSucceeded(e -> {
            log.info("初始化完成");
        });

        initTask.setOnFailed(e -> {
            log.error("初始化失敗", initTask.getException());
        });

        new Thread(initTask).start();
    }

    public static void main(String[] args) {
        System.setProperty("prism.order", "d3d,sw");
        System.setProperty("prism.verbose", "true");
        launch(args);
    }
}