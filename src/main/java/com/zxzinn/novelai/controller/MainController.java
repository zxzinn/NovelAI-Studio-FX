package com.zxzinn.novelai.controller;

import com.google.inject.Inject;
import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.component.NotificationPane;
import com.zxzinn.novelai.controller.filemanager.FileManagerController;
import com.zxzinn.novelai.controller.generation.GenerationController;
import com.zxzinn.novelai.service.filemanager.FileManagerService;
import com.zxzinn.novelai.service.filemanager.FileOperationService;
import com.zxzinn.novelai.service.filemanager.FilePreviewService;
import com.zxzinn.novelai.service.filemanager.MetadataService;
import com.zxzinn.novelai.service.generation.GenerationSettingsManager;
import com.zxzinn.novelai.service.ui.AlertService;
import com.zxzinn.novelai.service.ui.NotificationService;
import com.zxzinn.novelai.utils.common.FXMLLoaderFactory;
import com.zxzinn.novelai.utils.common.PropertiesManager;
import com.zxzinn.novelai.utils.common.ResourcePaths;
import com.zxzinn.novelai.utils.ui.DragAndDropHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;

public class MainController {
    @FXML private BorderPane rootPane;
    @FXML private TabPane mainTabPane;

    private final DragAndDropHandler dragAndDropHandler;
    private final PropertiesManager propertiesManager;
    private final FilePreviewService filePreviewService;
    private final FileManagerService fileManagerService;
    private final MetadataService metadataService;
    private final AlertService alertService;
    private final FileOperationService fileOperationService;

    @Inject
    public MainController(PropertiesManager propertiesManager,
                          FilePreviewService filePreviewService,
                          FileManagerService fileManagerService, MetadataService metadataService,
                          AlertService alertService, FileOperationService fileOperationService) {
        this.dragAndDropHandler = new DragAndDropHandler(this::handleFileDrop);
        this.propertiesManager = propertiesManager;
        this.filePreviewService = filePreviewService;
        this.fileManagerService = fileManagerService;
        this.metadataService = metadataService;
        this.alertService = alertService;
        this.fileOperationService = fileOperationService;
    }

    @FXML
    public void initialize() {
        loadTabContent();
    }

    private void loadTabContent() {
        try {
            mainTabPane.getTabs().addAll(
                    createUnifiedGeneratorTab(),
                    createFileManagerTab()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Tab createUnifiedGeneratorTab() throws IOException {
        FXMLLoader loader = FXMLLoaderFactory.createLoader(ResourcePaths.IMAGE_GENERATOR_FXML);
        GenerationSettingsManager generationSettingsManager = new GenerationSettingsManager(propertiesManager);
        loader.setControllerFactory(param -> new GenerationController(
                filePreviewService, generationSettingsManager));
        BorderPane content = loader.load();
        return new Tab("圖像生成", content) {{
            setClosable(false);
            setGraphic(new FontIcon("fas-image"));
        }};
    }

    private Tab createFileManagerTab() throws IOException {
        FXMLLoader loader = FXMLLoaderFactory.createLoader(ResourcePaths.FILE_MANAGER_FXML);
        FileManagerController controller = new FileManagerController(
                fileManagerService,
                filePreviewService,
                metadataService,
                alertService,
                fileOperationService
        );
        loader.setController(controller);
        BorderPane content = loader.load();
        return new Tab("File Manager", content) {{
            setClosable(false);
            setGraphic(new FontIcon("fas-folder-open"));
        }};
    }

    public void setStage(Stage stage) {

        StackPane root = new StackPane();
        root.getChildren().add(rootPane);

        NotificationPane notificationPane = new NotificationPane();
        root.getChildren().add(notificationPane);
        NotificationService.initialize(notificationPane);

        stage.getScene().setRoot(root);

        dragAndDropHandler.enableDragAndDrop(root);
    }

    private void handleFileDrop(File file) {
        System.out.println("File dropped: " + file.getAbsolutePath());
        NotificationService.showNotification("文件已拖放: " + file.getName());
    }
}