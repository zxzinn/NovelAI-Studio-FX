package com.zxzinn.novelai.controller;

import com.zxzinn.novelai.service.FileManagerService;
import com.zxzinn.novelai.utils.SettingsManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;

@Log4j2
public class FileManagerController {
    @FXML private TreeView<String> fileTreeView;
    @FXML private ImageView previewImageView;
    @FXML private ListView<String> metadataListView;
    @FXML private TextField searchField;
    @FXML private Button addButton;
    @FXML private Button removeButton;
    @FXML private Button clearMetadataButton;
    @FXML private Button constructDatabaseButton;
    @FXML private Button fitButton;
    @FXML private Button originalSizeButton;

    private SettingsManager settingsManager;
    private FileManagerService fileManagerService;

    public FileManagerController(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    @FXML
    public void initialize() {
        if (settingsManager == null) {
            log.error("SettingsManager is null in FileManagerController");
            return;
        }
        initializeFileManagerService();
    }

    private void initializeFileManagerService() {
        if (fileManagerService != null) {
            return;
        }

        try {
            fileManagerService = new FileManagerService(settingsManager);
            setupEventHandlers();
            refreshTreeView();
        } catch (IOException e) {
            log.error("無法初始化FileManagerService", e);
            showAlert("錯誤", "無法初始化文件管理服務: " + e.getMessage());
        }
    }

    public void setSettingsManager(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
        initializeFileManagerService();
    }

    private void setupEventHandlers() {
        addButton.setOnAction(event -> addWatchedDirectory());
        removeButton.setOnAction(event -> removeWatchedDirectory());
        clearMetadataButton.setOnAction(event -> clearMetadata());
        constructDatabaseButton.setOnAction(event -> constructDatabase());
        fitButton.setOnAction(event -> fitImage());
        originalSizeButton.setOnAction(event -> showOriginalSize());

        fileTreeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updatePreview(newValue);
            }
        });

        fileTreeView.addEventHandler(TreeItem.branchExpandedEvent(), this::handleBranchExpanded);
        fileTreeView.addEventHandler(TreeItem.branchCollapsedEvent(), this::handleBranchCollapsed);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            // TODO: 實現搜尋功能
        });
    }

    private void handleBranchExpanded(TreeItem.TreeModificationEvent<String> event) {
        TreeItem<String> source = event.getTreeItem();
        fileManagerService.setDirectoryExpanded(buildFullPath(source), true);
    }

    private void handleBranchCollapsed(TreeItem.TreeModificationEvent<String> event) {
        TreeItem<String> source = event.getTreeItem();
        fileManagerService.setDirectoryExpanded(buildFullPath(source), false);
    }

    private void addWatchedDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("選擇要監視的目錄");
        File selectedDirectory = directoryChooser.showDialog(fileTreeView.getScene().getWindow());

        if (selectedDirectory != null) {
            try {
                fileManagerService.addWatchedDirectory(selectedDirectory.getAbsolutePath());
                refreshTreeView();
            } catch (IOException e) {
                log.error("無法添加監視目錄", e);
                showAlert("錯誤", "無法添加監視目錄: " + e.getMessage());
            }
        }
    }

    private void removeWatchedDirectory() {
        TreeItem<String> selectedItem = fileTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getParent() != null) {
            String path = buildFullPath(selectedItem);
            fileManagerService.removeWatchedDirectory(path);
            refreshTreeView();
        }
    }

    private String buildFullPath(TreeItem<String> item) {
        StringBuilder path = new StringBuilder(item.getValue());
        TreeItem<String> parent = item.getParent();
        while (parent != null && !parent.getValue().equals("監視的目錄")) {
            path.insert(0, parent.getValue() + File.separator);
            parent = parent.getParent();
        }
        return path.toString();
    }

    private void refreshTreeView() {
        fileManagerService.getDirectoryTree().thenAccept(root -> {
            javafx.application.Platform.runLater(() -> fileTreeView.setRoot(root));
        });
    }

    private void updatePreview(TreeItem<String> item) {
        String fullPath = buildFullPath(item);
        File file = new File(fullPath);
        if (file.isFile()) {
            try {
                Image image = new Image(file.toURI().toString());
                previewImageView.setImage(image);
                updateMetadataList(file);
            } catch (Exception e) {
                log.error("無法載入預覽圖片", e);
                previewImageView.setImage(null);
            }
        } else {
            previewImageView.setImage(null);
            metadataListView.getItems().clear();
        }
    }

    private void updateMetadataList(File file) {
        // TODO: 實現讀取和顯示元數據的功能
        metadataListView.getItems().clear();
        metadataListView.getItems().add("文件名: " + file.getName());
        metadataListView.getItems().add("大小: " + file.length() + " bytes");
        // 添加更多元數據...
    }

    private void clearMetadata() {
        // TODO: 實現清除元數據功能
        showAlert("提示", "清除元數據功能尚未實現");
    }

    private void constructDatabase() {
        // TODO: 實現構建數據庫功能
        showAlert("提示", "構建數據庫功能尚未實現");
    }

    private void fitImage() {
        if (previewImageView.getImage() != null) {
            previewImageView.setFitWidth(previewImageView.getParent().getBoundsInLocal().getWidth());
            previewImageView.setFitHeight(previewImageView.getParent().getBoundsInLocal().getHeight());
            previewImageView.setPreserveRatio(true);
        }
    }

    private void showOriginalSize() {
        if (previewImageView.getImage() != null) {
            previewImageView.setFitWidth(0);
            previewImageView.setFitHeight(0);
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void shutdown() {
        if (fileManagerService != null) {
            fileManagerService.shutdown();
        }
    }
}