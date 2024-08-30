package com.zxzinn.novelai;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.zxzinn.novelai.controller.ui.MainController;
import com.zxzinn.novelai.utils.common.ResourcePaths;
import com.zxzinn.novelai.utils.common.SettingsManager;
import com.zxzinn.novelai.utils.ui.LoadingScreen;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Log4j2
public class Application extends javafx.application.Application {

    private Stage primaryStage;
    private LoadingScreen loadingScreen;
    private Injector injector;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.loadingScreen = new LoadingScreen();
        this.injector = Guice.createInjector(new AppModule());

        primaryStage.initStyle(StageStyle.UNDECORATED);

        CompletableFuture.runAsync(() -> {
            loadingScreen.show();
            javafx.application.Platform.runLater(this::initializeComponents);
        });

        primaryStage.setOnCloseRequest(event -> {
            injector.getInstance(SettingsManager.class).shutdown();
            javafx.application.Platform.exit();
            System.exit(0);
        });
    }

    private void initializeComponents() {
        try {
            loadingScreen.setProgress(0.2);
            loadingScreen.setMessage("正在初始化服務...");

            loadingScreen.setProgress(0.6);
            loadingScreen.setMessage("正在載入主界面...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource(ResourcePaths.MAIN_VIEW_FXML));
            MainController mainController = injector.getInstance(MainController.class);
            loader.setController(mainController);
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(ResourcePaths.STYLES_CSS)).toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.setTitle("圖像生成器");

            // 設置初始窗口大小
            primaryStage.setWidth(1024);
            primaryStage.setHeight(768);

            // 設置窗口位置為屏幕中央
            primaryStage.centerOnScreen();

            loadingScreen.setProgress(1.0);
            loadingScreen.setMessage("載入完成");

            CompletableFuture.delayedExecutor(1, java.util.concurrent.TimeUnit.SECONDS).execute(() -> {
                javafx.application.Platform.runLater(() -> {
                    loadingScreen.hide();
                    primaryStage.show();
                });
            });

            // 將 Stage 傳遞給 MainController
            mainController.setStage(primaryStage);

        } catch (Exception e) {
            log.error("初始化組件時發生錯誤", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}