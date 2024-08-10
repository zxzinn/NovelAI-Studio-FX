package com.zxzinn.novelai.controller.filemanager;

import com.zxzinn.novelai.component.PreviewPane;
import com.zxzinn.novelai.service.filemanager.*;
import com.zxzinn.novelai.service.ui.AlertService;
import com.zxzinn.novelai.utils.common.SettingsManager;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;

@Log4j2
public class FileManagerController {
    @FXML private TreeView<String> fileTreeView;
    @FXML private StackPane previewContainer;
    @FXML private ListView<String> metadataListView;
    @FXML private TextField searchField;
    @FXML private Button addButton;
    @FXML private Button removeButton;
    @FXML private Button constructDatabaseButton;
    @FXML private TextField watermarkTextField;
    @FXML private CheckBox clearLSBCheckBox;
    @FXML private Button processButton;
    @FXML private Button zoomInButton;
    @FXML private Button zoomOutButton;

    private final SettingsManager settingsManager;
    private final FileManagerService fileManagerService;
    private final FilePreviewService filePreviewService;
    private final MetadataService metadataService;
    private final ImageProcessingService imageProcessingService;
    private final AlertService alertService;
    private final FileTreeController fileTreeController;
    private PreviewPane previewPane;

    public FileManagerController(SettingsManager settingsManager,
                                 FileManagerService fileManagerService,
                                 FilePreviewService filePreviewService,
                                 MetadataService metadataService,
                                 ImageProcessingService imageProcessingService,
                                 AlertService alertService) {
        this.settingsManager = settingsManager;
        this.fileManagerService = fileManagerService;
        this.filePreviewService = filePreviewService;
        this.metadataService = metadataService;
        this.imageProcessingService = imageProcessingService;
        this.alertService = alertService;
        this.fileTreeController = new FileTreeController(fileManagerService);
    }

    @FXML
    public void initialize() {
        previewPane = new PreviewPane(filePreviewService);
        previewContainer.getChildren().add(previewPane);
        setupEventHandlers();
        fileTreeController.setFileTreeView(fileTreeView);
        fileTreeController.refreshTreeView();
    }

    private void setupEventHandlers() {
        addButton.setOnAction(event -> addWatchedDirectory());
        removeButton.setOnAction(event -> removeWatchedDirectory());
        constructDatabaseButton.setOnAction(event -> constructDatabase());

        fileTreeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updatePreview(newValue);
            }
        });

        fileTreeView.addEventHandler(TreeItem.branchExpandedEvent(), fileTreeController::handleBranchExpanded);
        fileTreeView.addEventHandler(TreeItem.branchCollapsedEvent(), fileTreeController::handleBranchCollapsed);

        searchField.textProperty().addListener((observable, oldValue, newValue) ->
                fileTreeController.setSearchFilter(newValue));

        processButton.setOnAction(event -> processSelectedImage());

        zoomInButton.setOnAction(event -> previewPane.zoomIn());
        zoomOutButton.setOnAction(event -> previewPane.zoomOut());
    }

    private void addWatchedDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("選擇要監視的目錄");
        File selectedDirectory = directoryChooser.showDialog(fileTreeView.getScene().getWindow());

        if (selectedDirectory != null) {
            try {
                fileManagerService.addWatchedDirectory(selectedDirectory.getAbsolutePath());
                fileTreeController.refreshTreeView();
            } catch (IOException e) {
                log.error("無法添加監視目錄", e);
                alertService.showAlert("錯誤", "無法添加監視目錄: " + e.getMessage());
            }
        }
    }

    private void removeWatchedDirectory() {
        TreeItem<String> selectedItem = fileTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            String path = fileTreeController.buildFullPath(selectedItem);
            if (path.isEmpty()) {
                alertService.showAlert("警告", "無法移除根目錄。請選擇一個具體的監視目錄。");
                return;
            }
            fileManagerService.removeWatchedDirectory(path);
            fileTreeController.refreshTreeView();
        } else {
            alertService.showAlert("警告", "請選擇一個要移除的監視目錄。");
        }
    }

    private void updatePreview(TreeItem<String> item) {
        String fullPath = fileTreeController.buildFullPath(item);
        File file = new File(fullPath);
        previewPane.updatePreview(file);
        updateMetadataList(file);
    }

    private void updateMetadataList(File file) {
        if (file != null) {
            metadataListView.getItems().setAll(metadataService.getMetadata(file));
        } else {
            metadataListView.getItems().clear();
        }
    }

    private void processSelectedImage() {
        TreeItem<String> selectedItem = fileTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            String path = fileTreeController.buildFullPath(selectedItem);
            File file = new File(path);
            if (file.isFile()) {
                try {
                    imageProcessingService.processImage(file, watermarkTextField.getText(), clearLSBCheckBox.isSelected());
                    alertService.showAlert("成功", "圖像處理完成: " + file.getName());
                    fileTreeController.refreshTreeView();
                } catch (RuntimeException e) {
                    alertService.showAlert("錯誤", e.getMessage());
                }
            }
        }
    }

    private void constructDatabase() {
        // TODO: 實現構建數據庫功能
        alertService.showAlert("提示", "構建數據庫功能尚未實現");
    }

    public void shutdown() {
        fileManagerService.shutdown();
    }
}