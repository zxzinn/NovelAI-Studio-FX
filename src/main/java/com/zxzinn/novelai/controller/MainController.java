package com.zxzinn.novelai.controller;

import com.google.inject.Inject;
import com.zxzinn.novelai.component.NotificationPane;
import com.zxzinn.novelai.controller.filemanager.FileManagerController;
import com.zxzinn.novelai.controller.generation.GenerationController;
import com.zxzinn.novelai.service.filemanager.FileManagerService;
import com.zxzinn.novelai.service.filemanager.FileOperationService;
import com.zxzinn.novelai.service.filemanager.MetadataService;
import com.zxzinn.novelai.service.generation.GenerationTaskManager;
import com.zxzinn.novelai.service.ui.AlertService;
import com.zxzinn.novelai.service.ui.NotificationService;
import com.zxzinn.novelai.utils.common.FXMLLoaderFactory;
import com.zxzinn.novelai.utils.common.ResourcePaths;
import com.zxzinn.novelai.viewmodel.GenerationViewModel;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;

public class MainController {
    private BorderPane rootPane;
    private TabPane mainTabPane;

    private final FileManagerService fileManagerService;
    private final MetadataService metadataService;
    private final AlertService alertService;
    private final FileOperationService fileOperationService;

    @Inject
    public MainController(FileManagerService fileManagerService, MetadataService metadataService,
                          AlertService alertService, FileOperationService fileOperationService) {
        this.fileManagerService = fileManagerService;
        this.metadataService = metadataService;
        this.alertService = alertService;
        this.fileOperationService = fileOperationService;
    }

    public BorderPane createView() {
        rootPane = new BorderPane();
        mainTabPane = new TabPane();
        rootPane.setCenter(mainTabPane);
        loadTabContent();
        return rootPane;
    }

    private void loadTabContent() {
        try {
            mainTabPane.getTabs().addAll(
                    createUnifiedGeneratorTab(),
                    createFileManagerTab(),
                    createTaskMonitorTab()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Tab createTaskMonitorTab() throws IOException {
        FXMLLoader loader = FXMLLoaderFactory.createLoader(ResourcePaths.TASK_MONITOR_FXML);
        BorderPane content = loader.load();
        TaskMonitorController controller = loader.getController();

        GenerationTaskManager.getInstance().setTaskMonitorController(controller);

        return new Tab("任務監控", content) {{
            setClosable(false);
            setGraphic(new FontIcon("fas-tasks"));
        }};
    }

    private Tab createUnifiedGeneratorTab() {
        GenerationViewModel viewModel = new GenerationViewModel();
        GenerationController generationController = new GenerationController(viewModel);
        BorderPane content = generationController.createView();
        return new Tab("圖像生成", content) {{
            setClosable(false);
            setGraphic(new FontIcon("fas-image"));
        }};
    }

    private Tab createFileManagerTab() throws IOException {
        FXMLLoader loader = FXMLLoaderFactory.createLoader(ResourcePaths.FILE_MANAGER_FXML);
        FileManagerController controller = new FileManagerController(
                fileManagerService,
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

    }
}