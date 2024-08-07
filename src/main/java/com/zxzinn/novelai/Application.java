package com.zxzinn.novelai;

import atlantafx.base.theme.PrimerDark;
import com.google.gson.Gson;
import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.NovelAIAPIClient;
import com.zxzinn.novelai.service.ImageGenerationService;
import com.zxzinn.novelai.utils.SettingsManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;


public class Application extends javafx.application.Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 創建所需的依賴項
        SettingsManager settingsManager = new SettingsManager();
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

        // 創建 MainController 實例
        MainController mainController = new MainController(settingsManager, apiClient, embedProcessor, imageGenerationService, imageUtils);

        // 加載 FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/MainView.fxml"));
        loader.setController(mainController);
        Parent root = loader.load();

        primaryStage.setTitle("圖像生成器");
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            settingsManager.shutdown();
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        System.setProperty("prism.order", "d3d,sw");
        System.setProperty("prism.verbose", "true");
        launch(args);
    }
}
