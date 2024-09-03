package com.zxzinn.novelai;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.zxzinn.novelai.controller.ui.MainController;
import com.zxzinn.novelai.service.VersionCheckService;
import com.zxzinn.novelai.utils.common.PropertiesManager;
import com.zxzinn.novelai.utils.common.ResourcePaths;
import com.zxzinn.novelai.utils.ui.LoadingManager;
import com.zxzinn.novelai.utils.ui.LoadingScreen;
import com.zxzinn.novelai.utils.ui.LoadingTask;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;

@Log4j2
public class MainApplication extends Application {

    private Stage primaryStage;
    private LoadingScreen loadingScreen;
    private Injector injector;
    private LoadingManager loadingManager;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.loadingScreen = new LoadingScreen();
        this.injector = Guice.createInjector(new AppModule());
        this.loadingManager = new LoadingManager();

        loadingManager.addObserver(loadingScreen);
        setupLoadingTasks();

        loadingScreen.show();
        loadingManager.start();

        primaryStage.setOnCloseRequest(event -> {
            injector.getInstance(PropertiesManager.class).shutdown();
            Platform.exit();
            System.exit(0);
        });
    }

    private void setupLoadingTasks() {
        loadingManager.addTask(new LoadingTask() {
            @Override
            public void execute() {
                injector.getInstance(VersionCheckService.class).checkForUpdates();
            }

            @Override
            public String getDescription() {
                return "正在檢查更新...";
            }
        });

        loadingManager.addTask(new LoadingTask() {
            @Override
            public void execute() {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(ResourcePaths.MAIN_VIEW_FXML));
                    MainController mainController = injector.getInstance(MainController.class);
                    loader.setController(mainController);
                    Parent root = loader.load();

                    Scene scene = new Scene(root);
                    scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(ResourcePaths.STYLES_CSS)).toExternalForm());

                    primaryStage.setScene(scene);
                    primaryStage.setTitle("NovelAI Studio FX");

                    primaryStage.setWidth(1024);
                    primaryStage.setHeight(768);
                    primaryStage.centerOnScreen();

                    mainController.setStage(primaryStage);
                } catch (Exception e) {
                    log.error("初始化組件時發生錯誤", e);
                }
            }

            @Override
            public String getDescription() {
                return "正在加載主介面...";
            }
        });

        loadingManager.addTask(new LoadingTask() {
            @Override
            public void execute() {
                loadingScreen.hide();
                primaryStage.show();
            }

            @Override
            public String getDescription() {
                return "準備就緒";
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}