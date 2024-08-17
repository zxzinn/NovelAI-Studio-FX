package com.zxzinn.novelai.utils.common;

import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.controller.filemanager.FileManagerController;
import com.zxzinn.novelai.controller.generation.UnifiedGeneratorController;
import com.zxzinn.novelai.controller.generation.img2img.Img2ImgGeneratorController;
import com.zxzinn.novelai.controller.generation.text2img.ImageGeneratorController;
import com.zxzinn.novelai.service.filemanager.FileManagerService;
import com.zxzinn.novelai.service.filemanager.FilePreviewService;
import com.zxzinn.novelai.service.filemanager.MetadataService;
import com.zxzinn.novelai.service.generation.ImageGenerationService;
import com.zxzinn.novelai.service.ui.AlertService;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;

public class TabFactory {
    private static TabFactory instance;

    private final SettingsManager settingsManager;
    private final APIClient apiClient;
    private final EmbedProcessor embedProcessor;
    private final ImageGenerationService imageGenerationService;
    private final ImageUtils imageUtils;
    private final FilePreviewService filePreviewService;
    private final FileManagerService fileManagerService;
    private final MetadataService metadataService;
    private final AlertService alertService;

    private TabFactory(SettingsManager settingsManager, APIClient apiClient, EmbedProcessor embedProcessor,
                       ImageGenerationService imageGenerationService, ImageUtils imageUtils,
                       FilePreviewService filePreviewService, FileManagerService fileManagerService,
                       MetadataService metadataService, AlertService alertService) {
        this.settingsManager = settingsManager;
        this.apiClient = apiClient;
        this.embedProcessor = embedProcessor;
        this.imageGenerationService = imageGenerationService;
        this.imageUtils = imageUtils;
        this.filePreviewService = filePreviewService;
        this.fileManagerService = fileManagerService;
        this.metadataService = metadataService;
        this.alertService = alertService;
    }

    public static TabFactory getInstance(SettingsManager settingsManager, APIClient apiClient, EmbedProcessor embedProcessor,
                                         ImageGenerationService imageGenerationService, ImageUtils imageUtils,
                                         FilePreviewService filePreviewService, FileManagerService fileManagerService,
                                         MetadataService metadataService, AlertService alertService) {
        if (instance == null) {
            instance = new TabFactory(settingsManager, apiClient, embedProcessor, imageGenerationService, imageUtils,
                    filePreviewService, fileManagerService, metadataService, alertService);
        }
        return instance;
    }

    public Tab createGeneratorTab() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/ImageGenerator.fxml"));
        loader.setControllerFactory(param -> new ImageGeneratorController(apiClient, embedProcessor, settingsManager,
                imageGenerationService, imageUtils, filePreviewService));
        BorderPane content = loader.load();
        return new TabBuilder()
                .setContent(content)
                .setText("Generator")
                .setClosable(false)
                .setGraphic(new FontIcon("fas-image"))
                .build();
    }

    public Tab createImg2ImgTab() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/Img2ImgGenerator.fxml"));
        loader.setControllerFactory(param -> new Img2ImgGeneratorController(apiClient, embedProcessor, settingsManager,
                imageGenerationService, imageUtils, filePreviewService));
        BorderPane content = loader.load();
        return new TabBuilder()
                .setContent(content)
                .setText("Img2Img")
                .setClosable(false)
                .setGraphic(new FontIcon("fas-exchange-alt"))
                .build();
    }

    public Tab createUnifiedGeneratorTab() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/UnifiedGenerator.fxml"));
        loader.setControllerFactory(param -> new UnifiedGeneratorController(apiClient, embedProcessor, settingsManager,
                imageGenerationService, imageUtils, filePreviewService));
        BorderPane content = loader.load();
        return new TabBuilder()
                .setContent(content)
                .setText("圖像生成")
                .setClosable(false)
                .setGraphic(new FontIcon("fas-image"))
                .build();
    }

    public Tab createFileManagerTab() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/FileManager.fxml"));
        FileManagerController controller = new FileManagerController(
                settingsManager,
                fileManagerService,
                filePreviewService,
                metadataService,
                alertService
        );
        loader.setController(controller);
        BorderPane content = loader.load();
        return new TabBuilder()
                .setContent(content)
                .setText("File Manager")
                .setClosable(false)
                .setGraphic(new FontIcon("fas-folder-open"))
                .build();
    }

    private static class TabBuilder {
        private final Tab tab = new Tab();

        public TabBuilder setContent(BorderPane content) {
            tab.setContent(content);
            return this;
        }

        public TabBuilder setText(String text) {
            tab.setText(text);
            return this;
        }

        public TabBuilder setClosable(boolean closable) {
            tab.setClosable(closable);
            return this;
        }

        public TabBuilder setGraphic(FontIcon icon) {
            tab.setGraphic(icon);
            return this;
        }

        public Tab build() {
            return tab;
        }
    }
}