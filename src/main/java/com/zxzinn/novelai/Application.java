package com.zxzinn.novelai;

import com.zxzinn.novelai.utils.SettingsManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;

public class Application extends javafx.application.Application {

    @Getter
    private static SettingsManager settingsManager;

    @Override
    public void start(Stage primaryStage) throws Exception {
        settingsManager = new SettingsManager();

        Parent root = FXMLLoader.load(getClass().getResource("/com/zxzinn/novelai/MainView.fxml"));
        primaryStage.setTitle("圖像生成器");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            settingsManager.shutdown();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

}