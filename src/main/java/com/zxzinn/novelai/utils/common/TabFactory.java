package com.zxzinn.novelai.utils.common;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zxzinn.novelai.controller.filemanager.FileManagerController;
import com.zxzinn.novelai.controller.generation.GenerationController;
import com.zxzinn.novelai.service.filemanager.FileManagerService;
import com.zxzinn.novelai.service.filemanager.FileOperationService;
import com.zxzinn.novelai.service.filemanager.FilePreviewService;
import com.zxzinn.novelai.service.filemanager.MetadataService;
import com.zxzinn.novelai.service.generation.GenerationSettingsManager;
import com.zxzinn.novelai.service.generation.ImageGenerationService;
import com.zxzinn.novelai.service.ui.AlertService;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;

@Singleton
public class TabFactory {

    private final SettingsManager settingsManager;
    private final EmbedProcessor embedProcessor;
    private final ImageGenerationService imageGenerationService;
    private final ImageUtils imageUtils;
    private final FilePreviewService filePreviewService;
    private final FileManagerService fileManagerService;
    private final MetadataService metadataService;
    private final AlertService alertService;
    private final FileOperationService fileOperationService;

    @Inject
    public TabFactory(SettingsManager settingsManager, EmbedProcessor embedProcessor,
                      ImageGenerationService imageGenerationService, ImageUtils imageUtils,
                      FilePreviewService filePreviewService, FileManagerService fileManagerService,
                      MetadataService metadataService, AlertService alertService,FileOperationService fileOperationService) {
        this.settingsManager = settingsManager;
        this.embedProcessor = embedProcessor;
        this.imageGenerationService = imageGenerationService;
        this.imageUtils = imageUtils;
        this.filePreviewService = filePreviewService;
        this.fileManagerService = fileManagerService;
        this.metadataService = metadataService;
        this.alertService = alertService;
        this.fileOperationService = fileOperationService;
    }

    public Tab createUnifiedGeneratorTab() throws IOException {
        FXMLLoader loader = FXMLLoaderFactory.createLoader(ResourcePaths.IMAGE_GENERATOR_FXML);
        GenerationSettingsManager generationSettingsManager = new GenerationSettingsManager(settingsManager);
        loader.setControllerFactory(param -> new GenerationController(embedProcessor,
                imageGenerationService, imageUtils, filePreviewService, generationSettingsManager));
        BorderPane content = loader.load();
        return new TabBuilder()
                .setContent(content)
                .setText("圖像生成")
                .setClosable(false)
                .setGraphic(new FontIcon("fas-image"))
                .build();
    }

    public Tab createFileManagerTab() throws IOException {
        FXMLLoader loader = FXMLLoaderFactory.createLoader(ResourcePaths.FILE_MANAGER_FXML);
        FileManagerController controller = new FileManagerController(
                fileManagerService,
                filePreviewService,
                metadataService,
                alertService,
                fileOperationService  // 使用 FileOperationService 而不是 ImageUtils
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