package com.zxzinn.novelai;

import com.zxzinn.novelai.utils.SettingsManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;

public class Application extends javafx.application.Application {

    @Getter
    private static SettingsManager settingsManager;

    public static void main(String[] args) {
        System.setProperty("prism.order", "d3d,sw");
        System.setProperty("prism.verbose", "true");
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        settingsManager = new SettingsManager();

        Parent root = FXMLLoader.load(getClass().getResource("/com/zxzinn/novelai/MainView.fxml"));
        primaryStage.setTitle("圖像生成器");
        primaryStage.initStyle(StageStyle.UNDECORATED);  // 設置無邊框樣式
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            settingsManager.shutdown();
            Platform.exit();
            System.exit(0);
        });
    }
}